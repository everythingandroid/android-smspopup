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
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.telephony.gsm.SmsManager;

public class SmsMessageSender {
  private final Context mContext;
  private final int mNumberOfDests;
  private final String[] mDests;
  private final String mMessageText;
  private final String mServiceCenter;
  private final long mThreadId;
  private long mTimestamp;

  // Default preference values
  private static final boolean DEFAULT_DELIVERY_REPORT_MODE = false;

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


  private static final String[] SERVICE_CENTER_PROJECTION =
    new String[] {REPLY_PATH_PRESENT, SERVICE_CENTER,};

  // private static final String[] DATE_PROJECTION = new String[] {
  // DATE
  // };

  private static final int COLUMN_REPLY_PATH_PRESENT = 0;
  private static final int COLUMN_SERVICE_CENTER = 1;

  // http://android.git.kernel.org/?p=platform/packages/apps/Mms.git;a=blob;f=src/com/android/mms/transaction/MessageStatusReceiver.java
  public static final String MESSAGE_STATUS_RECEIVED_ACTION =
    "com.android.mms.transaction.MessageStatusReceiver.MESSAGE_STATUS_RECEIVED";

  // http://android.git.kernel.org/?p=platform/packages/apps/Mms.git;a=blob;f=src/com/android/mms/transaction/SmsReceiverService.java
  public static final String MESSAGE_SENT_ACTION = "com.android.mms.transaction.MESSAGE_SENT";

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
  }

  public boolean sendMessage() {
    if (!(mThreadId > 0)) {
      return false;
    }

    final String MMS_PACKAGE_NAME = "com.android.mms";
    final String MMS_STATUS_RECEIVED_CLASS_NAME =
      "com.android.mms.transaction.MessageStatusReceiver";
    final String MMS_SENT_CLASS_NAME = "com.android.mms.transaction.SmsReceiver";

    SmsManager smsManager = SmsManager.getDefault();

    for (int i = 0; i < mNumberOfDests; i++) {
      ArrayList<String> messages = smsManager.divideMessage(mMessageText);
      int messageCount = messages.size();
      if (Log.DEBUG) Log.v("messageCount = " + messageCount);
      ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>(messageCount);
      ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(messageCount);

      // SharedPreferences prefs =
      // PreferenceManager.getDefaultSharedPreferences(mContext);

      // boolean requestDeliveryReport = prefs.getBoolean(
      // MessagingPreferenceActivity.SMS_DELIVERY_REPORT_MODE,
      // DEFAULT_DELIVERY_REPORT_MODE);
      boolean requestDeliveryReport = DEFAULT_DELIVERY_REPORT_MODE;

      Uri uri = null;
      try {
        uri =
          addMessage(mContext.getContentResolver(), mDests[i], mMessageText, null, mTimestamp,
              requestDeliveryReport, mThreadId);
      } catch (SQLiteException e) {
        // TODO: show error here
        // SqliteWrapper.checkSQLiteException(mContext, e);
      }

      for (int j = 0; j < messageCount; j++) {
        if (requestDeliveryReport) {

          deliveryIntents.add(PendingIntent.getBroadcast(mContext, 0, new Intent(
              MESSAGE_STATUS_RECEIVED_ACTION, uri).setClassName(MMS_PACKAGE_NAME,
                  MMS_STATUS_RECEIVED_CLASS_NAME),
                  // MessageStatusReceiver.class),
                  0));
        }
        sentIntents.add(PendingIntent.getBroadcast(mContext, 0,
            new Intent(MESSAGE_SENT_ACTION, uri)
        .setClassName(MMS_PACKAGE_NAME, MMS_SENT_CLASS_NAME),
        // SmsReceiver.class
        0));
      }
      smsManager.sendMultipartTextMessage(mDests[i], mServiceCenter, messages, sentIntents,
          deliveryIntents);
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
}
