package net.everythingandroid.smspopup;

import net.everythingandroid.smspopup.preferences.ButtonListPreference;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.preference.PreferenceManager;

public class ManagePreferences {
  private String contactId;
  private Context context;
  private Cursor contactCursor;
  private boolean useDatabase;
  private SharedPreferences mPrefs;
  private static final String one = "1";
  private SmsPopupDbAdapter mDbAdapter;

  /*
   * Define all default preferences in this static class.  Unfortunately these are also
   * stored in the resource xml files for use by the preference xml so they should be
   * updated in both places if a change is required.
   */
  public static final class Defaults {
    public static final boolean PREFS_AUTOROTATE = true;
    public static final boolean PREFS_PRIVACY = false;
    public static final boolean PREFS_PRIVACY_SENDER = false;
    public static final boolean PREFS_PRIVACY_ALWAYS = false;
    public static final boolean PREFS_SHOW_BUTTONS = true;
    public static final String PREFS_BUTTON1 = String.valueOf(ButtonListPreference.BUTTON_CLOSE);
    public static final String PREFS_BUTTON2 = String.valueOf(ButtonListPreference.BUTTON_DELETE);
    public static final String PREFS_BUTTON3 = String.valueOf(ButtonListPreference.BUTTON_REPLY);
    public static final boolean PREFS_SHOW_POPUP = true;
    public static final boolean PREFS_ONLY_SHOW_ON_KEYGUARD = false;
    public static final boolean PREFS_MARK_READ = true;

    public static final boolean PREFS_NOTIF_ENABLED = false;
    public static final String PREFS_NOTIF_ICON = "0";
    public static final boolean PREFS_NOTIFY_ON_CALL = false;
    public static final boolean PREFS_VIBRATE_ENABLED = true;
    public static final String PREFS_VIBRATE_PATTERN = "0,1200";
    public static final boolean PREFS_LED_ENABLED = true;
    public static final String PREFS_LED_PATTERN = "1000,1000";
    public static final String PREFS_LED_COLOR = "Yellow";
    public static final boolean PREFS_REPLY_TO_THREAD = true;
    public static final boolean PREFS_NOTIF_REPEAT = false;
    public static final String PREFS_NOTIF_REPEAT_INTERVAL = "5";
    public static final String PREFS_NOTIF_REPEAT_TIMES = "2";
    public static final Boolean PREFS_NOTIF_REPEAT_SCREEN_ON = false; 
    
  }

  public ManagePreferences(Context _context, String _contactId) {
    contactId = _contactId;
    context = _context;
    useDatabase = false;

    if (Log.DEBUG) Log.v("contactId = " + contactId);
    long contactIdLong;
    try {
      contactIdLong = Long.parseLong(contactId);
    } catch (NumberFormatException e) {
      contactIdLong = 0;
    }

    if (contactIdLong > 0) {
      mDbAdapter = new SmsPopupDbAdapter(context);
      try {
        mDbAdapter.open(true); // Open database read-only
        contactCursor = mDbAdapter.fetchContactSettings(contactIdLong);
        if (contactCursor != null) {
          if (Log.DEBUG) Log.v("Contact found - using database");
          useDatabase = true;
        }
        mDbAdapter.close();
      } catch (SQLException e) {
        if (Log.DEBUG) Log.v("Error opening or creating database");
        useDatabase = false;
      }
    } else {
      if (Log.DEBUG) Log.v("Contact NOT found - using prefs");
    }

    mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public boolean getBoolean(int resPrefId, int resDefaultId, int dbColumnNum) {
    if (useDatabase) {
      return one.equals(contactCursor.getString(dbColumnNum));
    } else {
      return getBoolean(resPrefId, resDefaultId);
    }
  }

  public boolean getBoolean(int resPrefId, boolean prefDefault, int dbColumnNum) {
    if (useDatabase) {
      return one.equals(contactCursor.getString(dbColumnNum));
    } else {
      return getBoolean(resPrefId, prefDefault);
    }
  }

  public boolean getBoolean(int resPrefId, int resDefaultId) {
    return mPrefs.getBoolean(context.getString(resPrefId),
        Boolean.parseBoolean(context.getString(resDefaultId)));
  }

  public boolean getBoolean(int resPrefId, boolean prefDefault) {
    return mPrefs.getBoolean(context.getString(resPrefId), prefDefault);
  }

  public String getString(int resPrefId, int resDefaultId, int dbColumnNum) {
    if (useDatabase) {
      return contactCursor.getString(dbColumnNum);
    } else {
      return getString(resPrefId, resDefaultId);
    }
  }

  public String getString(int resPrefId, String defaultVal, int dbColumnNum) {
    if (useDatabase) {
      return contactCursor.getString(dbColumnNum);
    } else {
      return mPrefs.getString(context.getString(resPrefId), defaultVal);
    }
  }

  public String getString(int resPrefId, int resDefaultId) {
    return mPrefs.getString(context.getString(resPrefId), context.getString(resDefaultId));
  }

  public String getString(int resPrefId, String defaultVal) {
    return mPrefs.getString(context.getString(resPrefId), defaultVal);
  }

  public void putString(int resPrefId, String newVal, String dbColumnNum) {
    if (useDatabase) {
      mDbAdapter.open(); // Open write
      mDbAdapter.updateContact(Long.valueOf(contactId), dbColumnNum, newVal);
      mDbAdapter.close();
    } else {
      SharedPreferences.Editor settings = mPrefs.edit();
      settings.putString(context.getString(resPrefId), newVal);
      settings.commit();
    }
  }

  public int getInt(String pref, int defaultVal) {
    return mPrefs.getInt(pref, defaultVal);
  }

  public int getInt(int resPrefId, int defaultVal) {
    return mPrefs.getInt(context.getString(resPrefId), defaultVal);
  }


  public void close() {
    if (contactCursor != null) {
      contactCursor.close();
    }
    if (mDbAdapter != null) {
      mDbAdapter.close();
    }
  }
}
