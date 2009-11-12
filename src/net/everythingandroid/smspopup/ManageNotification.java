package net.everythingandroid.smspopup;

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

/*
 * This class handles the Notifications (sounds/vibrate/LED)
 */
public class ManageNotification {
  public static final int NOTIFICATION_ALERT = 1337;
  public static final int NOTIFICATION_TEST = 888;
  public static final String defaultRingtone = Settings.System.DEFAULT_NOTIFICATION_URI.toString();

  /*
   * Show/play the notification given a SmsMmsMessage and a notification ID
   * (really just NOTIFICATION_ALERT for the main alert and NOTIFICATION_TEST
   * for the test notification from the preferences screen)
   */
  public static void show(Context context, SmsMmsMessage message, int notif) {
    notify(context, message, false, notif);
  }

  /*
   * Default to NOTIFICATION_ALERT if notif is left out
   */
  public static void show(Context context, SmsMmsMessage message) {
    notify(context, message, false, NOTIFICATION_ALERT);
  }

  /*
   * Only update the notification given the SmsMmsMessage (ie. do not play the
   * vibrate/sound, just update the text).
   */
  public static void update(Context context, SmsMmsMessage message) {
    // TODO: can I just use Notification.setLatestEventInfo() to update instead?

    if (message != null) {
      if (message.getUnreadCount() > 0) {
        notify(context, message, true, NOTIFICATION_ALERT);
        return;
      }
    }

    // TODO: Should reply flag be set to true?
    ManageNotification.clearAll(context, true);
  }

  /*
   * The main notify method, this thing is WAY too long. Needs to be broken up.
   */
  private static void notify(Context context, SmsMmsMessage message, boolean onlyUpdate, int notif) {

    ManagePreferences mPrefs = new ManagePreferences(context, message.getContactId());
    AudioManager AM = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

    // Fetch info from the message object
    int unreadCount = message.getUnreadCount();
    String messageBody = message.getMessageBody();
    String contactName = message.getContactName();
    long timestamp = message.getTimestamp();

    // Check if there are unread messages and if notifications are enabled -
    // if not, we're done :)
    if (unreadCount > 0
        && mPrefs.getBoolean(R.string.pref_notif_enabled_key, R.string.pref_notif_enabled_default,
            SmsPopupDbAdapter.KEY_ENABLED_NUM)) {

      // Make sure the NotificationManager is created
      // createNM(context);
      NotificationManager myNM =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

      // Get some preferences: vibrate and vibrate_pattern prefs
      boolean vibrate =
        mPrefs.getBoolean(R.string.pref_vibrate_key, R.string.pref_vibrate_default,
            SmsPopupDbAdapter.KEY_VIBRATE_ENABLED_NUM);
      String vibrate_pattern_raw =
        mPrefs.getString(R.string.pref_vibrate_pattern_key,
            R.string.pref_vibrate_pattern_default, SmsPopupDbAdapter.KEY_VIBRATE_PATTERN_NUM);
      String vibrate_pattern_custom_raw =
        mPrefs.getString(R.string.pref_vibrate_pattern_custom_key,
            R.string.pref_vibrate_pattern_default,
            SmsPopupDbAdapter.KEY_VIBRATE_PATTERN_CUSTOM_NUM);

      // Get LED preferences
      boolean flashLed =
        mPrefs.getBoolean(R.string.pref_flashled_key, R.string.pref_flashled_default,
            SmsPopupDbAdapter.KEY_LED_ENABLED_NUM);
      String flashLedCol =
        mPrefs.getString(R.string.pref_flashled_color_key, R.string.pref_flashled_color_default,
            SmsPopupDbAdapter.KEY_LED_COLOR_NUM);
      String flashLedColCustom =
        mPrefs.getString(R.string.pref_flashled_color_custom_key,
            R.string.pref_flashled_color_default, SmsPopupDbAdapter.KEY_LED_COLOR_NUM_CUSTOM);
      String flashLedPattern =
        mPrefs.getString(R.string.pref_flashled_pattern_key,
            R.string.pref_flashled_pattern_default, SmsPopupDbAdapter.KEY_LED_PATTERN_NUM);
      String flashLedPatternCustom =
        mPrefs.getString(R.string.pref_flashled_pattern_custom_key,
            R.string.pref_flashled_pattern_default, SmsPopupDbAdapter.KEY_LED_PATTERN_NUM_CUSTOM);

      // The default system ringtone
      // ("content://settings/system/notification_sound")
      // String defaultRingtone =
      // Settings.System.DEFAULT_NOTIFICATION_URI.toString();

      // Try and parse the user ringtone, use the default if it fails
      Uri alarmSoundURI =
        Uri.parse(mPrefs.getString(R.string.pref_notif_sound_key, defaultRingtone,
            SmsPopupDbAdapter.KEY_RINGTONE_NUM));

      if (Log.DEBUG) Log.v("Sounds URI = " + alarmSoundURI.toString());

      // The notification title, sub-text and text that will scroll
      String contentTitle;
      String contentText;
      SpannableString scrollText;

      // The default intent when the notification is clicked (Inbox)
      Intent smsIntent = SmsPopupUtils.getSmsInboxIntent();

      // See if user wants some privacy
      boolean privacyMode =
        mPrefs.getBoolean(R.string.pref_privacy_key, R.string.pref_privacy_default);

      // All done with prefs, close it up
      mPrefs.close();

      // If we're updating the notification, do not set the ticker text
      if (onlyUpdate) {
        scrollText = null;
      } else {

        // If we're in privacy mode and the keyguard is on then just display
        // the name of the person, otherwise scroll the name and message
        if (privacyMode && ManageKeyguard.inKeyguardRestrictedInputMode()) {
          scrollText =
            new SpannableString(context.getString(R.string.notification_scroll_privacy,
                contactName));
        } else {
          scrollText =
            new SpannableString(context.getString(R.string.notification_scroll, contactName,
                messageBody));

          // Set contact name as bold
          scrollText.setSpan(new StyleSpan(Typeface.BOLD), 0, contactName.length(),
              Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
      }

      // If more than one message waiting ...
      if (unreadCount > 1) {
        contentTitle = context.getString(R.string.notification_multiple_title);
        contentText = context.getString(R.string.notification_multiple_text, unreadCount);
        // smsIntent = SMSPopupUtils.getSmsIntent();
      } else { // Else 1 message, set text and intent accordingly
        contentTitle = contactName;
        contentText = messageBody;
        smsIntent = message.getReplyIntent();
      }

      /*
       * Ok, let's create our Notification object and set up all its parameters.
       */

      // Set the icon, scrolling text and timestamp
      Notification notification =
        new Notification(R.drawable.stat_notify_sms, scrollText, timestamp);

      // Set auto-cancel flag
      notification.flags = Notification.FLAG_AUTO_CANCEL;

      // Set audio stream to ring
      // notification.audioStreamType = AudioManager.STREAM_RING;
      notification.audioStreamType = Notification.STREAM_DEFAULT;

      /*
       * If this is a new notification (not updating a notification) then set
       * LED, vibrate and ringtone to fire
       */
      if (!onlyUpdate) {

        // Set up LED pattern and color
        if (flashLed) {
          notification.flags |= Notification.FLAG_SHOW_LIGHTS;

          /*
           * Set up LED blinking pattern
           */
          int[] led_pattern = null;

          if (context.getString(R.string.pref_custom_val).equals(flashLedPattern)) {
            led_pattern = parseLEDPattern(flashLedPatternCustom);
          } else {
            led_pattern = parseLEDPattern(flashLedPattern);
          }

          // Set to default if there was a problem
          if (led_pattern == null) {
            led_pattern =
              parseLEDPattern(context.getString(R.string.pref_flashled_pattern_default));
          }

          notification.ledOnMS = led_pattern[0];
          notification.ledOffMS = led_pattern[1];

          /*
           * Set up LED color
           */
          // Check if a custom color is set
          if (context.getString(R.string.pref_custom_val).equals(flashLedCol)) {
            flashLedCol = flashLedColCustom;
          }

          // Default in case the parse fails
          int col = Color.parseColor(context.getString(R.string.pref_flashled_color_default));

          // Try and parse the color
          if (flashLedCol != null) {
            try {
              col = Color.parseColor(flashLedCol);
            } catch (IllegalArgumentException e) {
              // No need to do anything here
            }
          }

          notification.ledARGB = col;
        }

        /*
         * Set up vibrate pattern
         */
        TelephonyManager TM = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int callState = TM.getCallState();

        // If vibrate is ON, or if phone is set to vibrate AND the phone is not on a call
        if ((vibrate || AudioManager.RINGER_MODE_VIBRATE == AM.getRingerMode())
            && callState != TelephonyManager.CALL_STATE_OFFHOOK) {
          long[] vibrate_pattern = null;
          if (context.getString(R.string.pref_custom_val).equals(vibrate_pattern_raw)) {
            vibrate_pattern = parseVibratePattern(vibrate_pattern_custom_raw);
          } else {
            vibrate_pattern = parseVibratePattern(vibrate_pattern_raw);
          }
          if (vibrate_pattern != null) {
            notification.vibrate = vibrate_pattern;
          } else {
            notification.defaults = Notification.DEFAULT_VIBRATE;
          }
        }

        // If in a call, start the media player
        if (callState == TelephonyManager.CALL_STATE_OFFHOOK
            && AM.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {

          // TODO: make this an option
          //          if (Log.DEBUG) Log.v("User on a call, running own mediaplayer");
          //          MediaPlayer mMediaPlayer = MediaPlayer.create(context, alarmSoundURI);
          //          mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
          //          mMediaPlayer.setLooping(false);
          //          mMediaPlayer.start();
        } else { // Otherwise just use built in notifications
          // Notification sound
          notification.sound = alarmSoundURI;
        }
      }

      // Set the PendingIntent if the status message is clicked
      PendingIntent notifIntent = PendingIntent.getActivity(context, 0, smsIntent, 0);

      // Set the messages that show when the status bar is pulled down
      notification.setLatestEventInfo(context, contentTitle, contentText, notifIntent);

      // Set number of events that this notification signifies (unread messages)
      if (unreadCount > 1) {
        notification.number = unreadCount;
      }

      // Set intent to execute if the "clear all" notifications button is
      // pressed -
      // basically stop any future reminders.
      Intent deleteIntent = new Intent(new Intent(context, ReminderReceiver.class));
      deleteIntent.setAction(Intent.ACTION_DELETE);
      PendingIntent pendingDeleteIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, 0);

      notification.deleteIntent = pendingDeleteIntent;

      // Seems this is needed for the .number value to take effect
      // on the Notification
      myNM.cancelAll();

      // Finally: run the notification!
      if (Log.DEBUG) Log.v("*** Notify running ***");
      myNM.notify(notif, notification);
    }
  }

  public static void clear(Context context) {
    clear(context, NOTIFICATION_ALERT);
  }

  public static void clear(Context context, int notif) {
    NotificationManager myNM =
      (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    myNM.cancel(notif);
    // createNM(context);
    // if (myNM != null) {
    // Log.v("Notification cleared");
    // myNM.cancel(notif);
    // }
  }

  public static void clearAll(Context context, boolean reply) {
    SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);

    if (reply
        || myPrefs.getBoolean(context.getString(R.string.pref_markread_key), Boolean
            .parseBoolean(context.getString(R.string.pref_markread_default)))) {
      NotificationManager myNM =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      myNM.cancelAll();

      // createNM(context);
      //
      // if (myNM != null) {
      // myNM.cancelAll();
      // Log.v("All notifications cleared");
      // }
    }
  }

  public static void clearAll(Context context) {
    clearAll(context, true);
  }

  /*
   * Parse the user provided custom vibrate pattern into a long[]
   */
  // TODO: tidy this up
  public static long[] parseVibratePattern(String stringPattern) {
    ArrayList<Long> arrayListPattern = new ArrayList<Long>();
    Long l;

    if (stringPattern == null) return null;

    String[] splitPattern = stringPattern.split(",");
    int VIBRATE_PATTERN_MAX_SECONDS = 60000;
    int VIBRATE_PATTERN_MAX_PATTERN = 100;

    for (int i = 0; i < splitPattern.length; i++) {
      try {
        l = Long.parseLong(splitPattern[i].trim());
      } catch (NumberFormatException e) {
        return null;
      }
      if (l > VIBRATE_PATTERN_MAX_SECONDS) {
        return null;
      }
      arrayListPattern.add(l);
    }

    // TODO: can i just cast the whole ArrayList into long[]?
    int size = arrayListPattern.size();
    if (size > 0 && size < VIBRATE_PATTERN_MAX_PATTERN) {
      long[] pattern = new long[size];
      for (int i = 0; i < pattern.length; i++) {
        pattern[i] = arrayListPattern.get(i);
      }
      return pattern;
    }

    return null;
  }

  public static int[] parseLEDPattern(String stringPattern) {
    int[] arrayPattern = new int[2];
    int on, off;

    if (stringPattern == null) return null;

    String[] splitPattern = stringPattern.split(",");

    if (splitPattern.length != 2) return null;

    int LED_PATTERN_MIN_SECONDS = 0;
    int LED_PATTERN_MAX_SECONDS = 60000;

    try {
      on = Integer.parseInt(splitPattern[0]);
    } catch (NumberFormatException e) {
      return null;
    }

    try {
      off = Integer.parseInt(splitPattern[1]);
    } catch (NumberFormatException e) {
      return null;
    }

    if (on >= LED_PATTERN_MIN_SECONDS && on <= LED_PATTERN_MAX_SECONDS
        && off >= LED_PATTERN_MIN_SECONDS && off <= LED_PATTERN_MAX_SECONDS) {
      arrayPattern[0] = on;
      arrayPattern[1] = off;
      return arrayPattern;
    }

    return null;
  }

}
