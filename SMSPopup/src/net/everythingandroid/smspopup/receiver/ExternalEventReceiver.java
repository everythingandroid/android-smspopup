package net.everythingandroid.smspopup.receiver;

import net.everythingandroid.smspopup.BuildConfig;
import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ExternalEventReceiver extends BroadcastReceiver {
	public static final String ACTION_SMSPOPUP_DONATED = "net.everythingandroid.smspopup.DONATED";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (BuildConfig.DEBUG) Log.v("ExternalEventReceiver: onReceive()");

		String action = intent.getAction();

		if (ACTION_SMSPOPUP_DONATED.equals(action)) {
			SharedPreferences.Editor settings = 
					PreferenceManager.getDefaultSharedPreferences(context).edit();
			settings.putBoolean(context.getString(R.string.pref_donated_key), true);
			settings.commit();
		} else if (Intent.ACTION_DOCK_EVENT.equals(action)) {

			SharedPreferences.Editor settings = 
					PreferenceManager.getDefaultSharedPreferences(context).edit();

			int event = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
			settings.putInt(context.getString(R.string.pref_docked_key), event);
			settings.commit();

		}
	}
}
