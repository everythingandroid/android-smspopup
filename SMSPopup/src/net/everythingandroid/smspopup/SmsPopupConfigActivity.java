package net.everythingandroid.smspopup;

import net.everythingandroid.smspopup.preferences.AppEnabledCheckBoxPreference;
import net.everythingandroid.smspopup.preferences.ButtonListPreference;
import net.everythingandroid.smspopup.preferences.DialogPreference;
import net.everythingandroid.smspopup.preferences.EmailDialogPreference;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SmsPopupConfigActivity extends PreferenceActivity {
  private static final int DIALOG_DONATE = Menu.FIRST;
  Preference donateDialogPref = null;

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
        new Intent(this, net.everythingandroid.smspopup.ConfigPresetMessagesActivity.class));

    // Button 1 preference
    ButtonListPreference button1 =
      (ButtonListPreference) findPreference(getString(R.string.pref_button1_key));
    button1.prefId = getString(R.string.pref_button1_key);
    button1.refreshSummary();

    // Button 2 preference
    ButtonListPreference button2 =
      (ButtonListPreference) findPreference(getString(R.string.pref_button2_key));
    button2.prefId = getString(R.string.pref_button2_key);
    button2.refreshSummary();

    // Button 3 preference
    ButtonListPreference button3 =
      (ButtonListPreference) findPreference(getString(R.string.pref_button3_key));
    button3.prefId = getString(R.string.pref_button3_key);
    button3.refreshSummary();

    // Donate dialog preference
    donateDialogPref = findPreference(getString(R.string.pref_donate_key));
    if (donateDialogPref != null) {
      donateDialogPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
          SmsPopupConfigActivity.this.showDialog(DIALOG_DONATE);
          return true;
        }
      });
    }

    // Opening and closing the database will trigger the update or create
    // TODO: this should be done on a separate thread to prevent "not responding" messages
    SmsPopupDbAdapter mDbAdapter = new SmsPopupDbAdapter(this);
    mDbAdapter.open(true); // Open database read-only
    mDbAdapter.close();

    // Testing large photo insertion
    //    Bitmap photo = BitmapFactory.decodeResource(getResources(), R.drawable.img);
    //
    //    Uri uri = Uri.withAppendedPath(Contacts.People.CONTENT_URI, "4");
    //    if (photo != null) {
    //      ByteArrayOutputStream stream = new ByteArrayOutputStream();
    //      photo.compress(Bitmap.CompressFormat.JPEG, 75, stream);
    //      People.setPhotoData(getContentResolver(), uri, stream.toByteArray());
    //    }

    //    Intent i = new Intent(Intent.ACTION_VIEW);
    //    i.setData(Uri.parse("content://contacts/people/1256"));
    //    startActivity(i);
  }

  @Override
  protected void onResume() {
    super.onResume();

    SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(this);

    //    SharedPreferences.Editor settings = myPrefs.edit();
    //    settings.putBoolean(this.getString(R.string.pref_donated_key), false);
    //    settings.commit();

    // Donate Dialog
    if (donateDialogPref != null) {
      boolean donated = myPrefs.getBoolean(this.getString(R.string.pref_donated_key), false);
      if (donated) {
        PreferenceCategory otherPrefCategory =
          (PreferenceCategory) findPreference(getString(R.string.pref_other_key));
        otherPrefCategory.removePreference(donateDialogPref);
        donateDialogPref = null;
      }
    }

    /*
     * This is quite hacky - in case the app was enabled or disabled externally (by
     * ExternalEventReceiver) this will refresh the checkbox that is visible to the user
     */
    AppEnabledCheckBoxPreference mEnabledPreference =
      (AppEnabledCheckBoxPreference) findPreference(getString(R.string.pref_enabled_key));

    boolean enabled = myPrefs.getBoolean(getString(R.string.pref_enabled_key), true);
    mEnabledPreference.setChecked(enabled);

    // If enabled, send a broadcast to disable other SMS Popup apps
    if (enabled) {
      SmsPopupUtils.disableOtherSMSPopup(this);
    }
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {

      case DIALOG_DONATE:
        LayoutInflater factory = LayoutInflater.from(this);
        final View donateView = factory.inflate(R.layout.donate, null);

        Button donateMarketButton = (Button) donateView.findViewById(R.id.DonateMarketButton);
        donateMarketButton.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(SmsPopupUtils.DONATE_MARKET_URI);
            SmsPopupConfigActivity.this.startActivity(
                Intent.createChooser(i, getString(R.string.pref_donate_title)));
          }
        });

        Button donatePaypalButton = (Button) donateView.findViewById(R.id.DonatePaypalButton);
        donatePaypalButton.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(SmsPopupUtils.DONATE_PAYPAL_URI);
            SmsPopupConfigActivity.this.startActivity(i);
          }
        });

        return new AlertDialog.Builder(this)
        .setIcon(R.drawable.smspopup_icon)
        .setTitle(R.string.pref_donate_title)
        .setView(donateView)
        .setPositiveButton(android.R.string.ok, null)
        .create();
    }
    return super.onCreateDialog(id);
  }

}