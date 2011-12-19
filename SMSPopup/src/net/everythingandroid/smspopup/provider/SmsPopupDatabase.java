package net.everythingandroid.smspopup.provider;

import net.everythingandroid.smspopup.Log;
import net.everythingandroid.smspopup.provider.SmsPopupContract.ContactNotifications;
import net.everythingandroid.smspopup.provider.SmsPopupContract.QuickMessages;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class SmsPopupDatabase extends SQLiteOpenHelper {
    public static final String CONTACTS_DB_TABLE = "contacts";
    public static final String QUICKMESSAGES_DB_TABLE = "quickmessages";

    private static final int QUICKMESSAGES_ORDERING_DEFAULT = 100;
    private static final String DATABASE_NAME = "data";
    private static final int DATABASE_VERSION = 2;

    // Table creation sql statement
    private static final String CONTACTS_DB_CREATE =
        "create table " + CONTACTS_DB_TABLE + " (" +
        ContactNotifications._ID                    + " integer primary key, " +
        ContactNotifications.CONTACT_NAME           + " text default 'Unknown', " +
        ContactNotifications.ENABLED                + " integer default 1, " +
        ContactNotifications.POPUP_ENABLED          + " integer default 1, " +
        ContactNotifications.RINGTONE               + " text default '" +
        //+ Settings.System.DEFAULT_NOTIFICATION_URI.toString() + "', " +
        "content://settings/system/notification_sound" + "', " +
        ContactNotifications.VIBRATE_ENABLED        + " integer default 1, " +
        ContactNotifications.VIBRATE_PATTERN        + " text default '0,1200', " +
        ContactNotifications.VIBRATE_PATTERN_CUSTOM + " text null, " +
        ContactNotifications.LED_ENABLED            + " integer default 1, " +
        ContactNotifications.LED_PATTERN            + " text default '1000,1000', " +
        ContactNotifications.LED_PATTERN_CUSTOM     + " text null, " +
        ContactNotifications.LED_COLOR              + " text default 'Yellow', " +
        ContactNotifications.LED_COLOR_CUSTOM       + " text null, " +
        ContactNotifications.SUMMARY                + " text default 'Default notifications' " +
        ");";

    // Table creation sql statement
    private static final String QUICKMESSAGES_DB_CREATE =
        "create table "  + QUICKMESSAGES_DB_TABLE + " (" +
        QuickMessages._ID          + " integer primary key autoincrement, " +
        QuickMessages.QUICKMESSAGE + " text, " +
        QuickMessages.ORDER        + " integer default " + QUICKMESSAGES_ORDERING_DEFAULT +
        ");";

    public SmsPopupDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (Log.DEBUG) Log.v("SmsPopupDatabase: Creating Database");
        db.execSQL(CONTACTS_DB_CREATE);
        db.execSQL(QUICKMESSAGES_DB_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (Log.DEBUG) Log.v("SmsPopupDatabase: Upgrading Database");
        db.execSQL("DROP TABLE IF EXISTS " + CONTACTS_DB_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + QUICKMESSAGES_DB_TABLE);
        onCreate(db);
    }
}