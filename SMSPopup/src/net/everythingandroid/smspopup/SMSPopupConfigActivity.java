package net.everythingandroid.smspopup;

import net.everythingandroid.smspopup.preferences.AppEnabledCheckBoxPreference;
import net.everythingandroid.smspopup.preferences.EmailDialogPreference;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SMSPopupConfigActivity extends PreferenceActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		//Try and find app version number
		String version = "";
		PackageManager pm = this.getPackageManager();
		try {
			//Get version number, not sure if there is a better way to do this
			version = " v" +
			pm.getPackageInfo(
				SMSPopupConfigActivity.class.getPackage().getName(), 0).versionName;
		} catch (NameNotFoundException e) {
			//No need to do anything here if it fails
			//e.printStackTrace();
		}
		
		// Set the version number in the about dialog preference
		DialogPreference aboutPref =
			(DialogPreference) findPreference(getString(R.string.pref_about_key));
		aboutPref.setDialogTitle(getString(R.string.app_name) + version);
		aboutPref.setDialogLayoutResource(R.layout.about);

		// Set the version number in the email preference dialog
		EmailDialogPreference emailPref = (EmailDialogPreference) findPreference(getString(R.string.pref_sendemail_key));
		emailPref.setVersion(version);
	}

	@Override
	protected void onResume() {
		super.onResume();

		/* 
		 * This is quite hacky - in case the app was enabled or disabled externally (by
		 * ExternalEventReceiver) this will refresh the checkbox that is visible to the user
		 */
		AppEnabledCheckBoxPreference mEnabledPreference =
			(AppEnabledCheckBoxPreference) findPreference(getString(R.string.pref_enabled_key));

		SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean enabled = myPrefs.getBoolean(getString(R.string.pref_enabled_key), true); 
		mEnabledPreference.setChecked(enabled);
		
		// If enabled, send a broadcast to disable other SMS Popup apps
		if (enabled) {
			SMSPopupUtils.disableOtherSMSPopup(this);
		}
	}
}