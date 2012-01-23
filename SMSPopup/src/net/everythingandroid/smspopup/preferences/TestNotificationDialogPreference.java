package net.everythingandroid.smspopup.preferences;

import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.provider.SmsMmsMessage;
import net.everythingandroid.smspopup.provider.SmsPopupContract.ContactNotifications;
import net.everythingandroid.smspopup.util.ManageNotification;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

public class TestNotificationDialogPreference extends DialogPreference {
    private Context context;
    private String contactId = null;

    public TestNotificationDialogPreference(Context _context, AttributeSet attrs) {
        super(_context, attrs);
        context = _context;
    }

    public TestNotificationDialogPreference(Context _context, AttributeSet attrs, int defStyle) {
        super(_context, attrs, defStyle);
        context = _context;
    }

    public void setContactId(String _contactId) {
        contactId = _contactId;
    }

    public void setContactId(long _contactId) {
        contactId = String.valueOf(_contactId);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        ManageNotification.clear(context, ManageNotification.NOTIFICATION_TEST);
    }

    @Override
    protected View onCreateDialogView() {

        // Create a test SmsMmsMessage
        String testPhone = "123-456-7890";

        // If contactId is set, use it's phone else just use a default.
        if (contactId != null) {
            // Cursor contactCursor = mDbAdapter.fetchContact(Long.valueOf(contactId));
            Cursor contactCursor =
                    context.getContentResolver()
                            .query(ContactNotifications.buildContactUri(contactId), null, null,
                                    null, null);
            if (contactCursor != null && contactCursor.moveToFirst()) {
                testPhone =
                        contactCursor.getString(contactCursor
                                .getColumnIndexOrThrow(ContactNotifications.CONTACT_NAME));
                contactCursor.close();
            }
        }

        SmsMmsMessage message =
                new SmsMmsMessage(context, testPhone, context
                        .getString(R.string.pref_notif_test_title),
                        0, contactId, null, testPhone, 1, 0, SmsMmsMessage.MESSAGE_TYPE_SMS);

        // Show notification
        ManageNotification.show(context, message, 1, ManageNotification.NOTIFICATION_TEST);

        return super.onCreateDialogView();
    }

}
