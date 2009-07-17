package net.everythingandroid.smspopup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SMSReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    Log.v("SMSReceiver: onReceive()");
    intent.setClass(context, SMSReceiverService.class);
    intent.putExtra("result", getResultCode());
    SMSReceiverService.beginStartingService(context, intent);
  }
}