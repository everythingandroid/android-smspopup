package net.everythingandroid.smspopup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.text.format.DateUtils;

public class SmsMmsMessage {
  // Private EXTRAS strings
  private static final String PREFIX = "net.everythingandroid.smspopup.";
  private static final String EXTRAS_FROM_ADDRESS = PREFIX + "EXTRAS_FROM_ADDRESS";
  private static final String EXTRAS_MESSAGE_BODY = PREFIX + "EXTRAS_MESSAGE_BODY";
  private static final String EXTRAS_TIMESTAMP    = PREFIX + "EXTRAS_TIMESTAMP";
  private static final String EXTRAS_UNREAD_COUNT = PREFIX + "EXTRAS_UNREAD_COUNT";
  private static final String EXTRAS_THREAD_ID    = PREFIX + "EXTRAS_THREAD_ID";
  private static final String EXTRAS_CONTACT_ID   = PREFIX + "EXTRAS_CONTACT_ID";
  private static final String EXTRAS_CONTACT_NAME = PREFIX + "EXTRAS_CONTACT_NAME";
  private static final String EXTRAS_MESSAGE_TYPE = PREFIX + "EXTRAS_MESSAGE_TYPE";
  private static final String EXTRAS_MESSAGE_ID   = PREFIX + "EXTRAS_MESSAGE_ID";
  private static final String EXTRAS_EMAIL_GATEWAY= PREFIX + "EXTRAS_EMAIL_GATEWAY";

  // Public EXTRAS strings
  public static final String EXTRAS_NOTIFY         = PREFIX + "EXTRAS_NOTIFY";
  public static final String EXTRAS_REMINDER_COUNT = PREFIX + "EXTRAS_REMINDER_COUNT";
  public static final String EXTRAS_REPLYING       = PREFIX + "EXTRAS_REPLYING";
  public static final String EXTRAS_QUICKREPLY     = PREFIX + "EXTRAS_QUICKREPLY";

  // Message types
  public static final int MESSAGE_TYPE_SMS = 0;
  public static final int MESSAGE_TYPE_MMS = 1;

  // Timestamp compare buffer for incoming messages
  public static final int MESSAGE_COMPARE_TIME_BUFFER = 5000; // 5 seconds

  // Main message object private vars
  private Context context;
  private String fromAddress = null;
  private String messageBody = null;
  private long timestamp = 0;
  private int unreadCount = 0;
  private long threadId = 0;
  private String contactId = null;
  private String contactName = null;
  private int messageType = 0;
  private boolean notify = true;
  private int reminderCount = 0;
  private long messageId = 0;
  private boolean fromEmailGateway = false;

  /**
   * Construct SmsMmsMessage with minimal information - this is useful for when
   * a raw SMS comes in which just contains address, body and timestamp.  We
   * must then look in the database for the rest of the information
   */
  public SmsMmsMessage(Context _context, String _fromAddress, String _messageBody,
      long _timestamp, boolean _fromEmailGateway, int _messageType) {
    context = _context;
    fromAddress = _fromAddress;
    messageBody = _messageBody;
    timestamp = _timestamp;
    messageType = _messageType;
    fromEmailGateway = _fromEmailGateway;

    // If this SMS is from an email gateway then lookup contactId by email address
    if (fromEmailGateway) {
      if (Log.DEBUG) Log.v("Sms came from email gateway");
      contactId = SmsPopupUtils.getPersonIdFromEmail(context, fromAddress);
    } else { // Else lookup contactId by phone number
      if (Log.DEBUG) Log.v("Sms did NOT come from email gateway");
      contactId = SmsPopupUtils.getPersonIdFromPhoneNumber(context, fromAddress);
    }

    contactName = SmsPopupUtils.getPersonName(context, contactId, fromAddress);
    unreadCount = SmsPopupUtils.getUnreadMessagesCount(context, timestamp, messageBody);

    if (contactName == null) {
      contactName = context.getString(android.R.string.unknownName);
    }
  }

  /**
   * Construct SmsMmsMessage for getMmsDetails() - info fetched from the MMS
   * database table
   */
  public SmsMmsMessage(Context _context, String _contactId, String _contactName,
      String _fromAddress, String _messageBody, long _timestamp, long _messageId,
      long _threadId, int _unreadCount, int _messageType) {

    context = _context;
    fromAddress = _fromAddress;
    messageBody = _messageBody;
    timestamp = _timestamp;
    messageType = _messageType;

    // TODO: I think contactId can come the MMS table, this would save
    // this database lookup
    contactId = _contactId;
    contactName = _contactName;
    unreadCount = _unreadCount;
    threadId = _threadId;
    messageId = _messageId;

    if (contactName == null) {
      contactName = context.getString(android.R.string.unknownName);
    }
  }

  /**
   * Construct SmsMmsMessage for getSmsDetails() - info fetched from the SMS
   * database table
   */
  public SmsMmsMessage(Context _context, String _fromAddress, String _contactId,
      String _messageBody, long _timestamp, long _threadId,
      int _unreadCount, long _messageId, int _messageType) {
    context = _context;
    fromAddress = _fromAddress;
    messageBody = _messageBody;
    timestamp = _timestamp;
    messageType = _messageType;
    contactId = _contactId;

    if ("0".equals(contactId))
      contactId = null;

    contactName = SmsPopupUtils.getPersonName(context, contactId, fromAddress);
    //contactPhoto = SmsPopupUtils.getPersonPhoto(context, contactId);

    unreadCount = _unreadCount;
    threadId = _threadId;

    messageId = _messageId;

    if (contactName == null) {
      contactName = context.getString(android.R.string.unknownName);
    }
  }

  /**
   * Construct SmsMmsMessage from an extras bundle
   */
  public SmsMmsMessage(Context _context, Bundle b) {
    context = _context;
    fromAddress = b.getString(EXTRAS_FROM_ADDRESS);
    messageBody = b.getString(EXTRAS_MESSAGE_BODY);
    timestamp = b.getLong(EXTRAS_TIMESTAMP);
    contactId = b.getString(EXTRAS_CONTACT_ID);
    contactName = b.getString(EXTRAS_CONTACT_NAME);
    // contactPhoto = b.getByteArray(EXTRAS_CONTACT_PHOTO);
    unreadCount = b.getInt(EXTRAS_UNREAD_COUNT, 1);
    threadId = b.getLong(EXTRAS_THREAD_ID, 0);
    messageType = b.getInt(EXTRAS_MESSAGE_TYPE, MESSAGE_TYPE_SMS);
    notify = b.getBoolean(EXTRAS_NOTIFY, false);
    reminderCount = b.getInt(EXTRAS_REMINDER_COUNT, 0);
    messageId = b.getLong(EXTRAS_MESSAGE_ID, 0);
    fromEmailGateway = b.getBoolean(EXTRAS_EMAIL_GATEWAY, false);
  }

  /**
   * Construct SmsMmsMessage by specifying all data, only used for testing the
   * notification from the preferences screen
   */
  public SmsMmsMessage(Context _context, String _fromAddress, String _messageBody,
      long _timestamp, String _contactId, String _contactName, int _unreadCount,
      long _threadId, int _messageType) {
    context = _context;
    fromAddress = _fromAddress;
    messageBody = _messageBody;
    timestamp = _timestamp;
    contactId = _contactId;
    contactName = _contactName;
    // contactPhoto = _contactPhoto;
    unreadCount = _unreadCount;
    threadId = _threadId;
    messageType = _messageType;
  }

  /**
   * Convert all SmsMmsMessage data to an extras bundle to send via an intent
   */
  public Bundle toBundle() {
    Bundle b = new Bundle();
    b.putString(EXTRAS_FROM_ADDRESS, fromAddress);
    b.putString(EXTRAS_MESSAGE_BODY, messageBody);
    b.putLong(EXTRAS_TIMESTAMP, timestamp);
    b.putString(EXTRAS_CONTACT_ID, contactId);
    b.putString(EXTRAS_CONTACT_NAME, contactName);
    //b.putByteArray(EXTRAS_CONTACT_PHOTO, contactPhoto);
    b.putInt(EXTRAS_UNREAD_COUNT, unreadCount);
    b.putLong(EXTRAS_THREAD_ID, threadId);
    b.putInt(EXTRAS_MESSAGE_TYPE, messageType);
    b.putBoolean(EXTRAS_NOTIFY, notify);
    b.putInt(EXTRAS_REMINDER_COUNT, reminderCount);
    b.putLong(EXTRAS_MESSAGE_ID, messageId);
    b.putBoolean(EXTRAS_EMAIL_GATEWAY, fromEmailGateway);
    return b;
  }

  //  public Bitmap getContactPhoto() {
  //    if (contactPhoto == null)
  //      return null;
  //    return BitmapFactory.decodeStream(new ByteArrayInputStream(contactPhoto));
  //  }

  public Intent getPopupIntent() {
    Intent popup = new Intent(context, SmsPopupActivity.class);
    popup.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    popup.putExtras(toBundle());
    return popup;
  }

  public Intent getReplyIntent() {
    if (messageType == MESSAGE_TYPE_MMS) {
      locateThreadId();
      return SmsPopupUtils.getSmsToIntent(context, threadId);
    } else if (messageType == MESSAGE_TYPE_SMS) {
      return SmsPopupUtils.getSmsToIntent(context, fromAddress);
    }
    return null;
  }

  public void setThreadRead() {
    locateThreadId();
    SmsPopupUtils.setThreadRead(context, threadId);
  }

  public void setMessageRead() {
    locateMessageId();
    SmsPopupUtils.setMessageRead(context, messageId, messageType);
  }

  public void setUnreadCount(int _unreadCount) {
    unreadCount = _unreadCount;
  }

  public int getUnreadCount() {
    return unreadCount;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public CharSequence getFormattedTimestamp() {
    /*
     * No need for my own format function now, the 1.5 SDK has this built in
     * (this will detect the system settings and return the correct format)
     */
    // return SMSPopupUtils.formatTimestamp(context, timestamp);
    return DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_TIME);
  }

  public String getContactName() {
    if (contactName == null) {
      contactName = context.getString(android.R.string.unknownName);
    }
    return contactName;
  }

  public String getMessageBody() {
    if (messageBody == null) {
      messageBody = "";
    }
    return messageBody;
  }

  public int getMessageType() {
    return messageType;
  }

  public boolean getNotify() {
    if (Log.DEBUG) Log.v("getNotify() - notify is " + notify);
    return notify;
  }

  public int getReminderCount() {
    return reminderCount;
  }

  public void updateReminderCount(int count) {
    reminderCount = count;
  }

  public void incrementReminderCount() {
    reminderCount++;
  }

  public void delete() {
    SmsPopupUtils.deleteMessage(context, getMessageId(), threadId, messageType);
  }

  public void locateThreadId() {
    if (threadId == 0) {
      threadId = SmsPopupUtils.findThreadIdFromAddress(context, fromAddress);
    }
  }

  public long getThreadId() {
    locateThreadId();
    return threadId;
  }

  public void locateMessageId() {
    if (messageId == 0) {
      if (threadId == 0) {
        locateThreadId();
      }
      messageId =
        SmsPopupUtils.findMessageId(context, threadId, timestamp, messageBody, messageType);
    }
  }

  public long getMessageId() {
    locateMessageId();
    return messageId;
  }

  public String getContactId() {
    return contactId;
  }

  public String getAddress() {
    return fromAddress;
  }

  public boolean isEmail() {
    return fromEmailGateway;
  }

  //	public boolean equals(SmsMmsMessage compareMessage) {
  //		boolean equals = false;
  //		if (PhoneNumberUtils.compare(this.fromAddress, compareMessage.fromAddress) &&
  //				this.compareTimeStamp(compareMessage.timestamp) &&
  //				this.messageType == compareMessage.messageType) {
  //			equals = true;
  //		}
  //		return equals;
  //	}

  /**
   * Check if this message is sufficiently the same as the provided parameters
   */
  public boolean equals(String fromAddress, long timestamp, long timestamp_provider, String body) {
    boolean equals = false;

    if (PhoneNumberUtils.compare(this.fromAddress, fromAddress) &&
        this.compareTimeStamp(timestamp, timestamp_provider) &&
        this.compareBody(body)) {
      equals = true;
    }
    return equals;
  }

  //	private boolean compareTimeStamp(long compareTimestamp) {
  //		return compareTimeStamp(compareTimestamp, 0);
  //	}

  /*
   * Compares the timestamps of a message, this is super hacky because the way
   * which builds of Android store SMS timestamps changed in the cupcake branch -
   * pre-cupcake it stored the timestamp provided by the telecom provider;
   * post-cupcake it stored the system timestamp.
   * Unfortunately this means we need to use 2 different ways to determine if
   * the received message timestamp is sufficiently equal to the database timestamp :(
   */
  private boolean compareTimeStamp(long compareTimestamp, long providerTimestamp) {

    //		final int buildVersion = Integer.valueOf(Build.VERSION.INCREMENTAL);

    /*
     * On March 28th, 2009 - these are the latest builds that I could find:
     * 128600 TMI-RC9 (Tmobile EU)
     * 126986 PLAT-RC33 (Tmobile US)
     * 129975 Emulator (from Android 1.1 SDK r1)
     * Hopefully anything later will have the updated SMS code that uses the system
     * timestamp rather than the SMS timestamp
     */
    //		final int LATEST_BUILD = 129975;
    //		boolean PRE_CUPCAKE = false;
    //		if (buildVersion <= LATEST_BUILD) {
    //			PRE_CUPCAKE = true;
    //		}

    if (Log.DEBUG) Log.v("DB timestamp = " + timestamp);
    if (Log.DEBUG) Log.v("Provider timestamp = " + providerTimestamp);
    if (Log.DEBUG) Log.v("System timestamp = " + compareTimestamp);

    /*
     * If pre-cupcake we can just do a direct comparison as the Mms app stores the
     * timestamp from the telecom provider (in the sms pdu)
     */
    //		if (PRE_CUPCAKE) {
    //			Log.v("Build is pre-cupcake ("+buildVersion+"), doing direct SMS timestamp comparison");
    //			Log.v("DB timestamp = " + timestamp);
    //			Log.v("Intent timestamp = " + providerTimestamp);
    if (timestamp == providerTimestamp) {
      if (Log.DEBUG) Log.v("SMS Compare: compareTimestamp() - intent timestamp = provider timestamp");
      return true;
    } //else {
    //				return false;
    //			}
    //		}

    /*
     * If post-cupcake, the system app stores a system timestamp - the only problem is
     * we have no way of knowing the exact time the system app used.  So what
     * we'll do is compare against our own system timestamp and add a buffer in.
     * This is an awful way of doing this, but I don't see any other way around it :(
     */
    //		Log.v("Build is post-cupcake ("+buildVersion+"), doing approx. SMS timestamp comparison");
    //		Log.v("DB timestamp = " + timestamp);
    //		Log.v("Intent timestamp = " + compareTimestamp);

    if (timestamp < (compareTimestamp + MESSAGE_COMPARE_TIME_BUFFER)
        && timestamp > (compareTimestamp - MESSAGE_COMPARE_TIME_BUFFER)) {
      if (Log.DEBUG) Log.v("SMS Compare: compareTimestamp() - timestamp is approx. the same");
      return true;
    }
    if (Log.DEBUG) Log.v("SMS Compare: compareTimestamp() - return false");
    return false;
  }

  /*
   * Compare message body
   */
  private boolean compareBody(String compareBody) {
    if (compareBody != null) {
      if (messageBody.length() != compareBody.length()) {
        if (Log.DEBUG) Log.v("SMS Compare: compareBody() - length is different");
        return false;
      }

      if (messageBody.equals(compareBody)) {
        if (Log.DEBUG) Log.v("SMS Compare: compareBody() - messageBody is the same");
        return true;
      }
    }
    if (Log.DEBUG) Log.v("SMS Compare: compareBody() - return false");
    return false;
  }

  public boolean replyToMessage(String quickreply) {
    locateThreadId();
    SmsMessageSender sender =
      new SmsMessageSender(context, new String[] {fromAddress}, quickreply, threadId);
    return sender.sendMessage();
  }
}