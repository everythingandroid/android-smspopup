package net.everythingandroid.smspopup;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Contacts;
import android.provider.Contacts.PeopleColumns;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.telephony.gsm.SmsMessage;
import android.text.TextUtils;

public class SmsPopupUtils {
  //Content URIs for SMS app, these may change in future SDK
  public static final Uri MMS_SMS_CONTENT_URI = Uri.parse("content://mms-sms/");
  public static final Uri THREAD_ID_CONTENT_URI =
    Uri.withAppendedPath(MMS_SMS_CONTENT_URI, "threadID");
  public static final Uri CONVERSATION_CONTENT_URI =
    Uri.withAppendedPath(MMS_SMS_CONTENT_URI, "conversations");

  public static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
  public static final Uri SMS_INBOX_CONTENT_URI = Uri.withAppendedPath(SMS_CONTENT_URI, "inbox");

  public static final Uri MMS_CONTENT_URI = Uri.parse("content://mms");
  public static final Uri MMS_INBOX_CONTENT_URI = Uri.withAppendedPath(MMS_CONTENT_URI, "inbox");

  public static final String SMS_ID = "_id";
  public static final String SMS_TO_URI = "smsto:/";
  public static final String SMS_MIME_TYPE = "vnd.android-dir/mms-sms";
  public static final int READ_THREAD = 1;
  public static final int MESSAGE_TYPE_SMS = 1;
  public static final int MESSAGE_TYPE_MMS = 2;

  public static final int CONTACT_PHOTO_PLACEHOLDER = android.R.drawable.ic_dialog_info;

  private static final String AUTHOR_CONTACT_INFO = "Adam K <adam@everythingandroid.net>";

  /**
   * Looks up a contacts display name by contact id - if not found, the address
   * (phone number) will be formatted and returned instead.
   */
  public static String getPersonName(Context context, String id, String address) {

    // Check for id, if null return the formatting phone number as the name
    if (id == null) {
      if (address != null) {
        return PhoneNumberUtils.formatNumber(address);
      } else {
        return null;
      }
    }

    Cursor cursor = context.getContentResolver().query(
        Uri.withAppendedPath(Contacts.People.CONTENT_URI, id),
        new String[] { PeopleColumns.DISPLAY_NAME }, null, null, null);
    if (cursor != null) {
      try {
        if (cursor.getCount() > 0) {
          cursor.moveToFirst();
          String name = cursor.getString(0);
          if (Log.DEBUG) Log.v("Contact Display Name: " + name);
          return name;
        }
      } finally {
        cursor.close();
      }
    }

    if (address != null) {
      return PhoneNumberUtils.formatNumber(address);
    }
    return null;
  }

  /**
   * Looks up a contacts id, given their address (phone number in this case).
   * Returns null if not found
   */
  public static String getPersonIdFromPhoneNumber(Context context, String address) {
    if (address == null)
      return null;

    Cursor cursor = context.getContentResolver().query(
        Uri.withAppendedPath(Contacts.Phones.CONTENT_FILTER_URL, address),
        new String[] { Contacts.Phones.PERSON_ID }, null, null, null);

    if (cursor != null) {
      try {
        if (cursor.getCount() > 0) {
          cursor.moveToFirst();
          Long id = Long.valueOf(cursor.getLong(0));
          if (Log.DEBUG) Log.v("Found person: " + id);
          return (String.valueOf(id));
        }
      } finally {
        cursor.close();
      }
    }
    return null;
  }

  /**
   * 
   * Looks up a contats photo by their contact id, returns a Bitmap array
   * that represents their photo (or null if not found)
   * 
   * @param context
   * @param id contact id
   * @return Bitmap of the contacts photo (null if none or an error)
   */
  public static Bitmap getPersonPhoto(Context context, String id) {
    if (id == null)
      return null;

    if ("0".equals(id))
      return null;

    return Contacts.People.loadContactPhoto(context,
        Uri.withAppendedPath(Contacts.People.CONTENT_URI, id),
        SmsPopupUtils.CONTACT_PHOTO_PLACEHOLDER, null);
  }


  /**
   * 
   * Tries to locate the message thread id given the address (phone or email)
   * of the message sender.
   * 
   * @param context a context to use
   * @param address phone number or email address of sender
   * @return the thread id (or 0 if there was a problem)
   */
  public static long findThreadIdFromAddress(Context context, String address) {
    if (address == null) return 0;

    String THREAD_RECIPIENT_QUERY = "recipient";

    Uri.Builder uriBuilder = THREAD_ID_CONTENT_URI.buildUpon();
    uriBuilder.appendQueryParameter(THREAD_RECIPIENT_QUERY, address);

    long threadId = 0;

    Cursor cursor = context.getContentResolver().query(
        uriBuilder.build(),
        new String[] { SMS_ID },
        null, null, null);
    if (cursor != null) {
      try {
        if (cursor.moveToFirst()) {
          threadId = cursor.getLong(0);
        }
      } finally {
        cursor.close();
      }
    }
    return threadId;
  }

  /**
   * Marks a specific message as read
   */
  public static void setMessageRead(Context context, long messageId, int messageType) {

    SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    boolean markRead = myPrefs.getBoolean(
        context.getString(R.string.pref_markread_key),
        Boolean.valueOf(context.getString(R.string.pref_markread_default)));
    if (!markRead) return;

    if (messageId > 0) {
      ContentValues values = new ContentValues(1);
      values.put("read", READ_THREAD);

      Uri messageUri;

      if (SmsMmsMessage.MESSAGE_TYPE_MMS == messageType) {
        messageUri = Uri.withAppendedPath(MMS_CONTENT_URI, String.valueOf(messageId));
      } else if (SmsMmsMessage.MESSAGE_TYPE_SMS == messageType) {
        messageUri = Uri.withAppendedPath(SMS_CONTENT_URI, String.valueOf(messageId));
      } else {
        return;
      }

      // Log.v("messageUri for marking message read: " + messageUri.toString());

      ContentResolver cr = context.getContentResolver();
      int result;
      try {
        result = cr.update(messageUri, values, null, null);
      } catch (Exception e) {
        result = 0;
      }
      if (Log.DEBUG) Log.v(String.format("message id = %s marked as read, result = %s", messageId, result ));
    }
  }

  /**
   * Marks a specific message thread as read - all messages in the thread will
   * be marked read
   */
  public static void setThreadRead(Context context, long threadId) {
    SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    boolean markRead = myPrefs.getBoolean(
        context.getString(R.string.pref_markread_key),
        Boolean.valueOf(context.getString(R.string.pref_markread_default)));

    if (!markRead) return;

    if (threadId > 0) {
      ContentValues values = new ContentValues(1);
      values.put("read", READ_THREAD);

      ContentResolver cr = context.getContentResolver();
      int result = 0;
      try {
        result = cr.update(
            ContentUris.withAppendedId(CONVERSATION_CONTENT_URI, threadId),
            values, null, null);
      } catch (Exception e) {
        if (Log.DEBUG) Log.v("error marking thread read");
      }
      if (Log.DEBUG) Log.v("thread id " + threadId + " marked as read, result = " + result);
    }
  }

  /**
   * Tries to locate the message id (from the system database), given the message
   * thread id, the timestamp of the message and the type of message (sms/mms)
   */
  public static long findMessageId(Context context, long threadId, long timestamp,
      String body, int messageType) {

    long id = 0;
    String where = String.format("body='%s' and ", body);
    if (threadId > 0) {

      if (Log.DEBUG) Log.v("Trying to find message ID");
      if (SmsMmsMessage.MESSAGE_TYPE_MMS == messageType) {
        // It seems MMS timestamps are stored in a seconds, whereas SMS timestamps are in millis
        where = "date=" + timestamp / 1000;
      } else {
        // As of Android OS >1.5 (>Cupcake) the system messaging process uses its own timestamp
        // rather than the carrier timestamp on the SMS, therefore we can't match up directly
        // against the timestamp as it's likely there is a difference by several millisecs.
        // Instead we use a hacky method to find the message by using a buffer period.
        where = String.format("date between %s and %s",
            timestamp - SmsMmsMessage.MESSAGE_COMPARE_TIME_BUFFER,
            timestamp + SmsMmsMessage.MESSAGE_COMPARE_TIME_BUFFER);
      }

      // Log.v("Where is: " + where);
      // Log.v("ThreadId is: " + threadId);

      Cursor cursor = context.getContentResolver().query(
          ContentUris.withAppendedId(CONVERSATION_CONTENT_URI, threadId),
          new String[] { "_id", "date", "thread_id" },
          where,
          null, "date desc");

      if (cursor != null) {
        try {
          if (cursor.moveToFirst()) {
            id = cursor.getLong(0);
            if (Log.DEBUG) Log.v("Message id found = " + id);
            //Log.v("Timestamp = " + cursor.getLong(1));
          }
        } finally {
          cursor.close();
        }
      }
    }
    return id;
  }

  /**
   * Tries to delete a message from the system database, given the thread id,
   * the timestamp of the message and the message type (sms/mms).
   */
  public static void deleteMessage(Context context, long messageId, long threadId, int messageType) {

    if (messageId > 0) {
      if (Log.DEBUG) Log.v("id of message to delete is " + messageId);
      Uri deleteUri;

      if (SmsMmsMessage.MESSAGE_TYPE_MMS == messageType) {
        deleteUri = Uri.withAppendedPath(MMS_CONTENT_URI, String.valueOf(messageId));
      } else if (SmsMmsMessage.MESSAGE_TYPE_SMS == messageType) {
        deleteUri = Uri.withAppendedPath(SMS_CONTENT_URI, String.valueOf(messageId));
      } else {
        return;
      }
      int count = context.getContentResolver().delete(deleteUri, null, null);
      if (Log.DEBUG) Log.v("Messages deleted: " + count);
      if (count == 1) {
        //TODO: should only set the thread read if there are no more unread
        // messages
        setThreadRead(context, threadId);
      }
    }
  }

  /**
   * 
   */
  public static Intent getSmsIntent() {
    Intent conversations = new Intent(Intent.ACTION_MAIN);
    //conversations.addCategory(Intent.CATEGORY_DEFAULT);
    conversations.setType(SMS_MIME_TYPE);
    // should I be using FLAG_ACTIVITY_RESET_TASK_IF_NEEDED??
    int flags =
      Intent.FLAG_ACTIVITY_NEW_TASK |
      Intent.FLAG_ACTIVITY_SINGLE_TOP |
      Intent.FLAG_ACTIVITY_CLEAR_TOP;
    conversations.setFlags(flags);

    return conversations;
  }

  /**
   * 
   */
  public static Intent getSmsToIntentFromThreadId(Context context, long threadId) {
    Intent popup = new Intent(Intent.ACTION_VIEW);
    // should I be using FLAG_ACTIVITY_RESET_TASK_IF_NEEDED??
    int flags =
      Intent.FLAG_ACTIVITY_NEW_TASK |
      Intent.FLAG_ACTIVITY_SINGLE_TOP |
      Intent.FLAG_ACTIVITY_CLEAR_TOP;
    popup.setFlags(flags);
    if (threadId > 0) {
      //Log.v("^^Found threadId (" + threadId + "), sending to Sms intent");
      popup.setData(Uri.withAppendedPath(THREAD_ID_CONTENT_URI, String.valueOf(threadId)));
    } else {
      return getSmsIntent();
    }
    return popup;
  }

  /**
   * 
   */
  public static void launchEmailToIntent(Context context, String subject, boolean includeDebug) {
    Intent msg = new Intent(Intent.ACTION_SEND);
    String[] recipients={AUTHOR_CONTACT_INFO};

    String body = "";

    if (includeDebug) {
      body = "\n\n----------\nSysinfo - " + Build.FINGERPRINT + "\n"
      + "Model: " + Build.MODEL + "\n\n";

      // Array of preference keys to include in email
      String[] pref_keys = {
          context.getString(R.string.pref_enabled_key),
          context.getString(R.string.pref_timeout_key),
          context.getString(R.string.pref_privacy_key),
          context.getString(R.string.pref_dimscreen_key),
          context.getString(R.string.pref_markread_key),
          context.getString(R.string.pref_onlyShowOnKeyguard_key),
          context.getString(R.string.pref_show_delete_button_key),
          context.getString(R.string.pref_blur_key),
          context.getString(R.string.pref_notif_enabled_key),
          context.getString(R.string.pref_notif_sound_key),
          context.getString(R.string.pref_vibrate_key),
          context.getString(R.string.pref_vibrate_pattern_key),
          context.getString(R.string.pref_vibrate_pattern_custom_key),
          context.getString(R.string.pref_flashled_key),
          context.getString(R.string.pref_flashled_color_key),
          context.getString(R.string.pref_notif_repeat_key),
          context.getString(R.string.pref_notif_repeat_times_key),
          context.getString(R.string.pref_notif_repeat_interval_key),
      };

      SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
      Map<String, ?> m = myPrefs.getAll();

      body += subject + " config -\n";
      for (int i=0; i<pref_keys.length; i++) {
        try {
          body += pref_keys[i] + ": " + String.valueOf(m.get(pref_keys[i])) + "\n";
        } catch (NullPointerException e) {
          // Nothing to do here
        }
      }

      // Add locale info
      body += "locale: "
        + context.getResources().getConfiguration().locale.getDisplayName()
        + "\n";

      // Add audio info
      AudioManager AM =
        (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

      String audioMode = "audio_mode: ";
      switch (AM.getMode()) {
        case AudioManager.MODE_NORMAL:
          audioMode += "MODE_NORMAL"; break;
        case AudioManager.MODE_IN_CALL:
          audioMode += "MODE_IN_CALL"; break;
        case AudioManager.MODE_RINGTONE:
          audioMode += "MODE_RINGTONE"; break;
        default:
          audioMode += "MODE is UNKNOWN"; break;
      }
      body += audioMode + "\n";

      String audioRouting = "audio_routing: ";
      switch (AM.getRouting(AudioManager.MODE_NORMAL)) {
        case AudioManager.ROUTE_SPEAKER:
          audioRouting += "ROUTE_SPEAKER"; break;
        case AudioManager.ROUTE_BLUETOOTH:
          audioRouting += "ROUTE_BLUETOOTH"; break;
        case AudioManager.ROUTE_HEADSET:
          audioRouting += "ROUTE_HEADSET"; break;
        default:
          audioRouting += "ROUTE is UNKNOWN"; break;
      }
      body += audioRouting + "\n";

      TelephonyManager TM =
        (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
      String callState = "call state: ";
      switch (TM.getCallState()) {
        case TelephonyManager.CALL_STATE_IDLE:
          callState += "CALL_STATE_IDLE"; break;
        case TelephonyManager.CALL_STATE_OFFHOOK:
          callState += "CALL_STATE_OFFHOOK"; break;
        case TelephonyManager.CALL_STATE_RINGING:
          callState += "CALL_STATE_RINGING"; break;
        default:
          callState += "CALL_STATE is UNKNOWN"; break;
      }
      body += callState + "\n";

    }

    msg.putExtra(Intent.EXTRA_EMAIL, recipients);
    msg.putExtra(Intent.EXTRA_SUBJECT, subject);
    msg.putExtra(Intent.EXTRA_TEXT, body);

    msg.setType("message/rfc822");
    context.startActivity(Intent.createChooser(msg, "Send E-mail"));
  }

  /**
   * Return current unread message count from system db (sms and mms)
   * 
   * @param context
   * @return unread sms+mms message count
   */
  public static int getUnreadMessagesCount(Context context) {
    return getUnreadMessagesCount(context, 0);
  }

  /**
   * Return current unread message count from system db (sms and mms)
   * 
   * @param context
   * @param timestamp only messages before this timestamp will be counted
   * @return unread sms+mms message count
   */
  public static int getUnreadMessagesCount(Context context, long timestamp) {
    return getUnreadSmsCount(context, timestamp) + getUnreadMmsCount(context);
  }

  /**
   * Return current unread message count from system db (sms only)
   * 
   * @param context
   * @return unread sms message count
   */
  public static int getUnreadSmsCount(Context context) {
    return getUnreadSmsCount(context, 0);
  }

  /**
   * Return current unread message count from system db (sms only)
   * 
   * @param context
   * @param timestamp only messages before this timestamp will be counted
   * @return unread sms message count
   */
  public static int getUnreadSmsCount(Context context, long timestamp) {
    //    String SMS_READ_COLUMN = "read";
    //    String UNREAD_CONDITION = SMS_READ_COLUMN + "=0";
    String UNREAD_CONDITION = "read=0";

    if (timestamp > 0) {
      if (Log.DEBUG) Log.v("getUnreadSmsCount(), timestamp = " + timestamp);
      UNREAD_CONDITION += " and date<"
        + String.valueOf(timestamp-SmsMmsMessage.MESSAGE_COMPARE_TIME_BUFFER);
    }

    int count = 0;

    Cursor cursor = context.getContentResolver().query(
        SMS_INBOX_CONTENT_URI,
        new String[] { SMS_ID },
        UNREAD_CONDITION, null, null);

    if (cursor != null) {
      try {
        count = cursor.getCount();
      } finally {
        cursor.close();
      }
    }

    // We ignored the latest incoming message so add one to the total count
    if (timestamp > 0) {
      if (Log.DEBUG) Log.v("adding 1 to unread, previous count was " + count);
      count += 1;
    }

    if (Log.DEBUG) Log.v("sms unread count = " + count);
    return count;
  }

  /**
   * Return current unread message count from system db (mms only)
   * 
   * @param context
   * @return unread mms message count
   */
  public static int getUnreadMmsCount(Context context) {

    String MMS_READ_COLUMN = "read";
    String UNREAD_CONDITION = MMS_READ_COLUMN + "=0";

    int count = 0;

    Cursor cursor = context.getContentResolver().query(
        MMS_INBOX_CONTENT_URI,
        new String[] { SMS_ID },
        UNREAD_CONDITION, null, null);

    if (cursor != null) {
      try {
        count = cursor.getCount();
      } finally {
        cursor.close();
      }
    }
    if (Log.DEBUG) Log.v("mms unread count = " + count);
    return count;
  }

  /*
   * 
   */
  public static SmsMmsMessage getSmsDetails(Context context,
      long ignoreThreadId, boolean unreadOnly) {

    String SMS_READ_COLUMN = "read";
    String WHERE_CONDITION = unreadOnly ? SMS_READ_COLUMN + " = 0" : null;
    String SORT_ORDER = "date DESC";
    int count = 0;

    //Log.v(WHERE_CONDITION);

    if (ignoreThreadId > 0) {
      //			Log.v("Ignoring sms threadId = " + ignoreThreadId);
      WHERE_CONDITION += " AND thread_id != " + ignoreThreadId;
    }

    Cursor cursor = context.getContentResolver().query(
        SMS_INBOX_CONTENT_URI,
        new String[] { "_id", "thread_id", "address", "person", "date", "body" },
        WHERE_CONDITION,
        null,
        SORT_ORDER);

    if (cursor != null) {
      try {
        count = cursor.getCount();
        if (count > 0) {
          cursor.moveToFirst();

          //					String[] columns = cursor.getColumnNames();
          //					for (int i=0; i<columns.length; i++) {
          //						Log.v("columns " + i + ": " + columns[i] + ": "
          //								+ cursor.getString(i));
          //					}

          long messageId = cursor.getLong(0);
          long threadId = cursor.getLong(1);
          String address = cursor.getString(2);
          long contactId = cursor.getLong(3);
          String contactId_string = String.valueOf(contactId);
          long timestamp = cursor.getLong(4);

          String body = cursor.getString(5);

          if (!unreadOnly) {
            count = 0;
          }

          SmsMmsMessage smsMessage = new SmsMmsMessage(
              context, address, contactId_string, body, timestamp,
              threadId, count, messageId, SmsMmsMessage.MESSAGE_TYPE_SMS);

          return smsMessage;

        }
      } finally {
        cursor.close();
      }
    }
    return null;
  }

  public static SmsMmsMessage getSmsDetails(Context context) {
    return getSmsDetails(context, 0);
  }

  public static SmsMmsMessage getSmsDetails(Context context, boolean unreadOnly) {
    return getSmsDetails(context, 0, unreadOnly);
  }

  public static SmsMmsMessage getSmsDetails(Context context, long ignoreThreadId) {
    return getSmsDetails(context, ignoreThreadId, true);
  }

  /*
   * 
   */
  public static SmsMmsMessage getMmsDetails(Context context, long ignoreThreadId) {
    String MMS_READ_COLUMN = "read";
    String UNREAD_CONDITION = MMS_READ_COLUMN + " = 0";
    String SORT_ORDER = "date DESC";
    int count = 0;

    if (ignoreThreadId > 0) {
      UNREAD_CONDITION += " AND thread_id != " + ignoreThreadId;
    }

    Cursor cursor = context.getContentResolver().query(
        MMS_INBOX_CONTENT_URI,
        //new String[] { "m_id", "\"from\"", "sub", "d_tm", "thread_id" },
        new String[] { "_id", "thread_id", "date", "sub", "sub_cs" },
        UNREAD_CONDITION, null,
        SORT_ORDER);

    if (cursor != null) {
      try {
        count = cursor.getCount();
        if (count > 0) {
          cursor.moveToFirst();
          // String[] columns = cursor.getColumnNames();
          // for (int i=0; i<columns.length; i++) {
          // Log.v("columns " + i + ": " + columns[i] + ": "
          // + cursor.getString(i));
          // }
          String address = getMmsFrom(context, cursor.getLong(0));
          long threadId = cursor.getLong(1);
          long timestamp = cursor.getLong(2) * 1000;
          String subject = cursor.getString(3);

          SmsMmsMessage mmsMessage = new SmsMmsMessage(context, address, subject, timestamp,
              threadId, count, SmsMmsMessage.MESSAGE_TYPE_MMS);

          return mmsMessage;

        }
      } finally {
        cursor.close();
      }
    }
    return null;
  }

  public static SmsMmsMessage getMmsDetails(Context context) {
    return getMmsDetails(context, 0);
  }

  public static String getMmsFrom(Context context, long message_id) {

    Uri.Builder builder = MMS_CONTENT_URI.buildUpon();
    builder.appendPath(String.valueOf(message_id)).appendPath("addr");

    Cursor cursor = context.getContentResolver().query(builder.build(),
        // new String[] { "contact_id", "address", "charset", "type" },
        new String[] { "address", "contact_id", "charset", "type" },
        // "type="+ PduHeaders.FROM,
        "type=137",
        // null,
        null, null);

    if (cursor != null) {
      try {
        if (cursor.moveToFirst()) {

          // String[] columns = cursor.getColumnNames();
          // for (int i = 0; i < columns.length; i++) {
          // Log.v("columns " + i + ": " + columns[i] + ": "
          // + cursor.getString(i));
          // }
          String address = cursor.getString(0);
          return getDisplayName(context, address).trim();

          // Needed for i18n strings??
          // if (!TextUtils.isEmpty(from)) {
          // byte[] bytes = PduPersister.getBytes(from);
          // int charset = cursor.getInt(1);
          // return new EncodedStringValue(charset, bytes).getString();
          // }
        }
      } finally {
        cursor.close();
      }
    }
    return context.getString(android.R.string.unknownName);
  }

  public static final Pattern NAME_ADDR_EMAIL_PATTERN = Pattern
  .compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*");

  public static final Pattern QUOTED_STRING_PATTERN = Pattern
  .compile("\\s*\"([^\"]*)\"\\s*");

  private static String getEmailDisplayName(String displayString) {
    Matcher match = QUOTED_STRING_PATTERN.matcher(displayString);
    if (match.matches()) {
      return match.group(1);
    }

    return displayString;
  }

  private static String getDisplayName(Context context, String email) {
    Matcher match = NAME_ADDR_EMAIL_PATTERN.matcher(email);
    if (match.matches()) {
      // email has display name, return that
      return getEmailDisplayName(match.group(1));
    }

    // otherwise let's check the contacts list for a user with this email
    Cursor cursor = context.getContentResolver().query(
        Contacts.ContactMethods.CONTENT_EMAIL_URI,
        new String[] { Contacts.ContactMethods.NAME },
        Contacts.ContactMethods.DATA + " = \'" + email + "\'", null, null);

    if (cursor != null) {
      try {
        int columnIndex = cursor
        .getColumnIndexOrThrow(Contacts.ContactMethods.NAME);
        while (cursor.moveToNext()) {
          String name = cursor.getString(columnIndex);
          if (!TextUtils.isEmpty(name)) {
            return name;
          }
        }
      } finally {
        cursor.close();
      }
    }
    return email;
  }

  /*
   * Get the most recent unread message, returning in a SmsMmsMessage which is
   * suitable for updating the notification.  Optional param is the message object:
   * we can pull out the thread id of this message in the case the user is "replying"
   * to the message and we should ignore all messages in the thread when working out
   * what to display in the notification bar (as these messages will soon be marked read
   * but we can't be sure when the messaging app will actually start).
   * 
   */
  public static SmsMmsMessage getRecentMessage(Context context, SmsMmsMessage ignoreMessage) {
    long ignoreThreadId = 0;

    if (ignoreMessage != null) {
      ignoreThreadId = ignoreMessage.getThreadId();
    }

    SmsMmsMessage smsMessage = getSmsDetails(context, ignoreThreadId);
    SmsMmsMessage mmsMessage = getMmsDetails(context, ignoreThreadId);

    if (mmsMessage == null && smsMessage != null) {
      return smsMessage;
    }

    if (mmsMessage != null && smsMessage == null) {
      return mmsMessage;
    }

    if (mmsMessage != null && smsMessage != null) {
      if (mmsMessage.getTimestamp() < smsMessage.getTimestamp()) {
        return mmsMessage;
      }
      return smsMessage;
    }

    return null;
  }

  public static SmsMmsMessage getRecentMessage(Context context) {
    return getRecentMessage(context, null);
  }

  /**
   * Read the PDUs out of an {@link #SMS_RECEIVED_ACTION} or a
   * {@link #DATA_SMS_RECEIVED_ACTION} intent.
   * 
   * @param intent
   *           the intent to read from
   * @return an array of SmsMessages for the PDUs
   */
  public static final SmsMessage[] getMessagesFromIntent(Intent intent) {
    Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
    if (messages == null) {
      return null;
    }
    if (messages.length == 0) {
      return null;
    }

    byte[][] pduObjs = new byte[messages.length][];

    for (int i = 0; i < messages.length; i++) {
      pduObjs[i] = (byte[]) messages[i];
    }
    byte[][] pdus = new byte[pduObjs.length][];
    int pduCount = pdus.length;
    SmsMessage[] msgs = new SmsMessage[pduCount];
    for (int i = 0; i < pduCount; i++) {
      pdus[i] = pduObjs[i];
      msgs[i] = SmsMessage.createFromPdu(pdus[i]);
    }
    return msgs;
  }

  /**
   * This function will see if the most recent activity was the system messaging app so we can suppress
   * the popup as the user is likely already viewing messages or composing a new message
   */
  public static final boolean inMessagingApp(Context context) {
    // TODO: move these to static strings somewhere
    final String PACKAGE_NAME = "com.android.mms";
    //final String COMPOSE_CLASS_NAME = "com.android.mms.ui.ComposeMessageActivity";
    final String CONVO_CLASS_NAME = "com.android.mms.ui.ConversationList";

    ActivityManager mAM = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

    List<RunningTaskInfo> mRunningTaskList = mAM.getRunningTasks(1);
    Iterator<RunningTaskInfo> mIterator = mRunningTaskList.iterator();
    if (mIterator.hasNext()) {
      RunningTaskInfo mRunningTask = mIterator.next();
      if (mRunningTask != null) {
        ComponentName runningTaskComponent = mRunningTask.baseActivity;

        //				Log.v("baseActivity = " + mRunningTask.baseActivity.toString());
        //				Log.v("topActivity = " + mRunningTask.topActivity.toString());

        if (PACKAGE_NAME.equals(runningTaskComponent.getPackageName()) &&
            CONVO_CLASS_NAME.equals(runningTaskComponent.getClassName())) {
          if (Log.DEBUG) Log.v("User in messaging app - from running task");
          return true;
        }
      }
    }

    /*
		List<RecentTaskInfo> mActivityList = mAM.getRecentTasks(1, 0);
		Iterator<RecentTaskInfo> mIterator = mActivityList.iterator();

		if (mIterator.hasNext()) {
			RecentTaskInfo mRecentTask = (RecentTaskInfo) mIterator.next();
			Intent recentTaskIntent = mRecentTask.baseIntent;

			if (recentTaskIntent != null) {
				ComponentName recentTaskComponentName = recentTaskIntent.getComponent();
				if (recentTaskComponentName != null) {
					String recentTaskClassName = recentTaskComponentName.getClassName();
					if (PACKAGE_NAME.equals(recentTaskComponentName.getPackageName()) &&
							(COMPOSE_CLASS_NAME.equals(recentTaskClassName) ||
							 CONVO_CLASS_NAME.equals(recentTaskClassName))) {
						if (Log.DEBUG) Log.v("User in messaging app");
						return true;
					}
				}
			}
		}
     */

    /*
		These appear to be the 2 main intents that mean the user is using the messaging app

		action "android.intent.action.MAIN"
		data null
		class "com.android.mms.ui.ConversationList"
		package "com.android.mms"

		action "android.intent.action.VIEW"
		data "content://mms-sms/threadID/3"
		class "com.android.mms.ui.ComposeMessageActivity"
		package "com.android.mms"
     */

    return false;
  }

  /**
   * Enables or disables the main SMS receiver
   */
  public static void enableSMSPopup(Context context, boolean enable) {
    PackageManager pm = context.getPackageManager();
    ComponentName cn = new ComponentName(context, SmsReceiver.class);

    // Update preference so it reflects in the preference activity
    SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor settings = myPrefs.edit();
    settings.putBoolean(context.getString(R.string.pref_enabled_key), enable);
    settings.commit();

    if (enable) {
      if (Log.DEBUG) Log.v("SMSPopup receiver is enabled");
      pm.setComponentEnabledSetting(cn,
          PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
          PackageManager.DONT_KILL_APP);

      // Send a broadcast to disable other SMS Popup apps
      disableOtherSMSPopup(context);

    } else {
      if (Log.DEBUG) Log.v("SMSPopup receiver is disabled");
      pm.setComponentEnabledSetting(cn,
          PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
          PackageManager.DONT_KILL_APP);
    }
  }

  public static void disableOtherSMSPopup(Context context) {
    // Send a broadcast to disable SMS Popup Pro
    Intent i = new Intent(EnableDisableReceiver.ACTION_SMSPOPUP_DISABLE);
    i.setClassName("net.everythingandroid.smspopuppro", "net.everythingandroid.smspopuppro.ExternalEventReceiver");
    context.sendBroadcast(i);
  }

}