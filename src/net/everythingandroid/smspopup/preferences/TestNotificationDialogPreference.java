package net.everythingandroid.smspopup.preferences;

import net.everythingandroid.smspopup.ManageNotification;
import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.SmsMmsMessage;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

public class TestNotificationDialogPreference extends DialogPreference {
	Context c;
	String version;
	
	public TestNotificationDialogPreference(Context context, AttributeSet attrs) {
	   super(context, attrs);
	   c = context;
   }
	
	public TestNotificationDialogPreference(Context context, AttributeSet attrs,
         int defStyle) {
	   super(context, attrs, defStyle);
	   c = context;
   }
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		ManageNotification.clear(c, ManageNotification.NOTIFICATION_TEST);
	}

	@Override
	protected View onCreateDialogView() {
		
		// Create a fake SmsMmsMessage
		String testPhone = "123-456-7890";
		SmsMmsMessage message = new SmsMmsMessage(c, testPhone,
		      c.getString(R.string.pref_notif_test_title), 0, null, testPhone, null, 1, 0,
		      SmsMmsMessage.MESSAGE_TYPE_SMS);
		
		// Show notification
		ManageNotification.show(c, message, ManageNotification.NOTIFICATION_TEST);
		
		return super.onCreateDialogView();
	}
}