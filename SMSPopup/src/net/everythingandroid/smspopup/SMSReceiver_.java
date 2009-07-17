package net.everythingandroid.smspopup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SMSReceiver_ extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    Log.v("SMSReceiver: onReceive()");
    intent.setClass(context, SMSReceiverService_.class);
    intent.putExtra("result", getResultCode());
    SMSReceiverService_.beginStartingService(context, intent);
  }
}