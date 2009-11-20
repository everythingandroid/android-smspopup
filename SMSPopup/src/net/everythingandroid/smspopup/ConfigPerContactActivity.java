package net.everythingandroid.smspopup;

import net.everythingandroid.smspopup.preferences.CustomLEDColorListPreference;
import net.everythingandroid.smspopup.preferences.CustomLEDPatternListPreference;
import net.everythingandroid.smspopup.preferences.CustomVibrateListPreference;
import net.everythingandroid.smspopup.preferences.TestNotificationDialogPreference;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.Menu;
import android.view.MenuItem;

public class ConfigPerContactActivity extends PreferenceActivity {
  private long contactId = 0;
  private String contactIdString = null;
  private SmsPopupDbAdapter mDbAdapter;
  private static final int MENU_SAVE_ID = Menu.FIRST;
  private static final int MENU_DELETE_ID = Menu.FIRST + 1;
  public static final String EXTRA_CONTACT_ID =
    "net.everythingandroid.smspopuppro.EXTRA_CONTACT_ID";

  private RingtonePreference ringtonePref;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (Log.DEBUG) Log.v("SMSPopupConfigPerContactActivity: onCreate()");

    /*
     * Create database object
     */
    mDbAdapter = new SmsPopupDbAdapter(getApplicationContext());
    mDbAdapter.open();

    /*
     * Create and setup preferences
     */
    createPreferences();
  }

  @Override
  protected void onResume() {
    super.onResume();

    SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    Uri ringtoneUri = Uri.parse(
        myPrefs.getString(
            getString(R.string.c_pref_notif_sound_key), ManageNotification.defaultRingtone));
    Log.v("Ringtone URI is: " + ringtoneUri.toString());
    Ringtone mRingtone = RingtoneManager.getRingtone(this, ringtoneUri);

    if (mRingtone == null) {
      ringtonePref.setSummary(getString(R.string.ringtone_silent));
    } else {
      ringtonePref.setSummary(mRingtone.getTitle(this));
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    mDbAdapter.close();
    super.onDestroy();
  }

  private void createPreferences() {
    /*
     * Ensure contactId was passed, if not, fire up an intent to choose a
     * contact (add)
     */
    contactId = getIntent().getLongExtra(EXTRA_CONTACT_ID, 0);

    if (contactId == 0) {
      if (Log.DEBUG) Log.v("Contact bad");
      finish();
    } else {
      if (Log.DEBUG) Log.v("contactId = " + contactId);
      contactIdString = String.valueOf(contactId);

      /*
       * Fetch the current user settings from the database
       */
      Cursor contact = mDbAdapter.fetchContactSettings(contactId);

      /*
       * If the contact is not yet in our db ...
       */
      if (contact == null) {
        createContact(contactId);
        contact = mDbAdapter.fetchContactSettings(contactId);
        if (contact == null) {
          if (Log.DEBUG) Log.v("Error creating or fetching contact");
          finish();
        }
      }

      startManagingCursor(contact);

      /*
       * Retrieve preferences from database
       */
      retrievePreferences(contact);

      /*
       * Add preference layout from XML
       */
      addPreferencesFromResource(R.xml.configcontact);

      /*
       * Customize Activity title + main notif enabled preference summaries
       */
      String contactName = contact.getString(SmsPopupDbAdapter.KEY_CONTACT_NAME_NUM);
      setTitle(getString(R.string.contact_customization_title, contactName));

      CheckBoxPreference enabledPref =
        (CheckBoxPreference) findPreference(getString(R.string.c_pref_notif_enabled_key));
      enabledPref.setSummaryOn(
          getString(R.string.contact_customization_enabled, contactName));
      enabledPref.setSummaryOff(
          getString(R.string.contact_customization_disabled, contactName));
      enabledPref.setOnPreferenceChangeListener(onPrefChangeListener);

      /*
       * Main Prefs
       */
      CheckBoxPreference enablePopupPref =
        (CheckBoxPreference) findPreference(getString(R.string.c_pref_popup_enabled_key));
      enablePopupPref.setOnPreferenceChangeListener(onPrefChangeListener);

      ringtonePref =
        (RingtonePreference) findPreference(getString(R.string.c_pref_notif_sound_key));
      ringtonePref.setOnPreferenceChangeListener(onPrefChangeListener);
      //      Uri ringtoneUri = Uri.parse(contact.getString(SmsPopupDbAdapter.KEY_RINGTONE_NUM));
      //      Ringtone mRingtone = RingtoneManager.getRingtone(this, ringtoneUri);
      //      ringtonePref.setSummary(mRingtone.getTitle(this));

      TestNotificationDialogPreference testPref =
        (TestNotificationDialogPreference) findPreference(getString(R.string.c_pref_notif_test_key));
      testPref.setContactId(contact.getLong(SmsPopupDbAdapter.KEY_CONTACT_ID_NUM));

      /*
       * Vibrate Prefs
       */
      CheckBoxPreference enableVibratePref =
        (CheckBoxPreference) findPreference(getString(R.string.c_pref_vibrate_key));
      enableVibratePref.setOnPreferenceChangeListener(onPrefChangeListener);

      CustomVibrateListPreference vibratePatternPref =
        (CustomVibrateListPreference) findPreference(getString(R.string.c_pref_vibrate_pattern_key));
      vibratePatternPref.setOnPreferenceChangeListener(onPrefChangeListener);
      vibratePatternPref.setContactId(contactIdString);

      /*
       * LED Prefs
       */
      CheckBoxPreference enableLEDPref =
        (CheckBoxPreference) findPreference(getString(R.string.c_pref_flashled_key));
      enableLEDPref.setOnPreferenceChangeListener(onPrefChangeListener);

      CustomLEDColorListPreference ledColorPref =
        (CustomLEDColorListPreference) findPreference(getString(R.string.c_pref_flashled_color_key));
      ledColorPref.setOnPreferenceChangeListener(onPrefChangeListener);
      ledColorPref.setContactId(contactIdString);

      CustomLEDPatternListPreference ledPatternPref =
        (CustomLEDPatternListPreference) findPreference(getString(R.string.c_pref_flashled_pattern_key));
      ledPatternPref.setOnPreferenceChangeListener(onPrefChangeListener);
      ledPatternPref.setContactId(contactIdString);

      /*
       * Close up database cursor
       */
      contact.close();
    }
  }

  /*
   * All preferences will trigger this when changed
   */
  private OnPreferenceChangeListener onPrefChangeListener = new OnPreferenceChangeListener() {
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      if (Log.DEBUG) Log.v("onPreferenceChange - " + newValue);
      return storePreferences(preference, newValue);
    }
  };

  /*
   * Store a single preference back to the database
   */
  private boolean storePreferences(Preference preference, Object newValue) {
    if (Log.DEBUG) Log.v("storePrefs()");
    String key = preference.getKey();
    String column = null;

    if (key.equals(getString(R.string.c_pref_notif_enabled_key))) {
      column = SmsPopupDbAdapter.KEY_ENABLED;
    } else if (key.equals(getString(R.string.c_pref_popup_enabled_key))) {
      column = SmsPopupDbAdapter.KEY_POPUP_ENABLED;
    } else if (key.equals(getString(R.string.c_pref_notif_sound_key))) {
      column = SmsPopupDbAdapter.KEY_RINGTONE;
    } else if (key.equals(getString(R.string.c_pref_vibrate_key))) {
      column = SmsPopupDbAdapter.KEY_VIBRATE_ENABLED;
    } else if (key.equals(getString(R.string.c_pref_vibrate_pattern_key))) {
      column = SmsPopupDbAdapter.KEY_VIBRATE_PATTERN;
    } else if (key.equals(getString(R.string.c_pref_vibrate_pattern_custom_key))) {
      column = SmsPopupDbAdapter.KEY_VIBRATE_PATTERN_CUSTOM;
    } else if (key.equals(getString(R.string.c_pref_flashled_key))) {
      column = SmsPopupDbAdapter.KEY_LED_ENABLED;
    } else if (key.equals(getString(R.string.c_pref_flashled_color_key))) {
      column = SmsPopupDbAdapter.KEY_LED_COLOR;
    } else if (key.equals(getString(R.string.c_pref_flashled_color_custom_key))) {
      column = SmsPopupDbAdapter.KEY_LED_COLOR_CUSTOM;
    } else if (key.equals(getString(R.string.c_pref_flashled_pattern_key))) {
      column = SmsPopupDbAdapter.KEY_LED_PATTERN;
    } else if (key.equals(getString(R.string.c_pref_flashled_pattern_custom_key))) {
      column = SmsPopupDbAdapter.KEY_LED_PATTERN_CUSTOM;
    } else {
      return false;
    }

    boolean success = mDbAdapter.updateContact(contactId, column, newValue);
    mDbAdapter.updateContactSummary(contactId);

    return success;
  }

  /*
   * Retrieve all preferences from the database into preferences
   */
  private void retrievePreferences(Cursor c) {
    String one = "1";
    if (Log.DEBUG) Log.v("retrievePrefs()");
    SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    SharedPreferences.Editor editor = myPrefs.edit();

    /*
     * Fetch Main Prefs
     */
    editor.putBoolean(getString(R.string.c_pref_notif_enabled_key),
        one.equals(c.getString(SmsPopupDbAdapter.KEY_ENABLED_NUM)));
    editor.putBoolean(getString(R.string.c_pref_popup_enabled_key),
        one.equals(c.getString(SmsPopupDbAdapter.KEY_POPUP_ENABLED_NUM)));
    editor.putString(getString(R.string.c_pref_notif_sound_key),
        c.getString(SmsPopupDbAdapter.KEY_RINGTONE_NUM));

    /*
     * Fetch Vibrate prefs
     */
    editor.putBoolean(getString(R.string.c_pref_vibrate_key),
        one.equals(c.getString(SmsPopupDbAdapter.KEY_VIBRATE_ENABLED_NUM)));
    editor.putString(getString(R.string.c_pref_vibrate_pattern_key),
        c.getString(SmsPopupDbAdapter.KEY_VIBRATE_PATTERN_NUM));
    editor.putString(getString(R.string.c_pref_vibrate_pattern_custom_key),
        c.getString(SmsPopupDbAdapter.KEY_VIBRATE_PATTERN_CUSTOM_NUM));

    /*
     * Fetch LED prefs
     */
    editor.putBoolean(getString(R.string.c_pref_flashled_key),
        one.equals(c.getString(SmsPopupDbAdapter.KEY_LED_ENABLED_NUM)));
    editor.putString(getString(R.string.c_pref_flashled_color_key),
        c.getString(SmsPopupDbAdapter.KEY_LED_COLOR_NUM));
    editor.putString(getString(R.string.c_pref_flashled_color_custom_key),
        c.getString(SmsPopupDbAdapter.KEY_LED_COLOR_CUSTOM_NUM));
    editor.putString(getString(R.string.c_pref_flashled_pattern_key),
        c.getString(SmsPopupDbAdapter.KEY_LED_PATTERN_NUM));
    editor.putString(getString(R.string.c_pref_flashled_pattern_custom_key),
        c.getString(SmsPopupDbAdapter.KEY_LED_PATTERN_CUSTOM_NUM));

    // Commit prefs
    editor.commit();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    MenuItem saveItem = menu.add(
        Menu.NONE, MENU_SAVE_ID, Menu.NONE, R.string.contact_customization_save);
    MenuItem deleteItem = menu.add(
        Menu.NONE, MENU_DELETE_ID, Menu.NONE, R.string.contact_customization_remove);

    saveItem.setIcon(android.R.drawable.ic_menu_save);
    deleteItem.setIcon(android.R.drawable.ic_menu_delete);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case MENU_SAVE_ID:
        finish();
        return true;
      case MENU_DELETE_ID:
        mDbAdapter.deleteContact(contactId);
        finish();
        return true;
    }
    return false;
  }

  private void createContact(long contactId) {
    getIntent().putExtra(EXTRA_CONTACT_ID, contactId);
    mDbAdapter.createContact(contactId);
    mDbAdapter.updateContactSummary(contactId);
  }

}
