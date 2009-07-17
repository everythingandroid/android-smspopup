package net.everythingandroid.smspopup;

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
  private SharedPreferences myPrefs;
  private static final String one = "1";
  private SmsPopupDbAdapter mDbAdapter;

  public ManagePreferences(Context _context, String _contactId) {
    contactId = _contactId;
    context = _context;
    useDatabase = false;

    Log.v("contactId = " + contactId);
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
          Log.v("Contact found - using database");
          useDatabase = true;
        }
        mDbAdapter.close();
      } catch (SQLException e) {
        Log.v("Error opening or creating database");
        useDatabase = false;
      }
    } else {
      Log.v("Contact NOT found - using prefs");
    }

    myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public boolean getBoolean(int resPrefId, int resDefaultId, int dbColumnNum) {
    if (useDatabase) {
      return one.equals(contactCursor.getString(dbColumnNum));
    } else {
      return myPrefs.getBoolean(context.getString(resPrefId), Boolean.parseBoolean(context
          .getString(resDefaultId)));
    }
  }

  public boolean getBoolean(int resPrefId, int resDefaultId) {
    return myPrefs.getBoolean(context.getString(resPrefId), Boolean.parseBoolean(context
        .getString(resDefaultId)));
  }

  public String getString(int resPrefId, int resDefaultId, int dbColumnNum) {
    if (useDatabase) {
      return contactCursor.getString(dbColumnNum);
    } else {
      return myPrefs.getString(context.getString(resPrefId), context.getString(resDefaultId));
    }
  }

  public String getString(int resPrefId, int resDefaultId) {
    return myPrefs.getString(context.getString(resPrefId), context.getString(resDefaultId));
  }

  public String getString(int resPrefId, String defaultVal, int dbColumnNum) {
    if (useDatabase) {
      return contactCursor.getString(dbColumnNum);
    } else {
      return myPrefs.getString(context.getString(resPrefId), defaultVal);
    }
  }

  public void putString(int resPrefId, String newVal, String dbColumnNum) {
    if (useDatabase) {
      mDbAdapter.open(); // Open write
      mDbAdapter.updateContact(Long.valueOf(contactId), dbColumnNum, newVal);
      mDbAdapter.close();
    } else {
      SharedPreferences.Editor settings = myPrefs.edit();
      settings.putString(context.getString(resPrefId), newVal);
      settings.commit();
    }
  }

  @Override
  protected void finalize() throws Throwable {
    close();
    super.finalize();
  }

  public void close() {
    if (contactCursor != null) {
      contactCursor.close();
    }
  }
}
