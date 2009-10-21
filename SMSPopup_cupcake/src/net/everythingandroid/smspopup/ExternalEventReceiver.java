package net.everythingandroid.smspopup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ExternalEventReceiver extends BroadcastReceiver {
  public static final String ACTION_SMSPOPUP_ENABLE = "net.everythingandroid.smspopup.ENABLE";
  public static final String ACTION_SMSPOPUP_DISABLE = "net.everythingandroid.smspopup.DISABLE";
  public static final String ACTION_SMSPOPUP_DONATED = "net.everythingandroid.smspopup.DONATED";

  @Override
  public void onReceive(Context context, Intent intent) {
    if (Log.DEBUG) Log.v("ExternalEventReceiver: onReceive()");

    String action = intent.getAction();

    if (ACTION_SMSPOPUP_ENABLE.equals(action)) {
      SmsPopupUtils.enableSMSPopup(context, true);
    } else if (ACTION_SMSPOPUP_DISABLE.equals(action)) {
      SmsPopupUtils.enableSMSPopup(context, false);
    } else if (ACTION_SMSPOPUP_DONATED.equals(action)) {
      SharedPreferences.Editor settings =
        PreferenceManager.getDefaultSharedPreferences(context).edit();
      settings.putBoolean(context.getString(R.string.pref_donated_key), true);
      settings.commit();
    }
  }
}
