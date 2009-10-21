package net.everythingandroid.smspopup;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;

public class SmsMonitorService extends Service {
  private static Uri uriSMS = Uri.parse("content://mms-sms/conversations/");
  //  private static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
  //  private static final Uri SMS_INBOX_CONTENT_URI = Uri.withAppendedPath(SMS_CONTENT_URI, "inbox");

  private ContentResolver crSMS;
  private SmsContentObserver observerSMS = null;
  private Context context;

  // private static PowerManager.WakeLock mStartingService;

  @Override
  public void onCreate() {
    super.onCreate();
    context = this.getApplicationContext();
    registerSMSObserver();
  }

  @Override
  public void onStart(Intent intent, int startId) {
    super.onStart(intent, startId);
    if (Log.DEBUG) Log.v("SmsMonitorService starting");
  }

  @Override
  public void onDestroy() {
    unregisterSMSObserver();
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  /**
   * Registers the observer for SMS changes
   */
  private void registerSMSObserver() {
    if (observerSMS == null) {
      observerSMS = new SmsContentObserver(new Handler());
      crSMS = getContentResolver();
      crSMS.registerContentObserver(uriSMS, true, observerSMS);
      if (Log.DEBUG) Log.v("SMS Observer registered.");
    }
  }

  /**
   * Unregisters the observer for call log changes
   */
  private void unregisterSMSObserver() {
    if (crSMS != null) {
      crSMS.unregisterContentObserver(observerSMS);
    }
    if (observerSMS != null) {
      observerSMS = null;
    }
    if (Log.DEBUG) Log.v("Unregistered SMS Observer");
  }

  private class SmsContentObserver extends ContentObserver {
    public SmsContentObserver(Handler handler) {
      super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
      super.onChange(selfChange);
      //Cursor c = context.getContentResolver().query(SMS_CONTENT_URI, null, "read = 0", null, null);
      int count = SmsPopupUtils.getUnreadMessagesCount(context);
      if (Log.DEBUG) Log.v("getUnreadCount = " + count);
      if (count == 0) {
        ManageNotification.clearAll(context);
        finishStartingService(SmsMonitorService.this);
      } else {
        // TODO: do something with count>0, maybe refresh the notification
      }
    }
  }

  /**
   * Start the service to process that will run the content observer
   */
  public static void beginStartingService(Context context) {
    if (Log.DEBUG) Log.v("SmsMonitorService: beginStartingService()");
    //    if (mStartingService == null) {
    //      PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    //      mStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Log.LOGTAG);
    //      mStartingService.setReferenceCounted(false);
    //    }
    //    mStartingService.acquire();
    context.startService(new Intent(context, SmsMonitorService.class));
  }

  /**
   * Called back by the service when it has finished processing notifications,
   * releasing the wake lock if the service is now stopping.
   */
  public static void finishStartingService(Service service) {
    if (Log.DEBUG) Log.v("SmsMonitorService: finishStartingService()");
    //    if (mStartingService != null) {
    service.stopSelf();
    //      mStartingService.release();
    //      mStartingService = null;
    //}
  }

}
