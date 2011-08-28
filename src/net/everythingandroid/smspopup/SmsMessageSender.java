/*
 * Copyright (C) 2008 Esmertec AG. Copyright (C) 2008 The Android Open Source
 * Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.everythingandroid.smspopup;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;

public class SmsMessageSender {
  private final Context mContext;
  private final int mNumberOfDests;
  private final String[] mDests;
  private final String mMessageText;
  private final String mServiceCenter;
  private final long mThreadId;
  private long mTimestamp;
  private boolean splitMessage;
  private boolean requestDeliveryReport;

  // Default preference values
  private static final boolean DEFAULT_DELIVERY_REPORT_MODE = false;
  private static final boolean DEFAULT_SPLIT_MESSAGE = false;

  // http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/provider/Telephony.java
  public static final String REPLY_PATH_PRESENT = "reply_path_present";
  public static final String SERVICE_CENTER = "service_center";
  // public static final String DATE = "date";
  /**
   * The thread ID of the message
   * <P>
   * Type: INTEGER
   * </P>
   */
  public static final String THREAD_ID = "thread_id";

  /**
   * The address of the other party
   * <P>
   * Type: TEXT
   * </P>
   */
  public static final String ADDRESS = "address";

  /**
   * The person ID of the sender
   * <P>
   * Type: INTEGER (long)
   * </P>
   */
  public static final String PERSON_ID = "person";

  /**
   * The date the message was sent
   * <P>
   * Type: INTEGER (long)
   * </P>
   */
  public static final String DATE = "date";

  /**
   * Has the message been read
   * <P>
   * Type: INTEGER (boolean)
   * </P>
   */
  public static final String READ = "read";

  /**
   * The TP-Status value for the message, or -1 if no status has been received
   */
  public static final String STATUS = "status";
  public static final int STATUS_NONE = -1;
  public static final int STATUS_COMPLETE = 0;
  public static final int STATUS_PENDING = 64;
  public static final int STATUS_FAILED = 128;

  /**
   * The subject of the message, if present
   * <P>
   * Type: TEXT
   * </P>
   */
  public static final String SUBJECT = "subject";

  /**
   * The body of the message
   * <P>
   * Type: TEXT
   * </P>
   */
  public static final String BODY = "body";

  public static final String TYPE = "type";

  public static final int MESSAGE_TYPE_ALL    = 0;
  public static final int MESSAGE_TYPE_INBOX  = 1;
  public static final int MESSAGE_TYPE_SENT   = 2;
  public static final int MESSAGE_TYPE_DRAFT  = 3;
  public static final int MESSAGE_TYPE_OUTBOX = 4;
  public static final int MESSAGE_TYPE_FAILED = 5; // for failed outgoing messages
  public static final int MESSAGE_TYPE_QUEUED = 6; // for messages to send later

  private static final String[] SERVICE_CENTER_PROJECTION =
    new String[] {REPLY_PATH_PRESENT, SERVICE_CENTER,};

  // private static final String[] DATE_PROJECTION = new String[] {
  // DATE
  // };

  private static final int COLUMN_REPLY_PATH_PRESENT = 0;
  private static final int COLUMN_SERVICE_CENTER = 1;

  // http://android.git.kernel.org/?p=platform/packages/apps/Mms.git;a=blob;f=src/com/android/mms/transaction/MessageStatusReceiver.java
  public static final String MESSAGING_STATUS_RECEIVED_ACTION =
    "com.android.mms.transaction.MessageStatusReceiver.MESSAGE_STATUS_RECEIVED";
  public static final String MESSAGING_PACKAGE_NAME = "com.android.mms";
  public static final String MESSAGING_STATUS_CLASS_NAME =
    MESSAGING_PACKAGE_NAME + ".transaction.MessageStatusReceiver";
  public static final String MESSAGING_RECEIVER_CLASS_NAME =
    MESSAGING_PACKAGE_NAME + ".transaction.SmsReceiver";
  public static final String MESSAGING_CONVO_CLASS_NAME = "com.android.mms.ui.ConversationList";
  public static final String MESSAGING_COMPOSE_CLASS_NAME = "com.android.mms.ui.ComposeMessageActivity";

// Bad idea, commenting out for now
/*
  public static final String[][] MMS_APP_LIST =
  {
    { // Stock Android messaging app (the bulk of phones use the same class/package names)
      MESSAGING_PACKAGE_NAME,
      MESSAGING_PACKAGE_NAME + ".transaction.SmsReceiver",
      SmsReceiverService.MESSAGE_SENT_ACTION,
      MESSAGING_STATUS_CLASS_NAME,
      MESSAGING_STATUS_RECEIVED_ACTION,

    },
    { // Motoblur phones like Droid X
      "com.motorola.blur.conversations",
      "com.motorola.blur.conversations.transaction.SmsReceiver",
      "com.motorola.blur.conversations.transaction.MESSAGE_SENT",
      "com.motorola.blur.conversations.transaction.MessageStatusReceiver",
      "com.motorola.blur.conversations.transaction.MessageStatusReceiver.MESSAGE_STATUS_RECEIVED",
    },
  };
*/

  /**
   * Send a message via the system app and system db
   *
   * @param context the context
   * @param dests the destination addresses
   * @param msgText the message text
   * @param threadId the message thread id
   */
  public SmsMessageSender(Context context, String[] dests, String msgText, long threadId) {
    mContext = context;
    mMessageText = msgText;
    mNumberOfDests = dests.length;
    mDests = new String[mNumberOfDests];
    System.arraycopy(dests, 0, mDests, 0, mNumberOfDests);
    mTimestamp = System.currentTimeMillis();
    mThreadId = threadId;
    // mThreadId = threadId > 0 ? threadId
    // : Threads.getOrCreateThreadId(context,
    // new HashSet<String>(Arrays.asList(dests)));
    mServiceCenter = getOutgoingServiceCenter(mThreadId);

    SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

    // Fetch split message pref
    splitMessage =
      mPrefs.getBoolean(
          mContext.getString(R.string.pref_split_message_key), DEFAULT_SPLIT_MESSAGE);

    // Fetch delivery report pref
    requestDeliveryReport =
      mPrefs.getBoolean(
          mContext.getString(R.string.pref_delivery_report_key), DEFAULT_DELIVERY_REPORT_MODE);
  }

  public boolean sendMessage() {
    if (!(mThreadId > 0)) {
      return false;
    }

    // Don't try to send an empty message.
    if ((mMessageText == null) || (mNumberOfDests == 0)) {
      return false;
    }

    SmsManager smsManager = SmsManager.getDefault();

    for (int i = 0; i < mNumberOfDests; i++) {
      ArrayList<String> messages = smsManager.divideMessage(mMessageText);
      int messageCount = messages.size();
      ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>(messageCount);
      ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(messageCount);

      // Apparently some CDMA networks have a 160 character limit on sending messages (no
      // multi-part SMS messages), if splitMessage is true we will break apart the messages
      // and send separately.
      if (splitMessage) {
        for (int j = 0; j < messageCount; j++) {
          Uri uri = null;
          try {
            uri =
              addMessage(mContext.getContentResolver(), mDests[i], messages.get(j),
                  null, mTimestamp, requestDeliveryReport, mThreadId);
          } catch (SQLiteException e) {
            // TODO: show error here
            // SqliteWrapper.checkSQLiteException(mContext, e);
          }

          PendingIntent deliveryReportIntent = null;
          if (requestDeliveryReport) {
            deliveryReportIntent =
              PendingIntent.getBroadcast(mContext, 0,
                  new Intent(MESSAGING_STATUS_RECEIVED_ACTION, uri)
              .setClassName(MESSAGING_PACKAGE_NAME, MESSAGING_STATUS_CLASS_NAME), 0);
          }

          PendingIntent sentIntent =
            PendingIntent.getBroadcast(mContext, 0,
                new Intent(SmsReceiverService.MESSAGE_SENT_ACTION, uri)
            .setClass(mContext, SmsReceiver.class), 0);

          smsManager.sendTextMessage(
              mDests[i], mServiceCenter, messages.get(j), sentIntent, deliveryReportIntent);
        }

      } else { // Otherwise send as multipart message
        Uri uri = null;
        try {
          uri =
            addMessage(mContext.getContentResolver(), mDests[i], mMessageText, null,
                mTimestamp, requestDeliveryReport, mThreadId);
        } catch (SQLiteException e) {
          // TODO: show error here
          // SqliteWrapper.checkSQLiteException(mContext, e);
        }

        for (int j = 0; j < messageCount; j++) {
          if (requestDeliveryReport) {
            deliveryIntents.add(PendingIntent.getBroadcast(mContext, 0,
                new Intent(
                    MESSAGING_STATUS_RECEIVED_ACTION, uri).setClassName(
                        MESSAGING_PACKAGE_NAME, MESSAGING_STATUS_CLASS_NAME),
                        // MessageStatusReceiver.class),
                        0));
          }

          sentIntents.add(PendingIntent.getBroadcast(mContext, 0,
              new Intent(
                  SmsReceiverService.MESSAGE_SENT_ACTION, uri).setClass(
                      mContext, SmsReceiver.class),
                      //.setClassName(MMS_PACKAGE_NAME, MMS_SENT_CLASS_NAME),
                      // SmsReceiver.class
                      0));
        }
        if (Log.DEBUG) Log.v("Sending message in " + messageCount + " parts");
        smsManager.sendMultipartTextMessage(
            mDests[i], mServiceCenter, messages, sentIntents, deliveryIntents);
      }
    }
    return false;
  }

  /**
   * Get the service center to use for a reply.
   *
   * The rule from TS 23.040 D.6 is that we send reply messages to the service
   * center of the message to which we're replying, but only if we haven't
   * already replied to that message and only if <code>TP-Reply-Path</code> was
   * set in that message.
   *
   * Therefore, return the service center from the most recent message in the
   * conversation, but only if it is a message from the other party, and only if
   * <code>TP-Reply-Path</code> is set. Otherwise, return null.
   */
  private String getOutgoingServiceCenter(long threadId) {
    Cursor cursor = null;

    try {
      cursor =
        mContext.getContentResolver().query(SmsPopupUtils.SMS_CONTENT_URI,
            SERVICE_CENTER_PROJECTION, "thread_id = " + threadId, null, "date DESC");

      // cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
      // Sms.CONTENT_URI, SERVICE_CENTER_PROJECTION,
      // "thread_id = " + threadId, null, "date DESC");

      if ((cursor == null) || !cursor.moveToFirst()) {
        return null;
      }

      boolean replyPathPresent = (1 == cursor.getInt(COLUMN_REPLY_PATH_PRESENT));
      return replyPathPresent ? cursor.getString(COLUMN_SERVICE_CENTER) : null;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  /**
   * Add an SMS to the Out box.
   *
   * @param resolver the content resolver to use
   * @param address the address of the sender
   * @param body the body of the message
   * @param subject the psuedo-subject of the message
   * @param date the timestamp for the message
   * @param deliveryReport whether a delivery report was requested for the
   *        message
   * @return the URI for the new message
   */
  public static Uri addMessage(ContentResolver resolver, String address, String body,
      String subject, Long date, boolean deliveryReport, long threadId) {

    /**
     * The content:// style URL for this table
     */
    final Uri CONTENT_URI = Uri.parse("content://sms/outbox");

    return addMessageToUri(resolver, CONTENT_URI, address, body, subject, date, true,
        deliveryReport, threadId);
  }

  /**
   * Add an SMS to the given URI with thread_id specified.
   *
   * @param resolver the content resolver to use
   * @param uri the URI to add the message to
   * @param address the address of the sender
   * @param body the body of the message
   * @param subject the psuedo-subject of the message
   * @param date the timestamp for the message
   * @param read true if the message has been read, false if not
   * @param deliveryReport true if a delivery report was requested, false if not
   * @param threadId the thread_id of the message
   * @return the URI for the new message
   */
  public static Uri addMessageToUri(ContentResolver resolver, Uri uri, String address, String body,
      String subject, Long date, boolean read, boolean deliveryReport, long threadId) {

    ContentValues values = new ContentValues(7);

    values.put(ADDRESS, address);
    if (date != null) {
      values.put(DATE, date);
    }
    values.put(READ, read ? Integer.valueOf(1) : Integer.valueOf(0));
    values.put(SUBJECT, subject);
    values.put(BODY, body);
    if (deliveryReport) {
      values.put(STATUS, STATUS_PENDING);
    }
    if (threadId != -1L) {
      values.put(THREAD_ID, threadId);
    }
    return resolver.insert(uri, values);
  }

  /**
   * Move a message to the given folder.
   *
   * @param context the context to use
   * @param uri the message to move
   * @param folder the folder to move to
   * @return true if the operation succeeded
   */
  public static boolean moveMessageToFolder(Context context, Uri uri, int folder) {
    if (uri == null) {
      return false;
    }

    boolean markAsUnread = false;
    boolean markAsRead = false;
    switch(folder) {
      case MESSAGE_TYPE_INBOX:
      case MESSAGE_TYPE_DRAFT:
        break;
      case MESSAGE_TYPE_OUTBOX:
      case MESSAGE_TYPE_SENT:
        markAsRead = true;
        break;
      case MESSAGE_TYPE_FAILED:
      case MESSAGE_TYPE_QUEUED:
        markAsUnread = true;
        break;
      default:
        return false;
    }

    ContentValues values = new ContentValues(2);

    values.put(TYPE, folder);
    if (markAsUnread) {
      values.put(READ, Integer.valueOf(0));
    } else if (markAsRead) {
      values.put(READ, Integer.valueOf(1));
    }

    int result = 0;

    try {
      result = context.getContentResolver().update(uri, values, null, null);
    } catch (Exception e) {

    }

    return 1 == result;

  }

}
