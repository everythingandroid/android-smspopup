package net.everythingandroid.smspopup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Contacts;
import android.provider.Settings;
import android.provider.Contacts.PeopleColumns;
import android.provider.Contacts.PhotosColumns;
import android.telephony.PhoneNumberUtils;
import android.telephony.gsm.SmsMessage;
import android.text.TextUtils;

public class SMSPopupUtils {
	//Content URIs for SMS app, these may change in future SDK
	public static final Uri MMS_SMS_CONTENT_URI = Uri.parse("content://mms-sms/");
	public static final Uri THREAD_ID_CONTENT_URI =
			Uri.withAppendedPath(MMS_SMS_CONTENT_URI, "threadID");
	public static final Uri CONVERSATION_CONTENT_URI =
			Uri.withAppendedPath(MMS_SMS_CONTENT_URI, "conversations");
	public static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
	// public static final Uri SMS_INBOX_CONTENT_URI =
	// Uri.parse("content://sms/inbox");
	public static final Uri SMS_INBOX_CONTENT_URI = Uri.withAppendedPath(SMS_CONTENT_URI, "inbox");
	public static final Uri MMS_CONTENT_URI = Uri.parse("content://mms");
	public static final Uri MMS_INBOX_CONTENT_URI = Uri.withAppendedPath(MMS_CONTENT_URI, "inbox");
	
	public static final String SMS_ID = "_id";
	public static final String SMS_TO_URI = "smsto:/";
	public static final String SMS_MIME_TYPE = "vnd.android-dir/mms-sms";
	public static final String SMS_ADDRESS_EXTRA = "address";
	public static final int READ_THREAD = 1;
	public static final int MESSAGE_TYPE_SMS = 1;
	public static final int MESSAGE_TYPE_MMS = 2;
	
	private static final String TIME_FORMAT_12_HOUR = "h:mm a";
	private static final String TIME_FORMAT_24_HOUR = "H:mm";
	private static final String AUTHOR_CONTACT_INFO = "Adam K <adam@everythingandroid.net>";

	
	public static String getPersonName(Context context, String id, String address) {
		if (id == null) {
			if (address != null) {
				Log.v("Contact not found, formatting number");
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
					Log.v("Contact Display Name: " + name);
					return name;
				}
			} finally {
				cursor.close();
			}
		}
		
		if (address != null) {
			Log.v("Contact not found, formatting number");
			return PhoneNumberUtils.formatNumber(address);
		}
		return null;
	}

	public static String getPersonIdFromPhoneNumber(Context context,
	      String address) {
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
					Log.v("Found person: " + id);
					return (String.valueOf(id));
				}
			} finally {
				cursor.close();
			}
		}
		return null;
	}
	
	public static byte[] getPersonPhoto(Context context, String id) {
		if (id == null)
			return null;
		byte photo[] = null;

		Cursor cursor = context.getContentResolver().query(
		      Uri.withAppendedPath(Contacts.Photos.CONTENT_URI, id),
		      new String[] { PhotosColumns.DATA }, null, null, null);
		if (cursor != null) {
			try {
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();
					photo = cursor.getBlob(0);
					if (photo != null) {
						return photo;
						// Log.v("Found photo for person: " + id);
						// bitmap = BitmapFactory.decodeStream(new
						// ByteArrayInputStream(photo));
					}
				}
			} finally {
				cursor.close();
			}
		}
		return photo;
	}
	
	public static long getThreadIdFromAddress(Context context, String address) {
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
				Log.v("error marking thread read");
			}
			Log.v("thread id " + threadId + " marked as read, result = " + result);
		}
	}
	
	public static void deleteMessage(Context context, long threadId, long _timestamp, int messageType) {
		int id = 0;
		long timestamp = _timestamp;
		if (threadId > 0) {
			
			// It seems MMS timestamps are stored in a seconds, whereas SMS
			// is in millis
			if (SmsMmsMessage.MESSAGE_TYPE_MMS == messageType) {
				timestamp = _timestamp / 1000;
				Log.v("deleteMessage() - adjusted timestmap for MMS (" + _timestamp + " -> " + timestamp + ")");
			}
			
			ContentResolver myCR = context.getContentResolver();
			Log.v("trying to find message to delete: thread_id = " + threadId + ", date = " + timestamp);
			Cursor cursor =
				myCR.query(
							ContentUris.withAppendedId(CONVERSATION_CONTENT_URI, threadId),
							new String[] { "_id", "date", "thread_id" },
							"thread_id=" + threadId + " and " + "date=" + timestamp, null, "date desc");
			if (cursor != null) {
//				Log.v("cursor was not null");
				try {
					if (cursor.moveToFirst()) {
						// for (int i = 0; i < cursor.getColumnNames().length; i++) {
						// Log.v("Column: " + cursor.getColumnNames()[i]);
						//							
						// }
						// Log.v("_id = " + cursor.getInt(0));
						// Log.v("date = " + cursor.getLong(1) + " (timestamp = " +
						// timestamp + ")");
						// Log.v("threadId = " + cursor.getInt(2));
						id = cursor.getInt(0);
					}
				} finally {
					cursor.close();
				}
			}
			
			if (id > 0) {
				Log.v("id of message to delete is " + id);
				Uri deleteUri;

				if (SmsMmsMessage.MESSAGE_TYPE_MMS == messageType) {
					deleteUri = Uri.withAppendedPath(MMS_CONTENT_URI, String.valueOf(id));
				} else if (SmsMmsMessage.MESSAGE_TYPE_SMS == messageType) {
					deleteUri = Uri.withAppendedPath(SMS_CONTENT_URI, String.valueOf(id));
				} else {
					return;
				}
				int count = myCR.delete(deleteUri, null, null);
				Log.v("Messages deleted: " + count);
			}
		}
	}
	
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

	public static Intent getSmsToIntentFromThreadId(Context context, long threadId) {
		Intent popup = new Intent(Intent.ACTION_VIEW);
		// should I be using FLAG_ACTIVITY_RESET_TASK_IF_NEEDED??
		int flags =
			Intent.FLAG_ACTIVITY_NEW_TASK |
			Intent.FLAG_ACTIVITY_SINGLE_TOP |
			Intent.FLAG_ACTIVITY_CLEAR_TOP;
		popup.setFlags(flags);
		if (threadId > 0) {
			Log.v("^^Found threadId (" + threadId + "), sending to Sms intent");
			popup.setData(Uri.withAppendedPath(THREAD_ID_CONTENT_URI, String.valueOf(threadId)));
		} else {
			return getSmsIntent();
		}
		return popup;
	}
	
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
			AudioManager myAM = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
			myAM.getStreamVolume(AudioManager.STREAM_RING);
		}
		
		msg.putExtra(Intent.EXTRA_EMAIL, recipients);  
		msg.putExtra(Intent.EXTRA_SUBJECT, subject);
		msg.putExtra(Intent.EXTRA_TEXT, body);  
		  
		msg.setType("message/rfc822");
		context.startActivity(Intent.createChooser(msg, "Send E-mail"));		
	}
	
	public static int getUnreadMessagesCount(Context context) {
		int unreadSms = getUnreadSmsCount(context);
		int unreadMms = getUnreadMmsCount(context);
		return (unreadSms + unreadMms);
	}
	
	public static int getUnreadSmsCount(Context context) {
		
		String SMS_READ_COLUMN = "read";
		String UNREAD_CONDITION = SMS_READ_COLUMN + "=0";
		
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
		Log.v("sms unread count = " + count);
		return count;
	}
	
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
		Log.v("mms unread count = " + count);
		return count;
	}
	
	
	public static class MmsMessage {
		public String subject;
		public long thread_id;
		public long timestamp;
		public int charset;
		public String address;
		public int contact_id;

		public MmsMessage(String _address, long _thread_id,
		      long _timestamp,
		      String _subject, int _charset) {
			address = _address;
			// contact_id = _contact_id;
			thread_id = _thread_id;
			timestamp = _timestamp;
			subject = _subject;
			charset = _charset;
		}
	}
	
	public static SmsMmsMessage getMmsDetails(Context context) {
		String MMS_READ_COLUMN = "read";
		String UNREAD_CONDITION = MMS_READ_COLUMN + "=0";
		String SORT_ORDER = "date DESC";

		Cursor cursor = context.getContentResolver().query(
				MMS_INBOX_CONTENT_URI,
				//new String[] { "m_id", "\"from\"", "sub", "d_tm", "thread_id" },
		      new String[] { "_id", "thread_id", "date", "sub", "sub_cs" },
				UNREAD_CONDITION, null,
				SORT_ORDER);

		if (cursor != null) {
			try {
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();
					// String[] columns = cursor.getColumnNames();
					// for (int i=0; i<columns.length; i++) {
					// Log.v("columns " + i + ": " + columns[i] + ": "
					// + cursor.getString(i));
					// }
					// Log.v("MMS from: " + cursor.getString(1));
					// Log.v("MMS subject: " + cursor.getString(2));
					// Log.v("MMS thread_id: " + cursor.getInt(4));
					String address = getMmsFrom(context, cursor.getLong(0));
					long threadId = cursor.getLong(1);
					long timestamp = cursor.getLong(2) * 1000;
					String subject = cursor.getString(3);
					// int charset = cursor.getInt(4);
					// MmsMessage message = new MmsMessage(address, thread_id,
					// timestamp, subject, charset);
					// return message;
					SmsMmsMessage mmsMessage = new SmsMmsMessage(context, address, subject, timestamp,
					      threadId, SmsMmsMessage.MESSAGE_TYPE_MMS);

					return mmsMessage;

				}
			} finally {
				cursor.close();
			}
		}		
		return null;
	}

	public static String getMmsFrom(Context context, long message_id) {
		
		String message_id_string = String.valueOf(message_id);
		
		Uri.Builder builder = MMS_CONTENT_URI.buildUpon();
		builder.appendPath(message_id_string).appendPath("addr");

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
	
	public static String formatTimestamp(Context context, long timestamp) {
		String HOURS_24 = "24";
		String hours;
		hours = Settings.System.getString(context.getContentResolver(), Settings.System.TIME_12_24);
		
		SimpleDateFormat mSDF = new SimpleDateFormat();
		if (HOURS_24.equals(hours)) {
			mSDF.applyLocalizedPattern(TIME_FORMAT_24_HOUR);
		} else {
			mSDF.applyLocalizedPattern(TIME_FORMAT_12_HOUR);
		}
		return mSDF.format(new Date(timestamp));
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
}