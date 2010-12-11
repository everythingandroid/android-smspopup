package net.everythingandroid.smspopup;

import net.everythingandroid.smspopup.ManagePreferences.Defaults;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ReminderReceiver extends BroadcastReceiver {
  private static PendingIntent reminderPendingIntent = null;

  /*
   * We're not going to do anything in the onReceive() as taking too long here
   * (>10 seconds) will cause a "Application Not Responding: Wait/Close" message
   * to the user. Instead we'll fire up a service that does the work in a
   * different thread.
   */
  @Override
  public void onReceive(Context context, Intent intent) {
    intent.setClass(context, ReminderReceiverService.class);
    ReminderReceiverService.beginStartingService(context, intent);
  }

  /*
   * This will schedule a reminder notification to play in the future using the
   * system AlarmManager. The time till the reminder and number of reminders is
   * taken from user preferences.
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

      AlarmManager myAM = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

      Intent reminderIntent = new Intent(context, ReminderReceiver.class);
      reminderIntent.setAction(ReminderReceiverService.ACTION_REMIND);
      message.incrementReminderCount();
      reminderIntent.putExtras(message.toBundle());
      // reminderIntent.putExtra(EXTRAS_COUNT, count + 1);

      reminderPendingIntent =
        PendingIntent.getBroadcast(context, 0, reminderIntent, PendingIntent.FLAG_CANCEL_CURRENT);

      long triggerTime = System.currentTimeMillis() + (reminder_interval * 1000);
      if (Log.DEBUG) Log.v("ReminderReceiver: scheduled reminder notification in " + reminder_interval
          + " seconds, count is " + message.getReminderCount());
      myAM.set(AlarmManager.RTC_WAKEUP, triggerTime, reminderPendingIntent);
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
