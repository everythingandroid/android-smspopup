package net.everythingandroid.smspopup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ExternalEventReceiver extends BroadcastReceiver {
  public static final String ACTION_SMSPOPUP_ENABLE = "net.everythingandroid.smspopup.ENABLE";
  public static final String ACTION_SMSPOPUP_DISABLE = "net.everythingandroid.smspopup.DISABLE";

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.v("ExternalEventReceiver: onReceive()");

    String action = intent.getAction();

    if (ACTION_SMSPOPUP_ENABLE.equals(action)) {
      SMSPopupUtils.enableSMSPopup(context, true);
    } else if (ACTION_SMSPOPUP_DISABLE.equals(action)) {
      SMSPopupUtils.enableSMSPopup(context, false);
    }
  }
}
