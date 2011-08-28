package net.everythingandroid.smspopup;

import java.util.List;

import net.everythingandroid.smspopup.ManagePreferences.Defaults;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SmsMessage.MessageClass;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class SmsReceiverService extends Service {
  private static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
  private static final String ACTION_MMS_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED";
  private static final String ACTION_MESSAGE_RECEIVED = "net.everythingandroid.smspopup.MESSAGE_RECEIVED";
  private static final String MMS_DATA_TYPE = "application/vnd.wap.mms-message";

  // http://android.git.kernel.org/?p=platform/packages/apps/Mms.git;a=blob;f=src/com/android/mms/transaction/SmsReceiverService.java
  public static final String MESSAGE_SENT_ACTION = "com.android.mms.transaction.MESSAGE_SENT";

  /*
   * This is the number of retries and pause between retries that we will keep
   * checking the system message database for the latest incoming message
   */
  private static final int MESSAGE_RETRY = 8;
  private static final int MESSAGE_RETRY_PAUSE = 1000;

  private Context context;
  private ServiceHandler mServiceHandler;
  private Looper mServiceLooper;
  private int mResultCode;

  private static final Object mStartingServiceSync = new Object();
  private static PowerManager.WakeLock mStartingService;

  private static final int TOAST_HANDLER_MESSAGE_SENT = 0;
  private static final int TOAST_HANDLER_MESSAGE_SEND_LATER = 1;
  private static final int TOAST_HANDLER_MESSAGE_FAILED = 2;

  @Override
  public void onCreate() {
    if (Log.DEBUG) Log.v("SMSReceiverService: onCreate()");
    HandlerThread thread = new HandlerThread(Log.LOGTAG, Process.THREAD_PRIORITY_BACKGROUND);
    thread.start();
    context = getApplicationContext();
    mServiceLooper = thread.getLooper();
    mServiceHandler = new ServiceHandler(mServiceLooper);
  }

  @Override
  public void onStart(Intent intent, int startId) {
    if (Log.DEBUG) Log.v("SMSReceiverService: onStart()");

    mResultCode = intent != null ? intent.getIntExtra("result", 0) : 0;

    Message msg = mServiceHandler.obtainMessage();
    msg.arg1 = startId;
    msg.obj = intent;
    mServiceHandler.sendMessage(msg);
  }

  @Override
  public void onDestroy() {
    if (Log.DEBUG) Log.v("SMSReceiverService: onDestroy()");
    mServiceLooper.quit();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private final class ServiceHandler extends Handler {
    public ServiceHandler(Looper looper) {
      super(looper);
    }

    @Override
    public void handleMessage(Message msg) {
      if (Log.DEBUG) Log.v("SMSReceiverService: handleMessage()");

      int serviceId = msg.arg1;
      Intent intent = (Intent) msg.obj;
      String action = intent.getAction();
      String dataType = intent.getType();

      if (ACTION_SMS_RECEIVED.equals(action)) {
        handleSmsReceived(intent);
      } else if (ACTION_MMS_RECEIVED.equals(action) && MMS_DATA_TYPE.equals(dataType)) {
        handleMmsReceived(intent);
      } else if (MESSAGE_SENT_ACTION.equals(action)) {
        handleSmsSent(intent);
      } else if (ACTION_MESSAGE_RECEIVED.equals(action)) {
        handleMessageReceived(intent);
      }

      // NOTE: We MUST not call stopSelf() directly, since we need to
      // make sure the wake lock acquired by AlertReceiver is released.
      finishStartingService(SmsReceiverService.this, serviceId);
    }
  }

  /**
   * Handle receiving a SMS message
   */
  private void handleSmsReceived(Intent intent) {
    if (Log.DEBUG) Log.v("SMSReceiver: Intercept SMS");

    Bundle bundle = intent.getExtras();
    if (bundle != null) {
      SmsMessage[] messages = SmsPopupUtils.getMessagesFromIntent(intent);
      if (messages != null) {
        notifyMessageReceived(new SmsMmsMessage(context, messages, System.currentTimeMillis()));
      }
    }
  }

  private void notifyMessageReceived(SmsMmsMessage message) {

    // Class 0 SMS, let the system handle this
    if (message.getMessageType() == SmsMmsMessage.MESSAGE_TYPE_SMS &&
        message.getMessageClass() == MessageClass.CLASS_0) {
      return;
    }

    if (message.isSprintVisualVoicemail()) {
      return;
    }

    // Fetch preferences
    ManagePreferences mPrefs = new ManagePreferences(context, message.getContactId());

    // Whether or not the popup should only show when keyguard is on
    boolean onlyShowOnKeyguard =
      mPrefs.getBoolean(R.string.pref_onlyShowOnKeyguard_key,
          Defaults.PREFS_ONLY_SHOW_ON_KEYGUARD);

    // check if popup is enabled for this contact
    boolean showPopup =
      mPrefs.getBoolean(R.string.pref_popup_enabled_key,
          Defaults.PREFS_SHOW_POPUP,
          SmsPopupDbAdapter.KEY_POPUP_ENABLED_NUM);

    // check if notifications are on for this contact
    boolean notifEnabled =
      mPrefs.getBoolean(R.string.pref_notif_enabled_key,
          Defaults.PREFS_NOTIF_ENABLED,
          SmsPopupDbAdapter.KEY_ENABLED_NUM);

    // get docked state of phone
    int docked_state =
      mPrefs.getInt(R.string.pref_docked_key, 0);

    boolean docked = docked_state == ExternalEventReceiver.EXTRA_DOCK_STATE_DESK;

    mPrefs.close();

    // Fetch call state, if the user is in a call or the phone is ringing we don't want to show the popup
    TelephonyManager mTM = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    boolean callStateIdle = mTM.getCallState() == TelephonyManager.CALL_STATE_IDLE;

    // Init keyguard manager
    ManageKeyguard.initialize(context);

    /*
     * If popup is enabled for this user -AND- the user is not in a call -AND-
     * -AND- phone is not docked -AND-
     * (screen is locked -OR- (setting is OFF to only show on keyguard -AND- user is not in messaging app:
     * then show the popup activity, otherwise check if notifications are on and just use the standard
     * notification))
     */
    if (showPopup && callStateIdle && !docked
        && (ManageKeyguard.inKeyguardRestrictedInputMode() ||
            (!onlyShowOnKeyguard && !SmsPopupUtils.inMessagingApp(context)))) {

      if (Log.DEBUG) Log.v("^^^^^^Showing SMS Popup");
      ManageWakeLock.acquireFull(context);
      context.startActivity(message.getPopupIntent());

    } else if (notifEnabled) {

      if (Log.DEBUG) Log.v("^^^^^^Not showing SMS Popup, using notifications");
      ManageNotification.show(context, message);
      ReminderReceiver.scheduleReminder(context, message);

    }
  }

  /**
   * Handle receiving a MMS message
   */
  private void handleMmsReceived(Intent intent) {
    if (Log.DEBUG) Log.v("MMS received!");
    SmsMmsMessage mmsMessage = null;
    int count = 0;

    // Ok this is super hacky, but fixes the case where this code
    // runs before the system MMS transaction service (that stores
    // the MMS details in the database). This should really be
    // a content listener that waits for a while then gives up...
    while (mmsMessage == null && count < MESSAGE_RETRY) {

      mmsMessage = SmsPopupUtils.getMmsDetails(context);

      if (mmsMessage != null) {
        if (Log.DEBUG) Log.v("MMS found in content provider");
        notifyMessageReceived(mmsMessage);
      } else {
        if (Log.DEBUG) Log.v("MMS not found, sleeping (count is " + count + ")");
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
    if (Log.DEBUG) Log.v("SMSReceiver: Intercept Message");

    Bundle bundle = intent.getExtras();

    /*
     * FROM: ContactURI -or- display name and display address -or- display address
     * MESSAGE BODY: message body
     * TIMESTAMP: optional (will use system timestamp)
     *
     * QUICK REPLY INTENT:
     * REPLY INTENT:
     * DELETE INTENT:
     */

    if (bundle != null) {

      //notifySmsReceived(new SmsMmsMessage(context, messages, System.currentTimeMillis()));
    }
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
            Toast.makeText(SmsReceiverService.this,
                SmsReceiverService.this.getString(R.string.quickreply_failed_send_later),
                Toast.LENGTH_SHORT).show();
            break;
          case TOAST_HANDLER_MESSAGE_FAILED:
            Toast.makeText(SmsReceiverService.this,
                SmsReceiverService.this.getString(R.string.quickreply_failed),
                Toast.LENGTH_SHORT).show();
            break;
        }
      }
    }
  };

  /*
   * Handle the result of a sms being sent
   */
  private void handleSmsSent(Intent intent) {
    if (Log.DEBUG) Log.v("SMSReceiver: Handle SMS sent");

    PackageManager pm = getPackageManager();
    Intent sysIntent = null;
    Intent tempIntent;
    List<ResolveInfo> receiverList;
    boolean forwardToSystemApp = true;

    // Search for system messaging app that will receive our "message sent complete" type intent
    tempIntent = intent.setClassName(
        SmsMessageSender.MESSAGING_PACKAGE_NAME,
        SmsMessageSender.MESSAGING_RECEIVER_CLASS_NAME);

    tempIntent.setAction(SmsReceiverService.MESSAGE_SENT_ACTION);

    receiverList = pm.queryBroadcastReceivers(tempIntent, 0);

    if (receiverList.size() > 0) {
      if (Log.DEBUG) Log.v("SMSReceiver: Found system messaging app - " + receiverList.get(0).toString());
      sysIntent = tempIntent;
    }

    /*
     * No system messaging app was found to forward this intent to, therefore we will need to do
     * the final piece of this ourselves which is basically moving the message to the correct
     * folder depending on the result.
     */
    if (sysIntent == null) {
      forwardToSystemApp = false;
      if (Log.DEBUG) Log.v("SMSReceiver: Did not find system messaging app, moving messages directly");

      Uri uri = intent.getData();

      if (mResultCode == Activity.RESULT_OK) {
        SmsMessageSender.moveMessageToFolder(this, uri, SmsMessageSender.MESSAGE_TYPE_SENT);
      } else if ((mResultCode == SmsManager.RESULT_ERROR_RADIO_OFF) ||
          (mResultCode == SmsManager.RESULT_ERROR_NO_SERVICE)) {
        SmsMessageSender.moveMessageToFolder(this, uri, SmsMessageSender.MESSAGE_TYPE_QUEUED);
      } else {
        SmsMessageSender.moveMessageToFolder(this, uri, SmsMessageSender.MESSAGE_TYPE_FAILED);
      }
    }

    // Check the result and notify the user using a toast
    if (mResultCode == Activity.RESULT_OK) {
      if (Log.DEBUG) Log.v("SMSReceiver: Message was sent");
      mToastHandler.sendEmptyMessage(TOAST_HANDLER_MESSAGE_SENT);

    } else if ((mResultCode == SmsManager.RESULT_ERROR_RADIO_OFF) ||
        (mResultCode == SmsManager.RESULT_ERROR_NO_SERVICE)) {
      if (Log.DEBUG) Log.v("SMSReceiver: Error sending message (will send later)");
      // The system shows a Toast here so no need to show one
      //mToastHandler.sendEmptyMessage(TOAST_HANDLER_MESSAGE_SEND_LATER);

    } else {
      if (Log.DEBUG) Log.v("SMSReceiver: Error sending message");
      //ManageNotification.notifySendFailed(this);
      mToastHandler.sendEmptyMessage(TOAST_HANDLER_MESSAGE_FAILED);
    }

    /*
     * Now let's forward the same intent onto the system app to make sure
     * things there are processed correctly
     */
    //    sysIntent = intent.setClassName(
    //        SmsMessageSender.MMS_PACKAGE_NAME,
    //        SmsMessageSender.MMS_SENT_CLASS_NAME);

    //    Log.v("sysIntent = " + sysIntent.toString());
    //    Log.v("bundle = " + sysIntent.getExtras().toString());

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

  /**
   * Start the service to process the current event notifications, acquiring the
   * wake lock before returning to ensure that the service will run.
   */
  public static void beginStartingService(Context context, Intent intent) {
    synchronized (mStartingServiceSync) {
      if (Log.DEBUG) Log.v("SMSReceiverService: beginStartingService()");
      if (mStartingService == null) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
            Log.LOGTAG+".SmsReceiverService");
        mStartingService.setReferenceCounted(false);
      }
      mStartingService.acquire();
      context.startService(intent);
    }
  }

  /**
   * Called back by the service when it has finished processing notifications,
   * releasing the wake lock if the service is now stopping.
   */
  public static void finishStartingService(Service service, int startId) {
    synchronized (mStartingServiceSync) {
      if (Log.DEBUG) Log.v("SMSReceiverService: finishStartingService()");
      if (mStartingService != null) {
        if (service.stopSelfResult(startId)) {
          mStartingService.release();
        }
      }
    }
  }

}
