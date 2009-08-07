package net.everythingandroid.smspopup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SmsReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    if (Log.DEBUG) Log.v("SMSReceiver: onReceive()");
    intent.setClass(context, SmsReceiverService.class);
    intent.putExtra("result", getResultCode());

    /*
     * This service will process the activity and show the popup (+ play notifications)
     * after it's work is done the service will be stopped.
     */
    SmsReceiverService.beginStartingService(context, intent);

    /*
     *  This service runs a content observer on the system sms db to help clear the notification
     *  icon in the case the user reads the messages outside of sms popup.  the service will be
     *  stopped when unread messages = 0
     */
    SmsMonitorService.beginStartingService(context);
  }
}