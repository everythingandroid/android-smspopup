package net.everythingandroid.smspopup.preferences;

import net.everythingandroid.smspopup.ManageNotification;
import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.SmsMmsMessage;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
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

		String LOGTAG = "SMS Popup";
		
		AudioManager AM = 
			(AudioManager) c.getSystemService(Context.AUDIO_SERVICE);

		switch (AM.getMode()) {
			case AudioManager.MODE_NORMAL:
				Log.v(LOGTAG, "MODE_NORMAL"); break;
			case AudioManager.MODE_IN_CALL:
				Log.v(LOGTAG, "MODE_IN_CALL"); break;
			case AudioManager.MODE_RINGTONE:
				Log.v(LOGTAG, "MODE_RINGTONE"); break;
			default:
				Log.v(LOGTAG, "MODE is UNKNOWN"); break;
		}
		
		switch (AM.getRouting(AudioManager.MODE_NORMAL)) {
			case AudioManager.ROUTE_SPEAKER:
				Log.v(LOGTAG, "ROUTE_SPEAKER"); break;
			case AudioManager.ROUTE_BLUETOOTH:
				Log.v(LOGTAG, "ROUTE_BLUETOOTH"); break;
			case AudioManager.ROUTE_HEADSET:
				Log.v(LOGTAG, "ROUTE_HEADSET"); break;
			default:
				Log.v(LOGTAG, "ROUTE is UNKNOWN"); break;
		}
		
		//AM.shouldVibrate(vibrateType);
		//AM.setVibrateSetting(vibrateType, vibrateSetting)
		//AM.setvib
		
//		AM.loadSoundEffects();
//		AM.playSoundEffect(AudioManager.FX_KEY_CLICK);
//		
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		Log.v(LOGTAG, "---END---");
		
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