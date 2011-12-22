// Copyright 2011 Google Inc. All Rights Reserved.

package net.everythingandroid.smspopup.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import net.everythingandroid.smspopup.service.ReminderService;

public class ReminderReceiver extends BroadcastReceiver {

    /* (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setClass(context, ReminderService.class);
        WakefulIntentService.sendWakefulWork(context, intent);
    }

}
