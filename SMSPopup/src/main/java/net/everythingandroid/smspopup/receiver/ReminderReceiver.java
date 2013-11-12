// Copyright 2011 Google Inc. All Rights Reserved.

package net.everythingandroid.smspopup.receiver;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import net.everythingandroid.smspopup.service.ReminderService;

public class ReminderReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setClass(context, ReminderService.class);
        startWakefulService(context, intent);
    }
}
