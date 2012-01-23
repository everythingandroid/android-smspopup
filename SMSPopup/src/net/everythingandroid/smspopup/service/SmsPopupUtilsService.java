package net.everythingandroid.smspopup.service;

import net.everythingandroid.smspopup.provider.SmsMmsMessage;
import net.everythingandroid.smspopup.util.Log;
import net.everythingandroid.smspopup.util.ManageNotification;
import net.everythingandroid.smspopup.util.SmsPopupUtils;
import android.content.Intent;

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
        if (Log.DEBUG) Log.v("SMSPopupUtilsService: doWakefulWork()");

        final String action = intent.getAction();

        if (ACTION_MARK_THREAD_READ.equals(action)) {
            if (Log.DEBUG) Log.v("SMSPopupUtilsService: Marking thread read");
            SmsMmsMessage message = new SmsMmsMessage(this, intent.getExtras());
            message.setThreadRead();
        } else if (ACTION_MARK_MESSAGE_READ.equals(action)) {
            if (Log.DEBUG) Log.v("SMSPopupUtilsService: Marking message read");
            SmsMmsMessage message = new SmsMmsMessage(this, intent.getExtras());
            message.setMessageRead();
        } else if (ACTION_DELETE_MESSAGE.equals(action)) {
            if (Log.DEBUG) Log.v("SMSPopupUtilsService: Deleting message");
            SmsMmsMessage message = new SmsMmsMessage(this, intent.getExtras());
            message.delete();
        } else if (ACTION_QUICKREPLY.equals(action)) {
            if (Log.DEBUG) Log.v("SMSPopupUtilsService: Quick Reply to message");
            SmsMmsMessage message = new SmsMmsMessage(this, intent.getExtras());
            // message.setThreadRead();
            message.replyToMessage(intent.getStringExtra(SmsMmsMessage.EXTRAS_QUICKREPLY));
        } else if (ACTION_UPDATE_NOTIFICATION.equals(action)) {
            if (Log.DEBUG) Log.v("SMSPopupUtilsService: Updating notification");
            updateNotification(intent);
        }
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
