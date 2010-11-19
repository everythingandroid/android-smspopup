package net.everythingandroid.smspopup;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.widget.Toast;

public class SmsPopupDbAdapter {

  private static final String CONTACTS_DB_TABLE = "contacts";

  public static final String KEY_CONTACT_ID              = "_id";
  public static final int KEY_CONTACT_ID_NUM             = 0;
  public static final String KEY_CONTACT_NAME            = "displayname";
  public static final int KEY_CONTACT_NAME_NUM           = 1;
  public static final String KEY_ENABLED                 = "enabled";
  public static final int KEY_ENABLED_NUM                = 2;
  public static final String KEY_POPUP_ENABLED           = "popup_enabled";
  public static final int KEY_POPUP_ENABLED_NUM          = 3;
  public static final String KEY_RINGTONE                = "ringtone";
  public static final int KEY_RINGTONE_NUM               = 4;
  public static final String KEY_VIBRATE_ENABLED         = "vibrate_enabled";
  public static final int KEY_VIBRATE_ENABLED_NUM        = 5;
  public static final String KEY_VIBRATE_PATTERN         = "vibrate_pattern";
  public static final int KEY_VIBRATE_PATTERN_NUM        = 6;
  public static final String KEY_VIBRATE_PATTERN_CUSTOM  = "vibrate_pattern_custom";
  public static final int KEY_VIBRATE_PATTERN_CUSTOM_NUM = 7;
  public static final String KEY_LED_ENABLED             = "led_enabled";
  public static final int KEY_LED_ENABLED_NUM            = 8;
  public static final String KEY_LED_PATTERN             = "led_pattern";
  public static final int KEY_LED_PATTERN_NUM            = 9;
  public static final String KEY_LED_PATTERN_CUSTOM      = "led_pattern_custom";
  public static final int KEY_LED_PATTERN_CUSTOM_NUM     = 10;
  public static final String KEY_LED_COLOR               = "led_color";
  public static final int KEY_LED_COLOR_NUM              = 11;
  public static final String KEY_LED_COLOR_CUSTOM        = "led_color_custom";
  public static final int   KEY_LED_COLOR_CUSTOM_NUM     = 12;
  public static final String KEY_SUMMARY                 = "summary";
  public static final int   KEY_SUMMARY_NUM              = 13;

  private static final String QUICKMESSAGES_DB_TABLE = "quickmessages";

  public static final String KEY_ROWID         = "_id";
  public static final int KEY_ROWID_NUM        = 0;
  public static final String KEY_QUICKMESSAGE  = "quickmessage";
  public static final int KEY_QUICKMESSAGE_NUM = 1;
  public static final String KEY_ORDER         = "ordering";
  public static final int KEY_ORDER_NUM        = 2;

  private static final int QUICKMESSAGES_ORDERING_DEFAULT = 100;
  // private static final String QUICKMESSAGES_SORT_BY = KEY_ROWID;
  private static final String QUICKMESSAGES_SORT_BY = KEY_ORDER + "," + KEY_ROWID;
  private static final String QUICKMESSAGES_UPDATE_ALL_ORDERING = "update quickmessages set ordering=ordering+" + QUICKMESSAGES_ORDERING_DEFAULT;
  private static final String QUICKMESSAGES_GET_MAX_ORDERING = "select max(" + KEY_ORDER + ") from " + QUICKMESSAGES_DB_TABLE;
  private static final String QUICKMESSAGES_GET_MIN_ORDERING = "select min(" + KEY_ORDER + ") from " + QUICKMESSAGES_DB_TABLE;

  private static final String DATABASE_NAME = "data";
  private static final int DATABASE_VERSION = 1;

  private DatabaseHelper mDbHelper;
  private SQLiteDatabase mDb;

  private static final String one = "1";

  /**
   * Table creation sql statement
   */
  private static final String CONTACTS_DB_CREATE =
    "create table " + CONTACTS_DB_TABLE + " (" +
    KEY_CONTACT_ID             + " integer primary key, " +
    KEY_CONTACT_NAME           + " text default 'Unknown', " +
    KEY_ENABLED                + " integer default 1, " +
    KEY_POPUP_ENABLED          + " integer default 1, " +
    KEY_RINGTONE               + " text default '" +
    //+ Settings.System.DEFAULT_NOTIFICATION_URI.toString() + "', " +
    "content://settings/system/notification_sound" + "', " +
    KEY_VIBRATE_ENABLED        + " integer default 1, " +
    KEY_VIBRATE_PATTERN        + " text default '0,1200', " +
    KEY_VIBRATE_PATTERN_CUSTOM + " text null, " +
    KEY_LED_ENABLED            + " integer default 1, " +
    KEY_LED_PATTERN            + " text default '1000,1000', " +
    KEY_LED_PATTERN_CUSTOM     + " text null, " +
    KEY_LED_COLOR              + " text default 'Yellow', " +
    KEY_LED_COLOR_CUSTOM       + " text null, " +
    KEY_SUMMARY                + " text default 'Default notifications' " +
    ");";

  /**
   * Table creation sql statement
   */
  private static final String QUICKMESSAGES_DB_CREATE =
    "create table "  + QUICKMESSAGES_DB_TABLE + " (" +
    KEY_ROWID        + " integer primary key autoincrement, " +
    KEY_QUICKMESSAGE + " text, " +
    KEY_ORDER        + " integer default " + QUICKMESSAGES_ORDERING_DEFAULT +
    ");";

  private final Context context;

  private static class DatabaseHelper extends SQLiteOpenHelper {

    DatabaseHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      if (Log.DEBUG) Log.v("SMSPopupDbAdapter: Creating Database");
      db.execSQL(CONTACTS_DB_CREATE);
      db.execSQL(QUICKMESSAGES_DB_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      //           Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
      //                   + newVersion + ", which will destroy all old data");
      if (Log.DEBUG) Log.v("SMSPopupDbAdapter: Upgrading Database");
      db.execSQL("DROP TABLE IF EXISTS " + CONTACTS_DB_TABLE);
      db.execSQL("DROP TABLE IF EXISTS " + QUICKMESSAGES_DB_TABLE);
      onCreate(db);
    }
  }

  /**
   * Constructor - takes the context to allow the database to be
   * opened/created
   *
   * @param _context the Context within which to work
   */
  public SmsPopupDbAdapter(Context _context) {
    this.context = _context;
    mDbHelper = null;
    mDb = null;
  }

  /**
   * Open the contacts database. If it cannot be opened, try to create a new
   * instance of the database. If it cannot be created, throw an exception to
   * signal the failure
   *
   * @return this (self reference, allowing this to be chained in an
   *         initialization call)
   * @throws SQLException if the database could be neither opened or created
   */
  public SmsPopupDbAdapter open() throws SQLException {
    return open(false);
  }

  /**
   * Open the contacts database. If it cannot be opened, try to create a new
   * instance of the database. If it cannot be created, throw an exception to
   * signal the failure
   *
   * @param readOnly if the database should be opened read only
   * @return this (self reference, allowing this to be chained in an
   *         initialization call)
   * @throws SQLException if the database could be neither opened or created
   */
  public SmsPopupDbAdapter open(boolean readOnly) throws SQLException {
    if (mDbHelper == null) {
      if (Log.DEBUG) Log.v("Opened database");
      mDbHelper = new DatabaseHelper(context);
      if (readOnly) {
        mDb = mDbHelper.getReadableDatabase();
      } else {
        mDb = mDbHelper.getWritableDatabase();
      }
    }
    return this;
  }

  /**
   * Close the database
   */
  public void close() {
    if (mDbHelper != null) {
      if (Log.DEBUG) Log.v("Closed database");
      mDbHelper.close();
      mDbHelper = null;
    }
  }

  public long createContact(long contactId) {
    Cursor c = fetchContact(contactId);

    if (c == null) {
      if (Log.DEBUG) Log.v("SMSPopupDbAdapter: creating contact");

      String contactName = SmsPopupUtils.getPersonName(
          context, String.valueOf(contactId), null);
      if (contactName == null) return 0;

      ContentValues vals = new ContentValues();
      vals.put(KEY_CONTACT_ID, contactId);
      vals.put(KEY_CONTACT_NAME, contactName.trim());
      return mDb.insert(CONTACTS_DB_TABLE, null, vals);
    }
    c.close();
    if (Log.DEBUG) Log.v("SMSPopupDbAdapter: contact exists");
    return 0;
  }

  public boolean deleteContact(long contactId) {
    return deleteContact(contactId, true);
  }

  public boolean deleteContact(long contactId, boolean showToast) {
    if (mDb.delete(CONTACTS_DB_TABLE, KEY_CONTACT_ID + "=" + String.valueOf(contactId), null) > 0) {

      if (showToast) {
        Toast.makeText(
            context, R.string.contact_customization_removed_toast, Toast.LENGTH_SHORT).show();
      }
      return true;
    }
    return false;
  }

  public Cursor fetchContact(long contactId) throws SQLException {
    boolean found = false;
    Cursor mCursor =
      mDb.query(true, CONTACTS_DB_TABLE,
          new String[] { KEY_CONTACT_ID, KEY_CONTACT_NAME, KEY_RINGTONE },
          KEY_CONTACT_ID + "=" + contactId, null,
          null, null, null, null);
    if (mCursor != null) {
      found = mCursor.moveToFirst();
    }
    if (!found) {
      if (mCursor != null) {
        mCursor.close();
      }
      return null;
    }
    return mCursor;
  }

  /**
   * Fetch all settings (basically all columns) for a contact
   *
   * @param contactId
   * @return
   * @throws SQLException
   */
  public Cursor fetchContactSettings(long contactId) throws SQLException {
    boolean found = false;
    Cursor mCursor =
      mDb.query(true, CONTACTS_DB_TABLE,
          null, // Return all columns
          KEY_CONTACT_ID + "=" + contactId, null,
          null, null, null, null);
    if (mCursor != null) {
      found = mCursor.moveToFirst();
    }
    if (!found) {
      if (mCursor != null) {
        mCursor.close();
      }
      return null;
    }
    if (Log.DEBUG) Log.v("fetchContactSettings - found contact in db, " + contactId);
    return mCursor;
  }

  /**
   * Fetch a list of all contacts in the database
   * @return Db cursor
   */
  public Cursor fetchAllContacts() {
    return mDb.query(CONTACTS_DB_TABLE,
        new String[] {KEY_CONTACT_ID, KEY_CONTACT_NAME, KEY_RINGTONE, KEY_SUMMARY},
        null, null, null, null, KEY_CONTACT_NAME);
  }

  /**
   * Update a specific database column for a contact
   *
   * @param contactId
   * @param columnName
   * @param data
   * @return true if success, false otherwise
   */
  public boolean updateContact(long contactId, String columnName, Object data) {
    ContentValues vals = new ContentValues();

    if (data.getClass().equals(Boolean.class)) {
      if (Log.DEBUG) Log.v("boolean! " + data);
      vals.put(columnName, ((Boolean) data));
    } else if (data.getClass().equals(String.class)) {
      if (Log.DEBUG) Log.v("string! " + data);
      vals.put(columnName, (String) data);
    } else {
      return false;
    }

    if (Log.DEBUG) Log.v("updateContact - " + columnName + ", " + data.getClass());

    return mDb.update(CONTACTS_DB_TABLE, vals, KEY_CONTACT_ID + "=" + contactId, null) > 0;
  }

  /*
   * This updates a summary column in the database that contains a brief summary of the
   * notification settings for the contact.
   */
  public boolean updateContactSummary(long contactId) {
    // TODO: this needs to be tidied up and converted to resource
    // strings so it can handle other languages
    Cursor contact = fetchContactSettings(contactId);
    if (contact == null) {
      return false;
    }

    StringBuilder summary = new StringBuilder("Popup ");

    if (one.equals(contact.getString(KEY_POPUP_ENABLED_NUM))) {
      summary.append("enabled");
    } else {
      summary.append("disabled");
    }

    summary.append(", Notifications ");
    if (!one.equals(contact.getString(KEY_ENABLED_NUM))) {
      summary.append("disabled");
    } else {
      summary.append("enabled");
      if (one.equals(contact.getString(KEY_VIBRATE_ENABLED_NUM))) {
        summary.append(", Vibrate on");
      }
      if (one.equals(contact.getString(KEY_LED_ENABLED_NUM))) {
        String ledColor = contact.getString(KEY_LED_COLOR_NUM);
        Log.v("ledColor = " + ledColor);
        if ("custom".equals(ledColor)) {
          ledColor = "Custom";
        }
        summary.append(", " + ledColor + " LED");
      }
    }
    contact.close();
    if (Log.DEBUG) Log.v("updateContactSummary()");
    return updateContact(contactId, KEY_SUMMARY, summary.toString());
  }

  /**
   * Creates a new quick message in the database
   */
  public long createQuickMessage(String message) {
    if (message == null) return -1;

    String trimmedMessage = message.trim();
    if (TextUtils.isEmpty(trimmedMessage)) return -1;

    if (Log.DEBUG) Log.v("QuickMessagesDbAdapter: creating row");

    ContentValues vals = new ContentValues();
    vals.put(KEY_QUICKMESSAGE, trimmedMessage);

    int maxOrderNum = getMaxQuickMessageOrdering();
    if (maxOrderNum > 0) {
      vals.put(KEY_ORDER, maxOrderNum + 1);
    }
    return mDb.insert(QUICKMESSAGES_DB_TABLE, null, vals);
  }

  /**
   * Deletes a quick message from the database
   *
   * @param id
   *          the database id of the message to delete
   * @return true if success, false otherwise
   */
  public boolean deleteQuickMessage(long id) {
    if (Log.DEBUG) Log.v("id to delete is " + id);
    if (mDb.delete(QUICKMESSAGES_DB_TABLE, KEY_ROWID + "=" + String.valueOf(id), null) > 0) {
      return true;
    }
    return false;
  }

  /**
   * Fetches a single quick message from the database
   *
   * @param id
   *          the database id of the message to fetch
   * @return a cursor to the data if success, null otherwise
   * @throws SQLException
   */
  public Cursor fetchQuickMessage(long id) throws SQLException {
    boolean found = false;

    Cursor mCursor =
      mDb.query(true, QUICKMESSAGES_DB_TABLE,
          new String[] {KEY_ROWID, KEY_QUICKMESSAGE, KEY_ORDER},
          KEY_ROWID + "=" + id, null,
          null, null, null, null);
    if (mCursor != null) {
      found = mCursor.moveToFirst();
    }
    if (!found) {
      if (mCursor != null) {
        mCursor.close();
      }
      return null;
    }
    return mCursor;
  }

  /**
   * Fetch all quick messages from the database
   *
   * @return a cursor to the data
   */
  public Cursor fetchAllQuickMessages() {
    return mDb.query(QUICKMESSAGES_DB_TABLE,
        new String[] {KEY_ROWID, KEY_QUICKMESSAGE, KEY_ORDER},
        null, null, null, null, QUICKMESSAGES_SORT_BY);
  }

  /**
   * Updates a quick message in the database
   *
   * @param id
   *          the database id of the message to update
   * @param message
   *          the new message to save
   * @return true if success, false otherwise
   */
  public boolean updateQuickMessage(long id, String message) {
    ContentValues vals = new ContentValues();
    vals.put(KEY_QUICKMESSAGE, message);
    return mDb.update(QUICKMESSAGES_DB_TABLE, vals, KEY_ROWID + "=" + id, null) > 0;
  }

  /**
   * Reorders a quick message so it has the lowest value for the 'ordering'
   * column (moving it to the top in the list)
   *
   * @param id
   *          the database id of the message to update
   * @return true if success, false otherwise
   */
  public boolean reorderQuickMessage(long id) {
    int minOrderNum = getMinQuickMessageOrdering();
    if (minOrderNum == -1 || minOrderNum == 0) return false;

    if (minOrderNum == 1) {
      mDb.execSQL(QUICKMESSAGES_UPDATE_ALL_ORDERING);
      minOrderNum = QUICKMESSAGES_ORDERING_DEFAULT;
    } else {
      minOrderNum -= 1;
    }
    ContentValues vals = new ContentValues();
    vals.put(KEY_ORDER, minOrderNum);
    return mDb.update(QUICKMESSAGES_DB_TABLE, vals, KEY_ROWID + "=" + id, null) > 0;
  }

  /*
   * Gets the minimum ordering value in the quick message table (or -1 if there's an error)
   */
  private int getMinQuickMessageOrdering() {
    Cursor c = mDb.rawQuery(QUICKMESSAGES_GET_MIN_ORDERING, null);
    if (c == null) return -1;
    if (c.moveToFirst()) {
      int result = c.getInt(0);
      c.close();
      if (Log.DEBUG) Log.v("Min ordering = " + result);
      return result;
    }
    return -1;
  }

  /*
   * Gets the maximum ordering value in the quick message table (or -1 if there's an error)
   */
  private int getMaxQuickMessageOrdering() {
    Cursor c = mDb.rawQuery(QUICKMESSAGES_GET_MAX_ORDERING, null);
    if (c == null) return -1;
    if (c.moveToFirst()) {
      if (c.isNull(0)) { // If no results
        return QUICKMESSAGES_ORDERING_DEFAULT;
      }
      int result = c.getInt(0);
      c.close();
      if (Log.DEBUG) Log.v("Max ordering = " + result);
      return result;
    }
    return -1;
  }
}