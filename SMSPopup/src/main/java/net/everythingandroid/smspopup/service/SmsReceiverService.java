package net.everythingandroid.smspopup.service;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SmsMessage.MessageClass;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import net.everythingandroid.smspopup.BuildConfig;
import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.provider.SmsMmsMessage;
import net.everythingandroid.smspopup.provider.SmsPopupContract.ContactNotifications;
import net.everythingandroid.smspopup.receiver.SmsReceiver;
import net.everythingandroid.smspopup.util.Log;
import net.everythingandroid.smspopup.util.ManageKeyguard;
import net.everythingandroid.smspopup.util.ManageNotification;
import net.everythingandroid.smspopup.util.ManagePreferences;
import net.everythingandroid.smspopup.util.ManagePreferences.Defaults;
import net.everythingandroid.smspopup.util.ManageWakeLock;
import net.everythingandroid.smspopup.util.SmsMessageSender;
import net.everythingandroid.smspopup.util.SmsPopupUtils;

import java.util.List;

public class SmsReceiverService extends IntentService {
    private static final String TAG = SmsReceiverService.class.getName();

    private static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String ACTION_MMS_RECEIVED =
            "android.provider.Telephony.WAP_PUSH_RECEIVED";
    private static final String ACTION_MESSAGE_RECEIVED =
            "net.everythingandroid.smspopup.MESSAGE_RECEIVED";
    private static final String MMS_DATA_TYPE = "application/vnd.wap.mms-message";

    // http://android.git.kernel.org/?p=platform/packages/apps/Mms.git;a=blob;f=src/com/android/mms/transaction/SmsReceiverService.java
    public static final String MESSAGE_SENT_ACTION = "com.android.mms.transaction.MESSAGE_SENT";

    /*
     * This is the number of retries and pause between retries that we will keep checking the system
     * message database for the latest incoming message
     */
    private static final int MESSAGE_RETRY = 8;
    private static final int MESSAGE_RETRY_PAUSE = 1000;

    private Context context;
    private int mResultCode;
    private boolean serviceRestarted = false;

    private static final int TOAST_HANDLER_MESSAGE_SENT = 0;
    private static final int TOAST_HANDLER_MESSAGE_SEND_LATER = 1;
    private static final int TOAST_HANDLER_MESSAGE_FAILED = 2;
    private static final int TOAST_HANDLER_MESSAGE_CUSTOM = 3;

    public SmsReceiverService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceRestarted = false;
        if ((flags & START_FLAG_REDELIVERY) !=0) {
            serviceRestarted = true;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (BuildConfig.DEBUG)
            Log.v("SMSReceiverService: doWakefulWork()");

        mResultCode = 0;
        if (intent != null && !serviceRestarted) {
            mResultCode = intent.getIntExtra("result", 0);
            final String action = intent.getAction();
            final String dataType = intent.getType();

            if (ACTION_SMS_RECEIVED.equals(action)) {
                handleSmsReceived(intent);
            } else if (ACTION_MMS_RECEIVED.equals(action) && MMS_DATA_TYPE.equals(dataType)) {
                handleMmsReceived(intent);
            } else if (MESSAGE_SENT_ACTION.equals(action)) {
                handleSmsSent(intent);
            } else if (ACTION_MESSAGE_RECEIVED.equals(action)) {
                handleMessageReceived(intent);
            }
        }
        SmsReceiver.completeWakefulIntent(intent);
    }

    /**
     * Handle receiving a SMS message
     */
    @TargetApi(VERSION_CODES.KITKAT)
    private void handleSmsReceived(Intent intent) {
        if (BuildConfig.DEBUG)
            Log.v("SMSReceiver: Intercept SMS");

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            SmsMessage[] messages = null;
            if (SmsPopupUtils.hasKitKat()) {
                messages = Intents.getMessagesFromIntent(intent);
            } else {
                messages = SmsPopupUtils.getMessagesFromIntent(intent);
            }
            if (messages != null) {
                notifyMessageReceived(new SmsMmsMessage(
                        context, messages, System.currentTimeMillis()));
            }
        }
    }

    private void notifyMessageReceived(SmsMmsMessage message) {

        // Class 0 SMS, let the system handle this
        if (message.isSms() && message.getMessageClass() == MessageClass.CLASS_0) {
            return;
        }

        if (message.isSprintVisualVoicemail()) {
            return;
        }

        // Unknown sender and empty body, ignore
        if (context.getString(android.R.string.unknownName).equals(message.getContactName())
                && "".equals(message.getMessageBody())) {
            return;
        }

        // Fetch preferences
        ManagePreferences mPrefs = new ManagePreferences(
                context, message.getContactId(), message.getContactLookupKey());

        // Whether or not the popup should only show when keyguard is on
        boolean onlyShowOnKeyguard =
                mPrefs.getBoolean(R.string.pref_onlyShowOnKeyguard_key,
                        Defaults.PREFS_ONLY_SHOW_ON_KEYGUARD);

        // check if popup is enabled for this contact
        boolean showPopup =
                mPrefs.getBoolean(R.string.pref_popup_enabled_key,
                        Defaults.PREFS_SHOW_POPUP,
                        ContactNotifications.POPUP_ENABLED);

        // check if notifications are on for this contact
        boolean notifEnabled =
                mPrefs.getBoolean(R.string.pref_notif_enabled_key,
                        Defaults.PREFS_NOTIF_ENABLED,
                        ContactNotifications.ENABLED);

        // get docked state of phone
        boolean docked = mPrefs.getInt(R.string.pref_docked_key, Intent.EXTRA_DOCK_STATE_UNDOCKED)
                != Intent.EXTRA_DOCK_STATE_UNDOCKED;

        mPrefs.close();

        // Fetch call state, if the user is in a call or the phone is ringing we don't want
        // to show the popup
        TelephonyManager mTM =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        boolean callStateIdle = mTM.getCallState() == TelephonyManager.CALL_STATE_IDLE;

        // Init keyguard manager
        ManageKeyguard.initialize(context);

        /*
         * If popup is enabled for this user -AND- the user is not in a call --AND- phone is
         * not docked -AND- (screen is locked -OR- (setting is OFF to only show on keyguard -AND-
         * user is not in messaging app: then show the popup activity, otherwise check if
         * notifications are on and just use the standard notification))
         */
        if (showPopup && callStateIdle && !docked
                && (ManageKeyguard.inKeyguardRestrictedInputMode() ||
                (!onlyShowOnKeyguard && !SmsPopupUtils.inMessagingApp(context)))) {

            if (BuildConfig.DEBUG)
                Log.v("^^^^^^Showing SMS Popup");
            ManageWakeLock.acquirePartial(context);
            context.startActivity(message.getPopupIntent());

        } else if (notifEnabled) {

            if (BuildConfig.DEBUG)
                Log.v("^^^^^^Not showing SMS Popup, using notifications");
            ManageNotification.show(context, message, message == null ? 0 : message.getUnreadCount());
            ReminderService.scheduleReminder(context, message);

        }
    }

    /**
     * Handle receiving a MMS message
     */
    private void handleMmsReceived(Intent intent) {
        if (BuildConfig.DEBUG)
            Log.v("MMS received!");
        SmsMmsMessage mmsMessage = null;
        int count = 0;

        // Ok this is super hacky, but fixes the case where this code
        // runs before the system MMS transaction service (that stores
        // the MMS details in the database). This should really be
        // a content listener that waits for a while then gives up...
        while (mmsMessage == null && count < MESSAGE_RETRY) {

            mmsMessage = SmsPopupUtils.getMmsDetails(context);

            if (mmsMessage != null) {
                if (BuildConfig.DEBUG)
                    Log.v("MMS found in content provider");
                notifyMessageReceived(mmsMessage);
            } else {
                if (BuildConfig.DEBUG)
                    Log.v("MMS not found, sleeping (count is " + count + ")");
                count++;
                try {
                    Thread.sleep(MESSAGE_RETRY_PAUSE);
                } catch (InterruptedException e) {
                    // e.printStackTrace();
                }
            }
        }
    }

    /**
     * Handle receiving an arbitrary message (potentially coming from a 3rd party app)
     */
    private void handleMessageReceived(Intent intent) {
        if (BuildConfig.DEBUG)
            Log.v("SMSReceiver: Intercept Message");

        Bundle bundle = intent.getExtras();

        /*
         * FROM: ContactURI -or- display name and display address -or- display address MESSAGE BODY:
         * message body TIMESTAMP: optional (will use system timestamp)
         *
         * QUICK REPLY INTENT: REPLY INTENT: DELETE INTENT:
         */

        if (bundle != null) {

            // notifySmsReceived(new SmsMmsMessage(context, messages, System.currentTimeMillis()));
        }
    }

    private void showToast(String message) {
        mToastHandler.sendMessage(
                Message.obtain(mToastHandler, TOAST_HANDLER_MESSAGE_CUSTOM, message));
    }

    /*
     * Handler to deal with showing Toast messages for message sent status
     */
    public Handler mToastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (msg != null) {
                switch (msg.what) {
                case TOAST_HANDLER_MESSAGE_SENT:
                    Toast.makeText(SmsReceiverService.this,
                            SmsReceiverService.this.getString(R.string.quickreply_sent_toast),
                            Toast.LENGTH_SHORT).show();
                    break;
                case TOAST_HANDLER_MESSAGE_SEND_LATER:
                    Toast.makeText(
                            SmsReceiverService.this,
                            SmsReceiverService.this
                                    .getString(R.string.quickreply_failed_send_later),
                            Toast.LENGTH_LONG).show();
                    break;
                case TOAST_HANDLER_MESSAGE_FAILED:
                    Toast.makeText(SmsReceiverService.this,
                            SmsReceiverService.this.getString(R.string.quickreply_failed),
                            Toast.LENGTH_LONG).show();
                    break;
                case TOAST_HANDLER_MESSAGE_CUSTOM:
                    Toast.makeText(SmsReceiverService.this,
                            msg.obj.toString(),
                            Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    };

    /*
     * Handle the result of a sms being sent
     */
    private void handleSmsSent(Intent intent) {
        if (BuildConfig.DEBUG)
            Log.v("SMSReceiver: Handle SMS sent");

        PackageManager pm = getPackageManager();
        Intent sysIntent = null;
        Intent tempIntent;
        List<ResolveInfo> receiverList;
        boolean forwardToSystemApp = true;

        // Search for system messaging app that will receive our "message sent complete" type intent
        tempIntent = intent.setClassName(
                SmsMessageSender.MESSAGING_PACKAGE_NAME,
                SmsMessageSender.MESSAGING_RECEIVER_CLASS_NAME);

        receiverList = pm.queryBroadcastReceivers(tempIntent, 0);

        if (receiverList.size() > 0) {
            if (BuildConfig.DEBUG) {
                Log.v("SMSReceiver: Found system messaging app - " + receiverList.get(0).toString());
            }

            sysIntent = tempIntent;

            // This is quite a hack - it seems most OEMs will replace the stock android messaging
            // app, however Samsung changes it but keeps the same package name. One change is that
            // it won't finish moving the messaging from "outbox" to "sent" or "failed" for us so
            // this checks to see if the modified samsung app is there and if so, we'll handle the
            // final move of the message ourselves.

            // Only the samsung sms/mms apk has this modified compose class
            final Intent samsungIntent = new Intent();
            samsungIntent.setClassName(
                    SmsMessageSender.MESSAGING_PACKAGE_NAME,
                    SmsMessageSender.SAMSUNG_MESSAGING_COMPOSE_CLASS_NAME);
            receiverList = pm.queryIntentActivities(samsungIntent, 0);
            if (receiverList.size() > 0) {
                // no stock system app found to finish the message move
                sysIntent = null;
            }
        }

        /*
         * No system messaging app was found to forward this intent to, therefore we will need to do
         * the final piece of this ourselves which is basically moving the message to the correct
         * folder depending on the result.
         */
        if (sysIntent == null) {
            forwardToSystemApp = false;
            if (BuildConfig.DEBUG)
                Log.v("SMSReceiver: Did not find system messaging app, moving messages directly");

            Uri uri = intent.getData();

            if (mResultCode == Activity.RESULT_OK) {
                SmsMessageSender.moveMessageToFolder(this, uri, SmsMessageSender.MESSAGE_TYPE_SENT);
            } else if ((mResultCode == SmsManager.RESULT_ERROR_RADIO_OFF) ||
                    (mResultCode == SmsManager.RESULT_ERROR_NO_SERVICE)) {
                SmsMessageSender.moveMessageToFolder(this, uri,
                        SmsMessageSender.MESSAGE_TYPE_QUEUED);
            } else {
                SmsMessageSender.moveMessageToFolder(this, uri,
                        SmsMessageSender.MESSAGE_TYPE_FAILED);
            }
        }

        // Check the result and notify the user using a toast
        if (mResultCode == Activity.RESULT_OK) {
            if (BuildConfig.DEBUG)
                Log.v("SMSReceiver: Message was sent");
            mToastHandler.sendEmptyMessage(TOAST_HANDLER_MESSAGE_SENT);

        } else if ((mResultCode == SmsManager.RESULT_ERROR_RADIO_OFF) ||
                (mResultCode == SmsManager.RESULT_ERROR_NO_SERVICE)) {
            if (BuildConfig.DEBUG)
                Log.v("SMSReceiver: Error sending message (will send later)");
            // The system shows a Toast here so no need to show one
            // mToastHandler.sendEmptyMessage(TOAST_HANDLER_MESSAGE_SEND_LATER);

        } else {
            if (BuildConfig.DEBUG)
                Log.v("SMSReceiver: Error sending message");
            // ManageNotification.notifySendFailed(this);
            mToastHandler.sendEmptyMessage(TOAST_HANDLER_MESSAGE_FAILED);
        }

        /*
         * Start the broadcast via PendingIntent so result code is passed over correctly
         */
        if (forwardToSystemApp) {
            try {
                Log.v("SMSReceiver: Broadcasting send complete to system messaging app");
                PendingIntent.getBroadcast(this, 0, sysIntent, 0).send(mResultCode);
            } catch (CanceledException e) {
                e.printStackTrace();
            }
        }
    }
}