package net.everythingandroid.smspopup.preferences;

import net.everythingandroid.smspopup.Log;
import net.everythingandroid.smspopup.SMSReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;


public class AppEnabledCheckBoxPreference extends CheckBoxPreference {
	private Context context;
	
	public AppEnabledCheckBoxPreference(Context c, AttributeSet attrs,
			int defStyle) {
		super(c, attrs, defStyle);
		context = c;
	}

	public AppEnabledCheckBoxPreference(Context c, AttributeSet attrs) {
		super(c, attrs);
		context = c;
	}

	public AppEnabledCheckBoxPreference(Context c) {
		super(c);
		context = c;
	}

	@Override
	protected void onClick() {
		super.onClick();
		
		PackageManager pm = (PackageManager) context.getPackageManager();
		ComponentName cn = new ComponentName(context, SMSReceiver.class);

		if (isChecked()) {
			Log.v("SMSPopup receiver is enabled");
			pm.setComponentEnabledSetting(cn, 
					PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 
					PackageManager.DONT_KILL_APP);
		} else {
			Log.v("SMSPopup receiver is disabled");
			pm.setComponentEnabledSetting(cn, 
					PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 
					PackageManager.DONT_KILL_APP);
		}
	}
}