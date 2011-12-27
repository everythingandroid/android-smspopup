package net.everythingandroid.smspopup.provider;

import java.util.List;

import android.net.Uri;
import android.provider.BaseColumns;

public class SmsPopupContract {

    public static final String CONTENT_AUTHORITY = "net.everythingandroid.smspopup.provider";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_CONTACTS = "contacts";
    public static final String PATH_CONTACTS_LOOKUP = "contactslookup";
    public static final String PATH_QUICKMESSAGES = "quickmessages";

    interface ContactNotificationsColumns {
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

    public static class ContactNotifications implements ContactNotificationsColumns, BaseColumns {
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

        public static Uri buildContactUri(String id) {
            return CONTENT_URI.buildUpon().appendPath(id).build();
        }
        
        public static Uri buildContactUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();
        }        

        public static String getContactId(Uri uri) {
            if (uri.getPathSegments().size() == 2) {
                return uri.getLastPathSegment();
            }
            return null;
        }
        
        public static Uri buildLookupUri(String lookupKey) {
            return buildLookupUri(null, lookupKey);
        }

        public static Uri buildLookupUri(String contactId, String lookupKey) {
            if (lookupKey == null) {
                return CONTENT_LOOKUP_URI.buildUpon().appendPath(contactId).build();
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
                return uri.getPathSegments().get(1);
            }
            return null;
        }

    }

    public static class QuickMessages implements QuickMessagesColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_QUICKMESSAGES).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.everythingandroid.quickmessage";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.everythingandroid.quickmessage";

        public static Uri buildQuickMessageUri(String quickMessageId) {
            return CONTENT_URI.buildUpon().appendPath(quickMessageId).build();
        }

        public static String getQuickMessageId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

    }

}
