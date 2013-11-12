package net.everythingandroid.smspopup.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.WakefulBroadcastReceiver;

import net.everythingandroid.smspopup.BuildConfig;
import net.everythingandroid.smspopup.provider.SmsMmsMessage;
import net.everythingandroid.smspopup.provider.SmsPopupContract.ContactNotifications;
import net.everythingandroid.smspopup.util.Log;
import net.everythingandroid.smspopup.util.ManageNotification;
import net.everythingandroid.smspopup.util.SmsPopupUtils;
import net.everythingandroid.smspopup.util.SmsPopupUtils.ContactIdentification;

import java.util.ArrayList;

public class SmsPopupUtilsService extends IntentService {
    private static final String TAG = SmsPopupUtilsService.class.getName();

    private static final String PREFIX = "net.everythingandroid.smspopup.";
    public static final String ACTION_MARK_THREAD_READ = PREFIX + "ACTION_MARK_THREAD_READ";
    public static final String ACTION_MARK_MESSAGE_READ = PREFIX + "ACTION_MARK_MESSAGE_READ";
    public static final String ACTION_DELETE_MESSAGE = PREFIX + "ACTION_DELETE_MESSAGE";
    public static final String ACTION_UPDATE_NOTIFICATION = PREFIX + "ACTION_UPDATE_NOTIFICATION";
    public static final String ACTION_QUICKREPLY = PREFIX + "ACTION_QUICKREPLY";
    public static final String ACTION_SYNC_CONTACT_NAMES = PREFIX + "ACTION_SYNC_CONTACT_NAMES";

    public SmsPopupUtilsService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (BuildConfig.DEBUG) Log.v("SMSPopupUtilsService: doWakefulWork()");

        final String action = intent.getAction();

        if (ACTION_MARK_THREAD_READ.equals(action)) {
            if (BuildConfig.DEBUG) Log.v("SMSPopupUtilsService: Marking thread read");
            SmsMmsMessage message = new SmsMmsMessage(this, intent.getExtras());
            message.setThreadRead();
        } else if (ACTION_MARK_MESSAGE_READ.equals(action)) {
            if (BuildConfig.DEBUG) Log.v("SMSPopupUtilsService: Marking message read");
            SmsMmsMessage message = new SmsMmsMessage(this, intent.getExtras());
            message.setMessageRead();
        } else if (ACTION_DELETE_MESSAGE.equals(action)) {
            if (BuildConfig.DEBUG) Log.v("SMSPopupUtilsService: Deleting message");
            SmsMmsMessage message = new SmsMmsMessage(this, intent.getExtras());
            message.delete();
        } else if (ACTION_QUICKREPLY.equals(action)) {
            if (BuildConfig.DEBUG) Log.v("SMSPopupUtilsService: Quick Reply to message");
            SmsMmsMessage message = new SmsMmsMessage(this, intent.getExtras());
            message.replyToMessage(intent.getStringExtra(SmsMmsMessage.EXTRAS_QUICKREPLY));
        } else if (ACTION_UPDATE_NOTIFICATION.equals(action)) {
            if (BuildConfig.DEBUG) Log.v("SMSPopupUtilsService: Updating notification");
            updateNotification(intent);
        } else if (ACTION_SYNC_CONTACT_NAMES.equals(action)) {
            if (BuildConfig.DEBUG) Log.v("SMSPopupUtilsService: Sync'ing contact names");
            syncContactNames(this);
        }

        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }

    /**
     * Any custom contact notifications are stored in a local database, including the contact names
     * so we can quickly display them on the configuration screens. This function will loop through
     * the locally stored contacts and check to see if the system contact name has changed at all
     * (from either a manual edit or some sort of sync event). If so, it will update the local
     * database with the new name.
     * @param context Context.
     * @return The number of rows updated with a new name.
     */
    private int syncContactNames(Context context) {

        final ContentResolver contentResolver = context.getContentResolver();
        final Cursor cursor = contentResolver.query(
                ContactNotifications.CONTENT_URI, null, null, null, null);

        if (cursor == null) {
            return 0;
        }

        if (cursor.getCount() == 0) {
            return 0;
        }

        int count = 0;
        int updatedCount = 0;
        String id;
        String contactName;
        String contactLookup;
        String contactId;

        // loop through the local sms popup contact notifications table
        while (cursor.moveToNext()) {
            count++;

            id = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactNotifications._ID));
            contactName = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactNotifications.CONTACT_NAME));
            contactId = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactNotifications.CONTACT_ID));
            contactLookup = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactNotifications.CONTACT_LOOKUPKEY));

            ContactIdentification contactInfo =
                    SmsPopupUtils.getPersonNameByLookup(context, contactLookup, contactId);

            if (contactInfo != null) {
                boolean runUpdate = false;
                ContentValues vals = new ContentValues();

                if (contactName == null || !contactName.equals(contactInfo.contactName)) {
                    vals.put(ContactNotifications.CONTACT_NAME, contactInfo.contactName);
                    runUpdate = true;
                }

                if (contactId == null || !contactId.equals(contactInfo.contactId)) {
                    vals.put(ContactNotifications.CONTACT_ID, contactInfo.contactId);
                    runUpdate = true;
                }

                if (contactLookup == null || !contactLookup.equals(contactInfo.contactLookup)) {
                    vals.put(ContactNotifications.CONTACT_LOOKUPKEY, contactInfo.contactLookup);
                    runUpdate = true;
                }

                if (runUpdate && 1 == contentResolver.update(
                        ContactNotifications.buildContactUri(id), vals, null, null)) {
                    updatedCount++;
                }
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        if (BuildConfig.DEBUG)
            Log.v("Sync Contacts: " + updatedCount + " / " + count);

        return updatedCount;
    }

    public static void startSyncContactNames(Context context) {
        Intent intent = new Intent(context, SmsPopupUtilsService.class);
        intent.setAction(SmsPopupUtilsService.ACTION_SYNC_CONTACT_NAMES);
        WakefulBroadcastReceiver.startWakefulService(context, intent);
    }

    private void updateNotification(Intent intent) {
        // In the case the user is "replying" to the message (ie. starting an
        // external intent) we need to ignore all messages in the thread when
        // calculating the unread messages to show in the status notification
        boolean ignoreThread = intent.getBooleanExtra(SmsMmsMessage.EXTRAS_REPLYING, false);

        long threadId = 0;
        if (ignoreThread) {
            // If ignoring messages from the thread, pass the full message over
            final SmsMmsMessage message = new SmsMmsMessage(this, intent.getExtras());
            threadId = message.getThreadId();
        }

        // Get the most recent message + total message counts
        final ArrayList<SmsMmsMessage> messages = SmsPopupUtils.getUnreadMessages(this);

        if (messages != null) {
            if (threadId > 0) {
                for (int i=0; i<messages.size(); i++) {
                    if (messages.get(i).getThreadId() == threadId) {
                        messages.remove(i);
                    }
                }
            }
            final int numMessages = messages.size();

            if (numMessages > 0) {
            // Update the notification in the status bar
            ManageNotification.update(this, messages.get(numMessages - 1), numMessages);
            } else {
                ManageNotification.clearAll(this);
            }
        } else {
            ManageNotification.clearAll(this);
        }
    }
}
