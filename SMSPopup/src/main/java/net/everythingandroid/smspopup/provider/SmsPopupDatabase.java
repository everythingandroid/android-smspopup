package net.everythingandroid.smspopup.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.Settings;

import net.everythingandroid.smspopup.BuildConfig;
import net.everythingandroid.smspopup.provider.SmsPopupContract.ContactNotifications;
import net.everythingandroid.smspopup.provider.SmsPopupContract.Messages;
import net.everythingandroid.smspopup.provider.SmsPopupContract.QuickMessages;
import net.everythingandroid.smspopup.service.SmsPopupUtilsService;
import net.everythingandroid.smspopup.util.Log;

public class SmsPopupDatabase extends SQLiteOpenHelper {
    public static final String CONTACTS_DB_TABLE = "contacts";
    public static final String QUICKMESSAGES_DB_TABLE = "quickmessages";
    public static final String MESSAGES_DB_TABLE = "messages";
    public static final int QUICKMESSAGE_ORDER_DEFAULT = 100;

    private static final String DATABASE_NAME = "data";
    private static final int DATABASE_VERSION = 4;

    public static final String QUICKMESSAGES_UPDATE_ORDER_SQL = "update "
            + QUICKMESSAGES_DB_TABLE + " set " + QuickMessages.ORDER + "="
            + QuickMessages.ORDER + "+" + QUICKMESSAGE_ORDER_DEFAULT;

    // Table creation sql statement
    private static final String CONTACTS_DB_CREATE =
        "create table " + CONTACTS_DB_TABLE + " (" +
        ContactNotifications._ID                    + " integer primary key autoincrement, " +
        ContactNotifications.CONTACT_ID             + " integer, " +
        ContactNotifications.CONTACT_LOOKUPKEY      + " text, " +
        ContactNotifications.CONTACT_NAME           + " text default 'Unknown', " +
        ContactNotifications.ENABLED                + " integer default 1, " +
        ContactNotifications.POPUP_ENABLED          + " integer default 1, " +
        ContactNotifications.RINGTONE               + " text default '" +
        Settings.System.DEFAULT_NOTIFICATION_URI.toString() + "', " +
        //"content://settings/system/notification_sound" + "', " +
        ContactNotifications.VIBRATE_ENABLED        + " integer default 1, " +
        ContactNotifications.VIBRATE_PATTERN        + " text default '0,1200', " +
        ContactNotifications.VIBRATE_PATTERN_CUSTOM + " text null, " +
        ContactNotifications.LED_ENABLED            + " integer default 1, " +
        ContactNotifications.LED_PATTERN            + " text default '1000,1000', " +
        ContactNotifications.LED_PATTERN_CUSTOM     + " text null, " +
        ContactNotifications.LED_COLOR              + " text default 'Yellow', " +
        ContactNotifications.LED_COLOR_CUSTOM       + " text null, " +
        ContactNotifications.SUMMARY                + " text default 'Default notifications', " +
        "UNIQUE (" + ContactNotifications.CONTACT_LOOKUPKEY + ") ON CONFLICT IGNORE" +
        ");";

    private static final String CONTACTS_DB_INDEX_CREATE =
            "create index lookup_idx ON " + CONTACTS_DB_TABLE +
            "(" + ContactNotifications.CONTACT_LOOKUPKEY + ");";

    private static final String CONTACTS_DB_INDEX2_CREATE =
            "create index lookup_idx2 ON " + CONTACTS_DB_TABLE +
            "(" + ContactNotifications.CONTACT_ID + ");";

    // Table creation sql statement
    private static final String QUICKMESSAGES_DB_CREATE =
        "create table "  + QUICKMESSAGES_DB_TABLE + " (" +
        QuickMessages._ID          + " integer primary key autoincrement, " +
        QuickMessages.QUICKMESSAGE + " text, " +
        QuickMessages.ORDER        + " integer default " + QUICKMESSAGE_ORDER_DEFAULT +
        ");";

    // Table creation sql statement
    private static final String MESSAGES_DB_CREATE =
            "create table "  + MESSAGES_DB_TABLE + " (" +
            Messages._ID        + " integer primary key, " +
            Messages.READ       + " integer default 0, " +
            Messages.TIMESTAMP  + " integer, " +
            Messages.ADDED      + " integer not null " +
            ");";

    private Context mContext;

    public SmsPopupDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (BuildConfig.DEBUG) Log.v("SmsPopupDatabase: Creating Database");
        db.execSQL(CONTACTS_DB_CREATE);
        db.execSQL(CONTACTS_DB_INDEX_CREATE);
        db.execSQL(CONTACTS_DB_INDEX2_CREATE);
        db.execSQL(QUICKMESSAGES_DB_CREATE);
        db.execSQL(MESSAGES_DB_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (BuildConfig.DEBUG) Log.v("SmsPopupDatabase: Upgrading Database");
        if (oldVersion == 2 && newVersion == 3) {
            // From v2->v3 a new column was added to the contacts table plus a new index
            db.execSQL("ALTER TABLE " + CONTACTS_DB_TABLE + " ADD COLUMN " +
                    ContactNotifications.CONTACT_ID  + " integer");
            db.execSQL(CONTACTS_DB_INDEX2_CREATE);
            SmsPopupUtilsService.startSyncContactNames(mContext);
        } else if (oldVersion == 3 && newVersion == 4) {
            // From v3->v4 the new messages table was added
            db.execSQL(MESSAGES_DB_CREATE);
        } else {
            // All other scenarios just drop all tables and recreate
            db.execSQL("DROP TABLE IF EXISTS " + CONTACTS_DB_TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + QUICKMESSAGES_DB_TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + MESSAGES_DB_CREATE);
            onCreate(db);
        }
    }
}