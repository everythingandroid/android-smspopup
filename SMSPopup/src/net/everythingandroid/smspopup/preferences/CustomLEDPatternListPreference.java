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

public class CustomLEDPatternListPreference extends ListPreference {
  private Context context;
  private ManagePreferences mPrefs = null;
  private String contactId = null;
  private String flashLedPattern;
  private String flashLedPatternCustom;
  private int[] led_pattern;

  public CustomLEDPatternListPreference(Context c) {
    super(c);
    context = c;
  }

  public CustomLEDPatternListPreference(Context c, AttributeSet attrs) {
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
      if (context.getString(R.string.pref_custom_val).equals(flashLedPattern)) {
        showDialog();
      }
    }
  }

  private void getPrefs() {
    if (mPrefs == null) {
      mPrefs = new ManagePreferences(context, contactId);
    }

    if (contactId == null) { // Default notifications
      flashLedPattern = mPrefs.getString(
          R.string.pref_flashled_pattern_key,
          R.string.pref_flashled_pattern_default);
      flashLedPatternCustom = mPrefs.getString(
          R.string.pref_flashled_pattern_custom_key,
          R.string.pref_flashled_pattern_default);
    } else { // Contact specific notifications
      flashLedPattern = mPrefs.getString(
          R.string.c_pref_flashled_pattern_key,
          R.string.pref_flashled_pattern_default,
          SmsPopupDbAdapter.KEY_LED_PATTERN_NUM);
      flashLedPatternCustom = mPrefs.getString(
          R.string.c_pref_flashled_pattern_custom_key,
          R.string.pref_flashled_pattern_default,
          SmsPopupDbAdapter.KEY_LED_PATTERN_CUSTOM_NUM);
    }

    led_pattern = null;

    if (context.getString(R.string.pref_custom_val).equals(flashLedPattern)) {
      led_pattern = ManageNotification.parseLEDPattern(flashLedPatternCustom);
    } else {
      led_pattern = ManageNotification.parseLEDPattern(flashLedPattern);
    }

    if (led_pattern == null) {
      led_pattern = ManageNotification.parseLEDPattern(
          mPrefs.getString(
              R.string.pref_flashled_pattern_default,
              R.string.pref_flashled_pattern_default));
    }

    if (mPrefs != null) {
      mPrefs.close();
      mPrefs = null;
    }
  }

  private void showDialog() {
    LayoutInflater inflater =
      (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    View v = inflater.inflate(R.layout.ledpatterndialog, null);

    final EditText onEditText = (EditText) v.findViewById(R.id.LEDOnEditText);
    final EditText offEditText = (EditText) v.findViewById(R.id.LEDOffEditText);

    onEditText.setText(String.valueOf(led_pattern[0]));
    offEditText.setText(String.valueOf(led_pattern[1]));

    new AlertDialog.Builder(context)
    .setIcon(android.R.drawable.ic_dialog_info)
    .setTitle(R.string.pref_flashled_pattern_title)
    .setView(v)
    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        String stringPattern = onEditText.getText() + "," + offEditText.getText();

        if (mPrefs == null) {
          mPrefs = new ManagePreferences(context, contactId);
        }

        if (ManageNotification.parseLEDPattern(stringPattern) != null) {

          if (contactId == null) { // Default notifications
            mPrefs.putString(
                R.string.pref_flashled_pattern_custom_key,
                stringPattern,
                SmsPopupDbAdapter.KEY_LED_PATTERN_CUSTOM);

          } else { // Contact specific notifications
            mPrefs.putString(
                R.string.c_pref_flashled_pattern_custom_key,
                stringPattern,
                SmsPopupDbAdapter.KEY_LED_PATTERN_CUSTOM);
          }

          Toast.makeText(context, context.getString(R.string.pref_flashled_pattern_ok),
              Toast.LENGTH_LONG).show();

        } else {

          /*
           * No need to store anything if the led pattern is invalid (just leave it
           * as the last good value).
           */
          /*
          if (contactId == null) { // Default notifications
            mPrefs.putString(
                R.string.pref_flashled_pattern_custom_key,
                context.getString(R.string.pref_flashled_pattern_default),
                SmsPopupDbAdapter.KEY_LED_PATTERN_CUSTOM);
          } else { // Contact specific notifications
            mPrefs.putString(
                R.string.c_pref_flashled_pattern_custom_key,
                context.getString(R.string.pref_flashled_pattern_default),
                SmsPopupDbAdapter.KEY_LED_PATTERN_CUSTOM);
          }
           */

          Toast.makeText(context, context.getString(R.string.pref_flashled_pattern_bad),
              Toast.LENGTH_LONG).show();
        }

        if (mPrefs != null) {
          mPrefs.close();
          mPrefs = null;
        }
      }
    }).show();
  }
}