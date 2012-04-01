package net.everythingandroid.smspopup.service;

import net.everythingandroid.smspopup.BuildConfig;
import net.everythingandroid.smspopup.provider.SmsMmsMessage;
import net.everythingandroid.smspopup.provider.SmsPopupContract.ContactNotifications;
import net.everythingandroid.smspopup.util.Log;
import net.everythingandroid.smspopup.util.ManageNotification;
import net.everythingandroid.smspopup.util.SmsPopupUtils;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class SmsPopupUtilsService extends WakefulIntentService {
    private static final String TAG = SmsPopupUtilsService.class.getName();

    public static final String ACTION_MARK_THREAD_READ =
            "net.everythingandroid.smspopup.ACTION_MARK_THREAD_READ";

    public static final String ACTION_MARK_MESSAGE_READ =
            "net.everythingandroid.smspopup.ACTION_MARK_MESSAGE_READ";

    public static final String ACTION_DELETE_MESSAGE =
            "net.everythingandroid.smspopup.ACTION_DELETE_MESSAGE";

    public static final String ACTION_UPDATE_NOTIFICATION =
            "net.everythingandroid.smspopup.ACTION_UPDATE_NOTIFICATION";

    public static final String ACTION_QUICKREPLY =
            "net.everythingandroid.smspopup.ACTION_QUICKREPLY";
    
    public static final String ACTION_SYNC_CONTACT_NAMES =
            "net.everythingandroid.smspopup.ACTION_SYNC_CONTACT_NAMES";
    
    public SmsPopupUtilsService() {
        super(TAG);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.commonsware.cwac.wakeful.WakefulIntentService#doWakefulWork(android
     * .content.Intent)
     */
    @Override
    protected void doWakefulWork(Intent intent) {
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
            // message.setThreadRead();
            message.replyToMessage(intent.getStringExtra(SmsMmsMessage.EXTRAS_QUICKREPLY));
        } else if (ACTION_UPDATE_NOTIFICATION.equals(action)) {
            if (BuildConfig.DEBUG) Log.v("SMSPopupUtilsService: Updating notification");
            updateNotification(intent);
        } else if (ACTION_SYNC_CONTACT_NAMES.equals(action)) {
        	if (BuildConfig.DEBUG) Log.v("SMSPopupUtilsService: Sync'ing contact names");
        	syncContactNames(this);
        	
        }
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
        String contactId;
        String contactName;
        String contactLookup;
        String sysContactName;

        // loop through the local sms popup contact notifications table
        while (cursor.moveToNext()) {
            count++;

            contactName = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactNotifications.CONTACT_NAME));
            contactId = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactNotifications._ID));
            contactLookup = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactNotifications.CONTACT_LOOKUPKEY));

            sysContactName = SmsPopupUtils.getPersonNameByLookup(context, contactLookup);
            
            if (sysContactName != null && !sysContactName.equals(contactName)) {
            	ContentValues vals = new ContentValues();
            	vals.put(ContactNotifications.CONTACT_NAME, sysContactName);
            	if (1 == contentResolver.update(
            			ContactNotifications.buildContactUri(contactId), vals, null, null)) {
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

    private void updateNotification(Intent intent) {
        // In the case the user is "replying" to the message (ie. starting an
        // external intent) we need to ignore all messages in the thread when
        // calculating the unread messages to show in the status notification
        boolean ignoreThread = intent.getBooleanExtra(SmsMmsMessage.EXTRAS_REPLYING, false);

        SmsMmsMessage message;
        if (ignoreThread) {
            // If ignoring messages from the thread, pass the full message over
            message = new SmsMmsMessage(this, intent.getExtras());
        } else {
            // Otherwise we can just calculate unread messages by checking the
            // database as normal
            message = null;
        }

        // Get the most recent message + total message counts
        SmsMmsMessage recentMessage = SmsPopupUtils.getRecentMessage(this, message);

        // Update the notification in the status bar
        ManageNotification.update(this, recentMessage, 
                recentMessage == null ? 0 : recentMessage.getUnreadCount());
    }

}
