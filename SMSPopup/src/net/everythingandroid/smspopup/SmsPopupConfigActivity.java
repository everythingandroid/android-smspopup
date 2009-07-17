package net.everythingandroid.smspopup;

import net.everythingandroid.smspopup.preferences.AppEnabledCheckBoxPreference;
import net.everythingandroid.smspopup.preferences.DialogPreference;
import net.everythingandroid.smspopup.preferences.EmailDialogPreference;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class SmsPopupConfigActivity extends PreferenceActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);

    //Try and find app version number
    String version;
    PackageManager pm = this.getPackageManager();
    try {
      //Get version number, not sure if there is a better way to do this
      version = " v" +
      pm.getPackageInfo(
          SmsPopupConfigActivity.class.getPackage().getName(), 0).versionName;
    } catch (NameNotFoundException e) {
      version = "";
    }

    // Set the version number in the about dialog preference
    DialogPreference aboutPref =
      (DialogPreference) findPreference(getString(R.string.pref_about_key));
    aboutPref.setDialogTitle(getString(R.string.app_name) + version);
    aboutPref.setDialogLayoutResource(R.layout.about);

    // Set the version number in the email preference dialog
    EmailDialogPreference emailPref = (EmailDialogPreference) findPreference(getString(R.string.pref_sendemail_key));
    emailPref.setVersion(version);

    // Set intent for contact notification option
    PreferenceScreen contactsPS =
      (PreferenceScreen) findPreference(getString(R.string.contacts_key));
    contactsPS.setIntent(
        new Intent(this, net.everythingandroid.smspopup.ConfigContactsActivity.class));

    // Set intent for quick message option
    PreferenceScreen quickMessagePS =
      (PreferenceScreen) findPreference(getString(R.string.quickmessages_key));
    quickMessagePS.setIntent(
        new Intent(this, net.everythingandroid.smspopup.ConfigQuickMessagesActivity.class));

    // Opening and closing the database will trigger the update or create
    // TODO: this should be done on a separate thread to prevent "not responding" messages
    SmsPopupDbAdapter mDbAdapter = new SmsPopupDbAdapter(this);
    mDbAdapter.open(true); // Open database read-only
    mDbAdapter.close();

    SmsMonitorService.beginStartingService(this);
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
      SmsPopupUtils.disableOtherSMSPopup(this);
    }
  }
}