package net.everythingandroid.smspopup.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import net.everythingandroid.smspopup.provider.SmsPopupContract.ContactNotifications;
import net.everythingandroid.smspopup.provider.SmsPopupContract.Messages;
import net.everythingandroid.smspopup.provider.SmsPopupContract.QuickMessages;

public class SmsPopupContentProvider extends ContentProvider {

    private static final int CONTACTS = 100;
    private static final int CONTACTS_ID = 101;
    private static final int CONTACTS_LOOKUP = 102;
    private static final int QUICKMESSAGES = 200;
    private static final int QUICKMESSAGES_ID = 201;
    private static final int QUICKMESSAGES_UPDATE_ORDER = 202;
    private static final int MESSAGES = 300;
    private static final int MESSAGES_ID = 301;
    private static final int MESSAGES_MARK_READ = 302;

    private static final UriMatcher uriMatcher = buildUriMatcher();

    private SmsPopupDatabase mOpenHelper;

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = SmsPopupContract.CONTENT_AUTHORITY;
        final String contactsPath = ContactNotifications.PATH_CONTACTS;
        final String contactsLookupPath = ContactNotifications.PATH_CONTACTS_LOOKUP;
        final String quickMessagesPath = QuickMessages.PATH_QUICKMESSAGES;
        final String quickMessagesUpdateOrderPath = QuickMessages.PATH_QUICKMESSAGES_UPDATE_ORDER;
        final String messagesPath = Messages.PATH_MESSAGES;
        final String messageMarkReadPath = Messages.PATH_MESSAGE_READ;

        matcher.addURI(authority, contactsPath, CONTACTS);
        matcher.addURI(authority, contactsPath + "/#", CONTACTS_ID);
        matcher.addURI(authority, contactsLookupPath + "/*", CONTACTS_LOOKUP);
        matcher.addURI(authority, contactsLookupPath + "/*/#", CONTACTS_LOOKUP);
        matcher.addURI(authority, quickMessagesPath, QUICKMESSAGES);
        matcher.addURI(authority, quickMessagesPath + "/#", QUICKMESSAGES_ID);
        matcher.addURI(authority, quickMessagesPath + "/" + quickMessagesUpdateOrderPath + "/#",
                QUICKMESSAGES_UPDATE_ORDER);
        matcher.addURI(authority, messagesPath, MESSAGES);
        matcher.addURI(authority, messagesPath + "/#", MESSAGES_ID);
        matcher.addURI(authority, messagesPath + "/" + messageMarkReadPath + "/#",
                MESSAGES_MARK_READ);

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
            count = db.delete(
                    SmsPopupDatabase.CONTACTS_DB_TABLE, contactSelection, contactSelectionArgs);
            break;
        case QUICKMESSAGES:
            count = db.delete(SmsPopupDatabase.QUICKMESSAGES_DB_TABLE, selection, selectionArgs);
            break;
        case QUICKMESSAGES_ID:
            final String qmSelection = QuickMessages._ID + " = ?";
            final String[] qmSelectionArgs = { QuickMessages.getQuickMessageId(uri) };
            count = db.delete(
                    SmsPopupDatabase.QUICKMESSAGES_DB_TABLE, qmSelection, qmSelectionArgs);
            break;
        case MESSAGES:
            count = db.delete(SmsPopupDatabase.MESSAGES_DB_TABLE, selection, selectionArgs);
            break;
        case MESSAGES_ID:
            final String messageSelection = Messages._ID + " = ?";
            final String[] messageSelectionArgs = { Messages.getMessageId(uri) };
            count = db.delete(
                    SmsPopupDatabase.MESSAGES_DB_TABLE, messageSelection, messageSelectionArgs);
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
        case MESSAGES:
            return Messages.CONTENT_TYPE;
        case MESSAGES_ID:
            return Messages.CONTENT_ITEM_TYPE;
        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = uriMatcher.match(uri);
        Uri newUri = null;
        final long id;
        switch (match) {
        case CONTACTS:
            id = db.insertOrThrow(SmsPopupDatabase.CONTACTS_DB_TABLE, null, values);
            newUri = ContactNotifications.buildContactUri(id);
            updateContactNotificationSummary(newUri);
            break;
        case QUICKMESSAGES:
            // Fetch max of quick message order column
            Cursor c = db.query(SmsPopupDatabase.QUICKMESSAGES_DB_TABLE,
                    new String[] { "max(" + QuickMessages.ORDER + ")" },
                    null, null, null, null, null);

            int highestOrder = SmsPopupDatabase.QUICKMESSAGE_ORDER_DEFAULT;
            if (c != null && c.moveToFirst()) {
                highestOrder = c.getInt(0) + 1;
            }

            if (c != null) {
                c.close();
            }

            values.put(QuickMessages.ORDER, highestOrder);

            id = db.insertOrThrow(SmsPopupDatabase.QUICKMESSAGES_DB_TABLE, null, values);
            newUri = QuickMessages.buildQuickMessageUri(String.valueOf(id));
            break;
        case MESSAGES_ID:
            String systemMessageId = Messages.getMessageId(uri);
            values.put(Messages._ID, systemMessageId);
            values.put(Messages.ADDED, System.currentTimeMillis());
            id = db.insertOrThrow(SmsPopupDatabase.MESSAGES_DB_TABLE, null, values);
            newUri = Messages.buildMessageUri(String.valueOf(id));
            break;
        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (id == -1) {
            return null;
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
            if (sortOrder == null) {
                sortOrder = ContactNotifications.DEFAULT_SORT;
            }
            break;
        case CONTACTS_ID:
            sqlBuilder.setTables(SmsPopupDatabase.CONTACTS_DB_TABLE);
            sqlBuilder.appendWhere(
                    ContactNotifications._ID + " = " + ContactNotifications.getContactId(uri));
            break;
        case CONTACTS_LOOKUP:
            sqlBuilder.setTables(SmsPopupDatabase.CONTACTS_DB_TABLE);
            sqlBuilder.appendWhere(
                    ContactNotifications.CONTACT_LOOKUPKEY + " = '" +
                            ContactNotifications.getLookupKey(uri) + "'");
            final String contactId = ContactNotifications.getContactId(uri);
            if (contactId != null) {
                sqlBuilder.appendWhere(
                        " OR " + ContactNotifications.CONTACT_ID + " = " + contactId);
            }
            break;
        case QUICKMESSAGES:
            sqlBuilder.setTables(SmsPopupDatabase.QUICKMESSAGES_DB_TABLE);
            if (sortOrder == null) {
                sortOrder = QuickMessages.DEFAULT_SORT;
            }
            break;
        case QUICKMESSAGES_ID:
            sqlBuilder.setTables(SmsPopupDatabase.QUICKMESSAGES_DB_TABLE);
            sqlBuilder.appendWhere(
                    QuickMessages._ID + " = " + QuickMessages.getQuickMessageId(uri));
            break;
        case MESSAGES:
            sqlBuilder.setTables(SmsPopupDatabase.MESSAGES_DB_TABLE);
            if (sortOrder == null) {
                sortOrder = Messages.DEFAULT_SORT;
            }
            break;
        case MESSAGES_ID:
            sqlBuilder.setTables(SmsPopupDatabase.MESSAGES_DB_TABLE);
            sqlBuilder.appendWhere(
                    Messages._ID + " = " + Messages.getMessageId(uri));
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
            count = db.update(SmsPopupDatabase.QUICKMESSAGES_DB_TABLE, values,
                    selection, selectionArgs);
            break;
        case QUICKMESSAGES_ID:
            final String qmSelection = QuickMessages._ID + " = ?";
            final String[] qmSelectionArgs = { QuickMessages.getQuickMessageId(uri) };
            count = db.update(SmsPopupDatabase.QUICKMESSAGES_DB_TABLE, values,
                    qmSelection, qmSelectionArgs);
            break;
        case QUICKMESSAGES_UPDATE_ORDER:
            count = updateQuickMessageOrder(db, QuickMessages.getQuickMessageId(uri));
            break;
        case MESSAGES:
            count = db.update(SmsPopupDatabase.MESSAGES_DB_TABLE, values, selection, selectionArgs);
            break;
        case MESSAGES_ID:
            final String messageSelection = Messages._ID + " = ?";
            final String[] messageSelectionArgs = { Messages.getMessageId(uri) };
            count = db.update(SmsPopupDatabase.MESSAGES_DB_TABLE, values,
                    messageSelection, messageSelectionArgs);
            break;
        case MESSAGES_MARK_READ:
            final String readSelection = Messages._ID + " = ?";
            final String[] readSelectionArgs = { Messages.getMessageId(uri) };
            ContentValues readValues = new ContentValues();
            readValues.put(Messages.READ, 1);
            count = db.update(SmsPopupDatabase.MESSAGES_DB_TABLE, readValues,
                    readSelection, readSelectionArgs);
            break;
        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    private int updateQuickMessageOrder(SQLiteDatabase db, String id) {
        // Fetch minimum of quick message order column
        Cursor c = db.query(SmsPopupDatabase.QUICKMESSAGES_DB_TABLE,
                new String[] { "min(" + QuickMessages.ORDER + ")" }, null, null, null, null, null);

        if (c != null && c.moveToFirst()) {

            // Reduce by one so ordering will place this on top
            int lowestOrder = c.getInt(0) - 1;

            c.close();

            // If we're at zero, then we need to update all rows to make some space
            if (lowestOrder == 0) {
                db.execSQL(SmsPopupDatabase.QUICKMESSAGES_UPDATE_ORDER_SQL);
                lowestOrder += SmsPopupDatabase.QUICKMESSAGE_ORDER_DEFAULT;
            }

            // Update the row with new ordering value
            final ContentValues vals = new ContentValues();
            vals.put(QuickMessages.ORDER, lowestOrder);
            return db.update(SmsPopupDatabase.QUICKMESSAGES_DB_TABLE,
                    vals, QuickMessages._ID + " = ?", new String[] { id });
        }

        if (c != null) {
            c.close();
        }

        return 0;
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
            if (one.equals(c.getString(
                    c.getColumnIndexOrThrow(ContactNotifications.VIBRATE_ENABLED)))) {
                summary.append(", Vibrate on");
            }
            if (one.equals(c.getString(c.getColumnIndexOrThrow(ContactNotifications.LED_ENABLED)))) {
                String ledColor =
                        c.getString(c.getColumnIndexOrThrow(ContactNotifications.LED_COLOR));
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
