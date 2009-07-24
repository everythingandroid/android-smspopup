package net.everythingandroid.smspopup;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;

public class SmsMonitorService extends Service {
  Uri uriSMS = Uri.parse("content://mms-sms/conversations/");
  public static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
  public static final Uri SMS_INBOX_CONTENT_URI = Uri.withAppendedPath(SMS_CONTENT_URI, "inbox");

  ContentResolver crSMS;
  SmsContentObserver observerSMS = null;
  Context context;

  public static void beginStartingService(Context context) {
    //synchronized (mStartingServiceSync) {
    if (Log.DEBUG) Log.v("SmsMonitorService: beginStartingService()");
    //      if (mStartingService == null) {
    //        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    //        mStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
    //            Log.LOGTAG);
    //        mStartingService.setReferenceCounted(false);
    //      }
    //      mStartingService.acquire();
    context.startService(new Intent(context, SmsMonitorService.class));
    //}
  }

  @Override
  public void onStart(Intent intent, int startId) {
    super.onStart(intent, startId);
    if (Log.DEBUG) Log.v("SmsMonitorService starting");
  }

  @Override
  public void onCreate() {
    super.onCreate();
    context = this.getApplicationContext();
    registerSMSObserver();
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
      //crSMS.notifyChange(uriSMS, observerSMS);
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

  public class SmsContentObserver extends ContentObserver {
    public SmsContentObserver(Handler handler) {
      super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
      super.onChange(selfChange);
      if (Log.DEBUG) Log.v("!!! SmsContentObserver - content changed!");
      Cursor c = context.getContentResolver().query(SMS_CONTENT_URI, null, "read = 0", null, null);
      if (Log.DEBUG) Log.v("!!! SMS count unread " + c.getCount());
      if (Log.DEBUG) Log.v("!!! selfChange = " + selfChange);
    }
  }

}
