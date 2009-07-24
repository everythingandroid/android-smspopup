package net.everythingandroid.smspopup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class EnableDisableReceiver extends BroadcastReceiver {
  public static final String ACTION_SMSPOPUP_ENABLE = "net.everythingandroid.smspopup.ENABLE";
  public static final String ACTION_SMSPOPUP_DISABLE = "net.everythingandroid.smspopup.DISABLE";

  @Override
  public void onReceive(Context context, Intent intent) {
    if (Log.DEBUG) Log.v("ExternalEventReceiver: onReceive()");

    String action = intent.getAction();

    if (ACTION_SMSPOPUP_ENABLE.equals(action)) {
      SmsPopupUtils.enableSMSPopup(context, true);
    } else if (ACTION_SMSPOPUP_DISABLE.equals(action)) {
      SmsPopupUtils.enableSMSPopup(context, false);
    }
  }
}
