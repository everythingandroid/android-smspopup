package net.everythingandroid.smspopup.service;

import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.provider.SmsMmsMessage;
import net.everythingandroid.smspopup.receiver.ReminderReceiver;
import net.everythingandroid.smspopup.util.Log;
import net.everythingandroid.smspopup.util.ManageNotification;
import net.everythingandroid.smspopup.util.ManagePreferences.Defaults;
import net.everythingandroid.smspopup.util.ManageWakeLock;
import net.everythingandroid.smspopup.util.SmsPopupUtils;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class ReminderService extends WakefulIntentService {
    private static final String TAG = ReminderService.class.getName();
    private static PendingIntent reminderPendingIntent = null;

    public static final String ACTION_REMIND = "net.everythingandroid.smspopup.ACTION_REMIND";
    public static final String ACTION_OTHER = "net.everythingandroid.smspopup.ACTION_OTHER";

    public ReminderService() {
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
        if (Log.DEBUG) Log.v("ReminderReceiverService: handleMessage()");

        String action = intent.getAction();

        if (ACTION_REMIND.equals(action)) {
            if (Log.DEBUG) Log.v("ReminderReceiverService: processReminder()");
            ReminderService.processReminder(this, intent);
        } else if (Intent.ACTION_DELETE.equals(action)) {
            // TODO: update message count pref
            if (Log.DEBUG) Log.v("ReminderReceiverService: cancelReminder()");
            ReminderService.cancelReminder(this);
        }
    }


    private static void processReminder(Context context, Intent intent) {
        int unreadSms = SmsPopupUtils.getUnreadMessagesCount(context);
        if (unreadSms > 0) {
            SmsMmsMessage message = new SmsMmsMessage(context, intent.getExtras());

            SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            int repeat_times =
                    Integer.parseInt(myPrefs.getString(
                            context.getString(R.string.pref_notif_repeat_times_key),
                            Defaults.PREFS_NOTIF_REPEAT_TIMES));

            // values of repeat_times as follows:
            // -1 repeat indefinitely
            // positive value is exact number of repeats
            if (message.getReminderCount() <= repeat_times || repeat_times == -1) {
                ManageNotification.show(context, message);
                ReminderService.scheduleReminder(context, message);
                if (myPrefs.getBoolean(context.getString(R.string.pref_notif_repeat_screen_on_key),
                        Defaults.PREFS_NOTIF_REPEAT_SCREEN_ON)) {
                    ManageWakeLock.acquireFull(context);
                }

            }
        }
    }

    /*
     * This will schedule a reminder notification to play in the future using
     * the system AlarmManager. The time till the reminder and number of
     * reminders is taken from user preferences.
     */
    public static void scheduleReminder(Context context, SmsMmsMessage message) {

        SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean reminder_notifications =
                myPrefs.getBoolean(context.getString(R.string.pref_notif_repeat_key),
                        Defaults.PREFS_NOTIF_REPEAT);

        if (reminder_notifications) {
            int reminder_interval =
                    Integer.parseInt(myPrefs.getString(
                            context.getString(R.string.pref_notif_repeat_interval_key),
                            Defaults.PREFS_NOTIF_REPEAT_INTERVAL));

            reminder_interval *= 60;

            AlarmManager mAM = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            Intent reminderIntent = new Intent(context, ReminderReceiver.class);
            reminderIntent.setAction(ReminderService.ACTION_REMIND);
            message.incrementReminderCount();
            reminderIntent.putExtras(message.toBundle());

            reminderPendingIntent =
                    PendingIntent.getBroadcast(context, 0, reminderIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT);

            long triggerTime = System.currentTimeMillis() + (reminder_interval * 1000);
            if (Log.DEBUG)
                Log.v("ReminderReceiver: scheduled reminder notification in " + reminder_interval
                        + " seconds, count is " + message.getReminderCount());
            mAM.set(AlarmManager.RTC_WAKEUP, triggerTime, reminderPendingIntent);
        }
    }

    /*
     * Cancels the reminder notification in the case the user reads the message
     * before it ends up playing.
     */
    public static void cancelReminder(Context context) {
        if (reminderPendingIntent != null) {
            AlarmManager myAM = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            myAM.cancel(reminderPendingIntent);
            reminderPendingIntent.cancel();
            reminderPendingIntent = null;
            if (Log.DEBUG) Log.v("ReminderReceiver: cancelReminder()");
        }
    }

}
