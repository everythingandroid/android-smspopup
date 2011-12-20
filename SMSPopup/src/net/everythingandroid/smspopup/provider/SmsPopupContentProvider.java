package net.everythingandroid.smspopup.provider;

import net.everythingandroid.smspopup.provider.SmsPopupContract.ContactNotifications;
import net.everythingandroid.smspopup.provider.SmsPopupContract.QuickMessages;
import net.everythingandroid.smspopup.util.Log;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class SmsPopupContentProvider extends ContentProvider {

    private static final int CONTACTS = 100;
    private static final int CONTACTS_ID = 101;
    private static final int QUICKMESSAGES = 200;
    private static final int QUICKMESSAGES_ID = 201;

    private static final UriMatcher uriMatcher = buildUriMatcher();

    private SmsPopupDatabase mOpenHelper;

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = SmsPopupContract.CONTENT_AUTHORITY;
        final String contactsPath = SmsPopupContract.PATH_CONTACTS;
        final String quickMessagesPath = SmsPopupContract.PATH_QUICKMESSAGES;
        matcher.addURI(authority, contactsPath, CONTACTS);
        matcher.addURI(authority, contactsPath + "/*", CONTACTS_ID);
        matcher.addURI(authority, quickMessagesPath, QUICKMESSAGES);
        matcher.addURI(authority, quickMessagesPath + "/*", QUICKMESSAGES_ID);

        return matcher;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = uriMatcher.match(uri);
        int count = 0;
        switch (match) {
        case CONTACTS:
            count = db.delete(SmsPopupDatabase.CONTACTS_DB_TABLE, selection, selectionArgs);
            break;
        case CONTACTS_ID:
            final String contactSelection = ContactNotifications._ID + " = ?";
            final String[] contactSelectionArgs = { ContactNotifications.getContactId(uri) };
            count =
                    db.delete(SmsPopupDatabase.CONTACTS_DB_TABLE, contactSelection,
                            contactSelectionArgs);
            break;
        case QUICKMESSAGES:
            count = db.delete(SmsPopupDatabase.QUICKMESSAGES_DB_TABLE, selection, selectionArgs);
            break;
        case QUICKMESSAGES_ID:
            final String qmSelection = QuickMessages._ID + " = ?";
            final String[] qmSelectionArgs = { QuickMessages.getQuickMessageId(uri) };
            count =
                    db.delete(SmsPopupDatabase.QUICKMESSAGES_DB_TABLE, qmSelection, qmSelectionArgs);
            break;
        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    @Override
    public String getType(Uri uri) {
        final int match = uriMatcher.match(uri);
        switch (match) {
        case CONTACTS:
            return ContactNotifications.CONTENT_TYPE;
        case CONTACTS_ID:
            return ContactNotifications.CONTENT_ITEM_TYPE;
        case QUICKMESSAGES:
            return QuickMessages.CONTENT_TYPE;
        case QUICKMESSAGES_ID:
            return QuickMessages.CONTENT_ITEM_TYPE;
        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = uriMatcher.match(uri);
        Uri newUri = null;
        switch (match) {
        case CONTACTS:
            db.insertOrThrow(SmsPopupDatabase.CONTACTS_DB_TABLE, null, values);
            newUri =
                    ContactNotifications.buildContactUri(values
                            .getAsString(ContactNotifications._ID));
            updateContactNotificationSummary(newUri);
            break;
        case QUICKMESSAGES:
            db.insertOrThrow(SmsPopupDatabase.QUICKMESSAGES_DB_TABLE, null, values);
            newUri = QuickMessages.buildQuickMessageUri(QuickMessages._ID);
            break;
        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(newUri, null);

        return newUri;
    }

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        mOpenHelper = new SmsPopupDatabase(context);
        return mOpenHelper == null ? false : true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        final SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        final int match = uriMatcher.match(uri);
        switch (match) {
        case CONTACTS:
            sqlBuilder.setTables(SmsPopupDatabase.CONTACTS_DB_TABLE);
            break;
        case CONTACTS_ID:
            sqlBuilder.setTables(SmsPopupDatabase.CONTACTS_DB_TABLE);
            sqlBuilder.appendWhere(
                    ContactNotifications._ID + " = " + ContactNotifications.getContactId(uri));
            break;
        case QUICKMESSAGES:
            sqlBuilder.setTables(SmsPopupDatabase.QUICKMESSAGES_DB_TABLE);
            if (sortOrder == null) {
                sortOrder = QuickMessages.ORDER;
            }
            break;
        case QUICKMESSAGES_ID:
            sqlBuilder.setTables(SmsPopupDatabase.QUICKMESSAGES_DB_TABLE);
            sqlBuilder.appendWhere(
                    QuickMessages._ID + " = " + QuickMessages.getQuickMessageId(uri));
            break;
        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        final Cursor c =
                sqlBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = uriMatcher.match(uri);
        int count = 0;
        switch (match) {
        case CONTACTS:
            count = db.update(SmsPopupDatabase.CONTACTS_DB_TABLE, values, selection, selectionArgs);
            break;
        case CONTACTS_ID:
            final String contactSelection = ContactNotifications._ID + " = ?";
            final String[] contactSelectionArgs = { ContactNotifications.getContactId(uri) };
            count =
                    db.update(SmsPopupDatabase.CONTACTS_DB_TABLE, values, contactSelection,
                            contactSelectionArgs);
            if (!values.containsKey(ContactNotifications.SUMMARY)
                    && !values.containsKey(ContactNotifications.CONTACT_NAME)) {
                updateContactNotificationSummary(uri);
            }
            break;
        case QUICKMESSAGES:
            count =
                    db.update(SmsPopupDatabase.QUICKMESSAGES_DB_TABLE, values, selection,
                            selectionArgs);
            break;
        case QUICKMESSAGES_ID:
            final String qmSelection = QuickMessages._ID + " = ?";
            final String[] qmSelectionArgs = { QuickMessages.getQuickMessageId(uri) };
            count =
                    db.update(SmsPopupDatabase.QUICKMESSAGES_DB_TABLE, values, qmSelection,
                            qmSelectionArgs);
            break;
        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    /**
     * Update the custom contact notification summary field.
     * 
     * @param uri
     */
    private void updateContactNotificationSummary(Uri uri) {
        final Cursor c = query(uri, null, null, null, null);
        final String one = "1";
        if (c == null) {
            return;
        }

        if (c.getCount() != 1) {
            c.close();
            return;
        }

        c.moveToFirst();

        StringBuilder summary = new StringBuilder("Popup ");

        if (one.equals(c.getString(c.getColumnIndexOrThrow(ContactNotifications.POPUP_ENABLED)))) {
            summary.append("enabled");
        } else {
            summary.append("disabled");
        }

        summary.append(", Notifications ");
        if (!one.equals(c.getString(c.getColumnIndexOrThrow(ContactNotifications.ENABLED)))) {
            summary.append("disabled");
        } else {
            summary.append("enabled");
            if (one.equals(c.getString(c
                    .getColumnIndexOrThrow(ContactNotifications.VIBRATE_ENABLED)))) {
                summary.append(", Vibrate on");
            }
            if (one.equals(c.getString(c.getColumnIndexOrThrow(ContactNotifications.LED_ENABLED)))) {
                String ledColor =
                        c.getString(c.getColumnIndexOrThrow(ContactNotifications.LED_COLOR));
                Log.v("ledColor = " + ledColor);
                if ("custom".equals(ledColor)) {
                    ledColor = "Custom";
                }
                summary.append(", " + ledColor + " LED");
            }
        }

        ContentValues vals = new ContentValues();
        vals.put(ContactNotifications.SUMMARY, summary.toString());
        update(uri, vals, null, null);

        c.close();
    }

}
