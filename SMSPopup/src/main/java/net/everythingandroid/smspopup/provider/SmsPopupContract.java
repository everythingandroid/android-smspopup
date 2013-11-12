package net.everythingandroid.smspopup.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import java.util.List;

public class SmsPopupContract {

    public static final String CONTENT_AUTHORITY = "net.everythingandroid.smspopup.provider";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    interface ContactNotificationsColumns {
        String CONTACT_ID = "contact_id";
        String CONTACT_LOOKUPKEY = "contact_lookupkey";
        String CONTACT_NAME = "contact_displayname";
        String ENABLED = "contact_enabled";
        String POPUP_ENABLED = "contact_popup_enabled";
        String RINGTONE = "contact_ringtone";
        String VIBRATE_ENABLED = "contact_vibrate_enabled";
        String VIBRATE_PATTERN = "contact_vibrate_pattern";
        String VIBRATE_PATTERN_CUSTOM = "contact_vibrate_pattern_custom";
        String LED_ENABLED = "contact_led_enabled";
        String LED_PATTERN = "contact_led_pattern";
        String LED_PATTERN_CUSTOM = "contact_led_pattern_custom";
        String LED_COLOR = "contact_led_color";
        String LED_COLOR_CUSTOM = "contact_led_color_custom";
        String SUMMARY = "contact_summary";
    }

    interface QuickMessagesColumns {
        String QUICKMESSAGE = "quickmessage_message";
        String ORDER = "quickmessage_order";
    }

    interface MessageColumns {
        String READ = "read";
        String TIMESTAMP = "timestamp";
        String ADDED = "time_added";
    }

    public static class ContactNotifications implements ContactNotificationsColumns, BaseColumns {
        public static final String PATH_CONTACTS = "contacts";
        public static final String PATH_CONTACTS_LOOKUP = "contactslookup";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_CONTACTS).build();
        public static final Uri CONTENT_LOOKUP_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_CONTACTS_LOOKUP).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.everythingandroid.contact";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.everythingandroid.contact";
        public static final String[] PROJECTION_SUMMARY =
                new String[] { _ID, CONTACT_NAME, SUMMARY };

        public static final String DEFAULT_SORT = CONTACT_NAME + ", " + _ID;

        public static Uri buildContactUri(String id) {
            return CONTENT_URI.buildUpon().appendPath(id).build();
        }

        public static Uri buildContactUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
        }

        public static String getContactId(Uri uri) {
            final int size = uri.getPathSegments().size();
            if (size >= 2 && size <= 3) {
                return uri.getLastPathSegment();
            }
            return null;
        }

        public static Uri buildLookupUri(String lookupKey) {
            return buildLookupUri(null, lookupKey);
        }

        public static Uri buildLookupUri(String contactId, String lookupKey) {
            if (lookupKey == null) {
                return null;
            }
            if (contactId == null) {
                return CONTENT_LOOKUP_URI.buildUpon().appendPath(lookupKey).build();
            }
            return CONTENT_LOOKUP_URI.buildUpon()
                    .appendPath(lookupKey).appendPath(contactId).build();
        }

        public static String getLookupKey(Uri uri) {
            final List<String> segments = uri.getPathSegments();
            if (segments.size() > 1) {
                // getPathSegments() decodes the segment, so we need to encode again as we want
                // to keep LOOKUP_URI in encoded format
                return Uri.encode(uri.getPathSegments().get(1));
            }
            return null;
        }
    }

    public static class QuickMessages implements QuickMessagesColumns, BaseColumns {
        public static final String PATH_QUICKMESSAGES = "quickmessages";
        public static final String PATH_QUICKMESSAGES_UPDATE_ORDER = "updateorder";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_QUICKMESSAGES).build();

        public static final Uri UPDATE_ORDER_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_QUICKMESSAGES)
                        .appendPath(PATH_QUICKMESSAGES_UPDATE_ORDER).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.everythingandroid.quickmessage";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.everythingandroid.quickmessage";

        public static final String DEFAULT_SORT = ORDER + ", " + _ID;

        public static Uri buildQuickMessageUri(String quickMessageId) {
            return CONTENT_URI.buildUpon().appendPath(quickMessageId).build();
        }

        public static String getQuickMessageId(Uri uri) {
        final List<String> segments = uri.getPathSegments();
            return segments.get(segments.size() - 1);
        }

        public static Uri buildQuickMessageOrderUpdateUri(String quickMessageId) {
            return UPDATE_ORDER_URI.buildUpon().appendPath(quickMessageId).build();
        }
    }

    public static class Messages implements MessageColumns, BaseColumns {
        public static final String PATH_MESSAGES = "messages";
        public static final String PATH_MESSAGE_READ = "read";
        public static final long STALE_READ_MESSAGES_TIME = 1000 * 60 * 60 * 24 * 2; // 2 days

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MESSAGES).build();

        public static final Uri MESSAGE_READ_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MESSAGES)
                        .appendPath(PATH_MESSAGE_READ).build();


        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.everythingandroid.messages";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.everythingandroid.message";

        public static final String DEFAULT_SORT = TIMESTAMP;

        public static Uri buildMessageUri(String messageId) {
            return CONTENT_URI.buildUpon().appendPath(messageId).build();
        }

        public static String getMessageId(Uri uri) {
            final List<String> segments = uri.getPathSegments();
            return segments.get(segments.size() - 1);
        }

        public static Uri buildMessageRead(String messageId) {
            return MESSAGE_READ_URI.buildUpon().appendPath(messageId).build();
        }
    }
}
