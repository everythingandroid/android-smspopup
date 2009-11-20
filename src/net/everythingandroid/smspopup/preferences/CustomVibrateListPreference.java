package net.everythingandroid.smspopup.preferences;

import net.everythingandroid.smspopup.ManageNotification;
import net.everythingandroid.smspopup.ManagePreferences;
import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.SmsPopupDbAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class CustomVibrateListPreference extends ListPreference {
  private Context context;
  private ManagePreferences mPrefs = null;
  private String contactId = null;
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

  public void setContactId(String _contactId) {
    contactId = _contactId;
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
    if (mPrefs == null) {
      mPrefs = new ManagePreferences(context, contactId);
    }

    if (contactId == null) { // Default notifications
      vibrate_pattern = mPrefs.getString(
          R.string.pref_vibrate_pattern_key,
          R.string.pref_vibrate_pattern_default);
      vibrate_pattern_custom = mPrefs.getString(
          R.string.pref_vibrate_pattern_custom_key,
          R.string.pref_vibrate_pattern_default);

    } else { // Contact specific notifications
      vibrate_pattern = mPrefs.getString(
          R.string.c_pref_vibrate_pattern_key,
          R.string.pref_vibrate_pattern_default,
          SmsPopupDbAdapter.KEY_VIBRATE_PATTERN_NUM);
      vibrate_pattern_custom = mPrefs.getString(
          R.string.c_pref_vibrate_pattern_custom_key,
          R.string.pref_vibrate_pattern_default,
          SmsPopupDbAdapter.KEY_VIBRATE_PATTERN_CUSTOM_NUM);
    }

    if (vibrate_pattern_custom == null) {
      vibrate_pattern_custom = mPrefs.getString(
          R.string.pref_vibrate_pattern_default,
          R.string.pref_vibrate_pattern_default);
    }

    if (mPrefs != null) {
      mPrefs.close();
      mPrefs = null;
    }
  }

  private void showDialog() {
    LayoutInflater inflater =
      (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    View v = inflater.inflate(R.layout.vibratepatterndialog, null);

    final EditText et = (EditText) v.findViewById(R.id.CustomVibrateEditText);

    et.setText(vibrate_pattern_custom);

    new AlertDialog.Builder(context)
    .setIcon(android.R.drawable.ic_dialog_info)
    .setTitle(R.string.pref_vibrate_pattern_title)
    .setView(v)
    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        String new_pattern = et.getText().toString();

        if (mPrefs == null) {
          mPrefs = new ManagePreferences(context, contactId);
        }

        if (ManageNotification.parseVibratePattern(et.getText().toString()) != null) {

          if (contactId == null) { // Default notifications
            mPrefs.putString(
                R.string.pref_vibrate_pattern_custom_key,
                new_pattern,
                SmsPopupDbAdapter.KEY_VIBRATE_PATTERN_CUSTOM);
          } else { // Contact specific notifications
            mPrefs.putString(
                R.string.c_pref_vibrate_pattern_custom_key,
                new_pattern,
                SmsPopupDbAdapter.KEY_VIBRATE_PATTERN_CUSTOM);
          }

          Toast.makeText(context, context.getString(R.string.pref_vibrate_pattern_ok),
              Toast.LENGTH_LONG).show();

        } else {

          /*
           * No need to store anything if the contact pattern is invalid (just leave it
           * as the last good value).
           */
          /*
          if (contactId == null) { // Default notifications
            mPrefs.putString(
                R.string.pref_vibrate_pattern_custom_key,
                context.getString(R.string.pref_vibrate_pattern_default),
                SmsPopupDbAdapter.KEY_VIBRATE_PATTERN_CUSTOM);
          } else { // Contact specific notifications
            mPrefs.putString(
                R.string.c_pref_vibrate_pattern_custom_key,
                context.getString(R.string.pref_vibrate_pattern_default),
                SmsPopupDbAdapter.KEY_VIBRATE_PATTERN_CUSTOM);
          }
           */

          Toast.makeText(context, context.getString(R.string.pref_vibrate_pattern_bad),
              Toast.LENGTH_LONG).show();
        }

        if (mPrefs != null) {
          mPrefs.close();
          mPrefs = null;
        }
      }
    })
    .show();
  }
}
