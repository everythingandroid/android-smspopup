package net.everythingandroid.smspopup.preferences;

import net.everythingandroid.smspopup.ManageNotification;
import net.everythingandroid.smspopup.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class CustomVibrateListPreference extends ListPreference {
	private Context context;
	private static boolean dialogShowing;
	private SharedPreferences myPrefs = null;
	private String vibrate_pattern;
	private String vibrate_pattern_custom; 
	
	public CustomVibrateListPreference(Context c) {
	   super(c);
	   context = c;
   }
	public CustomVibrateListPreference(Context c, AttributeSet attrs) {
	   super(c, attrs);
	   context = c;
   }
	
	@Override
   protected void onDialogClosed(boolean positiveResult) {
	   super.onDialogClosed(positiveResult);
	   
	   if (positiveResult) {	   
	   	getPrefs();
			if (context.getString(R.string.pref_custom_val).equals(vibrate_pattern)) {
				showDialog();
			}
		}
	}

	private void getPrefs() {
		if (myPrefs == null) {
			myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		}
		vibrate_pattern = myPrefs.getString(
				context.getString(R.string.pref_vibrate_pattern_key),
		      context.getString(R.string.pref_vibrate_pattern_default));
		vibrate_pattern_custom = myPrefs.getString(
				context.getString(R.string.pref_vibrate_pattern_custom_key),
				context.getString(R.string.pref_vibrate_pattern_default));
	}
	
	private void showDialog() {
		LayoutInflater inflater = (LayoutInflater) context
		      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View v = inflater.inflate(R.layout.vibratepatterndialog, null);

		final EditText et = (EditText) v.findViewById(R.id.CustomVibrateEditText);
		
		et.setText(vibrate_pattern_custom);

		new AlertDialog.Builder(context)
			.setIcon(android.R.drawable.ic_dialog_info)
			.setTitle(R.string.pref_vibrate_pattern_title)
			.setView(v)
			.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					dialogShowing = false;
				}
			})
			.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
		      public void onClick(DialogInterface dialog, int whichButton) {
		      	dialogShowing = false;
		      }
			})
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
		      public void onClick(DialogInterface dialog, int whichButton) {
			      SharedPreferences.Editor settings = myPrefs.edit();
			      String new_pattern = et.getText().toString();
			      dialogShowing = false;
			      if (ManageNotification.parseVibratePattern(et.getText().toString()) != null) {
				      settings.putString(
				      		context.getString(R.string.pref_vibrate_pattern_custom_key), 
				      		new_pattern);
				      
				      Toast.makeText(context,
				      		context.getString(R.string.pref_vibrate_pattern_ok),
				            Toast.LENGTH_LONG).show();
			      } else {
				      settings.putString(
				            context.getString(R.string.pref_vibrate_pattern_custom_key), 
				            context.getString(R.string.pref_vibrate_pattern_default));
				      Toast.makeText(context, 
				      		context.getString(R.string.pref_vibrate_pattern_bad),
				            Toast.LENGTH_LONG).show();
				}
				settings.commit();
			}})
			.show();
		dialogShowing = true;
	}

	@Override
   protected void onRestoreInstanceState(Parcelable state) {
		super.onRestoreInstanceState(state);
	   if (dialogShowing) {
	   	getPrefs();
			showDialog();
	   }	   
   }
	
	@Override
   protected View onCreateDialogView() {
		// Log.v("onCreateDialogView()");
		dialogShowing = false;
	   return super.onCreateDialogView();
   }
}