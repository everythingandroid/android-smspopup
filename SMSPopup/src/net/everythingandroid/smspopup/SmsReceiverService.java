package net.everythingandroid.smspopup;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.preference.PreferenceManager;
import android.telephony.gsm.SmsMessage;
import android.telephony.gsm.SmsMessage.MessageClass;

public class SmsReceiverService extends Service {
  private static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
  private static final String ACTION_MMS_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED";
  private static final String MMS_DATA_TYPE = "application/vnd.wap.mms-message";

  /*
   * This is the number of retries and pause between retries that we will keep
   * checking the system message database for the latest incoming message
   */
  private static final int MESSAGE_RETRY = 8;
  private static final int MESSAGE_RETRY_PAUSE = 500;

  private Context context;
  private ServiceHandler mServiceHandler;
  private Looper mServiceLooper;
  // private int mResultCode;

  private static final Object mStartingServiceSync = new Object();
  private static PowerManager.WakeLock mStartingService;

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

    // mResultCode = intent.getIntExtra("result", 0);
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
        notifySmsReceived(new SmsMmsMessage(context, messages, System.currentTimeMillis()));
      }
    }
  }

  /**
   * Notify the user of the SMS - either via notification bar or popup
   */
  private void notifySmsReceived(SmsMmsMessage smsMessage) {

    // Class 0 message, let the system handle this
    if (smsMessage.getMessageClass() == MessageClass.CLASS_0) return;

    ManagePreferences mPrefs = new ManagePreferences(context, smsMessage.getContactId());

    boolean onlyShowOnKeyguard =
      mPrefs.getBoolean(R.string.pref_onlyShowOnKeyguard_key,
          R.string.pref_onlyShowOnKeyguard_default);

    boolean showPopup =
      mPrefs.getBoolean(R.string.pref_popup_enabled_key, R.string.pref_popup_enabled_default,
          SmsPopupDbAdapter.KEY_POPUP_ENABLED_NUM);

    boolean notifEnabled =
      mPrefs.getBoolean(R.string.pref_notif_enabled_key, R.string.pref_notif_enabled_default,
          SmsPopupDbAdapter.KEY_ENABLED_NUM);

    mPrefs.close();

    ManageKeyguard.initialize(context);

    if (showPopup
        && (ManageKeyguard.inKeyguardRestrictedInputMode() || (!onlyShowOnKeyguard &&
            !SmsPopupUtils.inMessagingApp(context)))) {
      if (Log.DEBUG) Log.v("^^^^^^Showing SMS Popup");
      Intent popup = smsMessage.getPopupIntent();
      ManageWakeLock.acquirePartial(context);
      context.startActivity(popup);
    } else if (notifEnabled) {
      if (Log.DEBUG) Log.v("^^^^^^Not showing SMS Popup, using notifications");
      ManageNotification.show(context, smsMessage);
      ReminderReceiver.scheduleReminder(context, smsMessage);
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
        SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean onlyShowOnKeyguard =
          myPrefs.getBoolean(context.getString(R.string.pref_onlyShowOnKeyguard_key),
              Boolean.valueOf(context.getString(R.string.pref_onlyShowOnKeyguard_default)));

        if (ManageKeyguard.inKeyguardRestrictedInputMode() || !onlyShowOnKeyguard) {
          if (Log.DEBUG)
            Log.v("^^^^^^In keyguard or pref set to always show - showing popup activity");
          Intent popup = mmsMessage.getPopupIntent();
          ManageWakeLock.acquirePartial(context);
          context.startActivity(popup);
        } else {
          if (Log.DEBUG) Log.v("^^^^^^Not in keyguard, only using notification");
          ManageNotification.show(context, mmsMessage);
          ReminderReceiver.scheduleReminder(context, mmsMessage);
        }
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
   * Start the service to process the current event notifications, acquiring the
   * wake lock before returning to ensure that the service will run.
   */
  public static void beginStartingService(Context context, Intent intent) {
    synchronized (mStartingServiceSync) {
      if (Log.DEBUG) Log.v("SMSReceiverService: beginStartingService()");
      if (mStartingService == null) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Log.LOGTAG);
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
