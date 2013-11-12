package net.everythingandroid.smspopup.receiver;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import net.everythingandroid.smspopup.BuildConfig;
import net.everythingandroid.smspopup.service.SmsReceiverService;
import net.everythingandroid.smspopup.util.Log;

public class SmsReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.DEBUG)
            Log.v("SMSReceiver: onReceive()");
        intent.setClass(context, SmsReceiverService.class);
        intent.putExtra("result", getResultCode());
        startWakefulService(context, intent);
    }
}