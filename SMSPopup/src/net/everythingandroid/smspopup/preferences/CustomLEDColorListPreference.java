package net.everythingandroid.smspopup.preferences;

import net.everythingandroid.smspopup.ManagePreferences;
import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.SmsPopupDbAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class CustomLEDColorListPreference extends ListPreference implements OnSeekBarChangeListener {
  private Context context;
  private ManagePreferences mPrefs = null;
  private String contactId = null;
  private String led_color;
  private String led_color_custom;
  private SeekBar redSeekBar;
  private SeekBar greenSeekBar;
  private SeekBar blueSeekBar;

  private TextView redTV;
  private TextView greenTV;
  private TextView blueTV;
  private ImageView previewIV;

  public CustomLEDColorListPreference(Context c) {
    super(c);
    context = c;
  }

  public CustomLEDColorListPreference(Context c, AttributeSet attrs) {
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
      if (context.getString(R.string.pref_custom_val).equals(led_color)) {
        showDialog();
      }
    }
  }

  private void getPrefs() {
    if (mPrefs == null) {
      mPrefs = new ManagePreferences(context, contactId);
    }

    if (contactId == null) { // Default notifications
      led_color = mPrefs.getString(
          R.string.pref_flashled_color_key,
          R.string.pref_flashled_color_default);

      led_color_custom = mPrefs.getString(
          R.string.pref_flashled_color_custom_key,
          R.string.pref_flashled_color_default);

    } else { // Contact specific notifications
      led_color = mPrefs.getString(
          R.string.c_pref_flashled_color_key,
          R.string.pref_flashled_color_default,
          SmsPopupDbAdapter.KEY_LED_COLOR_NUM);

      led_color_custom = mPrefs.getString(
          R.string.c_pref_flashled_color_custom_key,
          R.string.pref_flashled_color_default,
          SmsPopupDbAdapter.KEY_LED_COLOR_CUSTOM_NUM);
    }

    if (mPrefs != null) {
      mPrefs.close();
      mPrefs = null;
    }
  }

  private void showDialog() {
    LayoutInflater inflater =
      (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    int color = Color.parseColor(context.getString(R.string.pref_flashled_color_default));
    if (led_color_custom != null) {
      try {
        color = Color.parseColor(led_color_custom);
      } catch (IllegalArgumentException e) {
        // No need to do anything here
      }
    }

    int red = Color.red(color);
    int green = Color.green(color);
    int blue = Color.blue(color);

    View v = inflater.inflate(R.layout.ledcolordialog, null);

    redSeekBar = (SeekBar) v.findViewById(R.id.RedSeekBar);
    greenSeekBar = (SeekBar) v.findViewById(R.id.GreenSeekBar);
    blueSeekBar = (SeekBar) v.findViewById(R.id.BlueSeekBar);

    redTV = (TextView) v.findViewById(R.id.RedTextView);
    greenTV = (TextView) v.findViewById(R.id.GreenTextView);
    blueTV = (TextView) v.findViewById(R.id.BlueTextView);

    previewIV = (ImageView) v.findViewById(R.id.PreviewImageView);

    redSeekBar.setProgress(red);
    greenSeekBar.setProgress(green);
    blueSeekBar.setProgress(blue);

    redSeekBar.setOnSeekBarChangeListener(this);
    greenSeekBar.setOnSeekBarChangeListener(this);
    blueSeekBar.setOnSeekBarChangeListener(this);

    updateSeekBarTextView(redSeekBar);
    updateSeekBarTextView(greenSeekBar);
    updateSeekBarTextView(blueSeekBar);

    updateColorImageView();

    new AlertDialog.Builder(context)
    .setIcon(android.R.drawable.ic_dialog_info)
    .setTitle(R.string.pref_flashled_color_title)
    .setView(v)
    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        int red = redSeekBar.getProgress();
        int green = greenSeekBar.getProgress();
        int blue = blueSeekBar.getProgress();
        int color = Color.rgb(red, green, blue);

        if (mPrefs == null) {
          mPrefs = new ManagePreferences(context, contactId);
        }

        if (contactId == null) { // Default notifications
          mPrefs.putString(
              R.string.pref_flashled_color_custom_key,
              "#" + Integer.toHexString(color),
              SmsPopupDbAdapter.KEY_LED_COLOR_CUSTOM);
        } else { // Contact specific notifications
          mPrefs.putString(
              R.string.c_pref_flashled_color_custom_key,
              "#" + Integer.toHexString(color),
              SmsPopupDbAdapter.KEY_LED_COLOR_CUSTOM);
        }

        if (mPrefs != null) {
          mPrefs.close();
          mPrefs = null;
        }

        Toast.makeText(context, R.string.pref_flashled_color_custom_set, Toast.LENGTH_LONG).show();
      }
    })
    .show();
  }

  public void onProgressChanged(SeekBar seekbar, int progress, boolean fromTouch) {
    updateSeekBarTextView(seekbar, progress);
    updateColorImageView();
  }

  private void updateSeekBarTextView(SeekBar seekbar) {
    updateSeekBarTextView(seekbar, seekbar.getProgress());
  }

  private void updateSeekBarTextView(SeekBar seekbar, int progress) {
    if (seekbar.equals(redSeekBar)) {
      redTV.setText(
          context.getString(R.string.pref_flashled_color_custom_dialog_red) + " " + progress);
    } else if (seekbar.equals(greenSeekBar)) {
      greenTV.setText(
          context.getString(R.string.pref_flashled_color_custom_dialog_green) + " " + progress);
    } else if (seekbar.equals(blueSeekBar)) {
      blueTV.setText(
          context.getString(R.string.pref_flashled_color_custom_dialog_blue) + " " + progress);
    }
  }

  private void updateColorImageView() {
    previewIV.setBackgroundColor(
        Color.rgb(
            redSeekBar.getProgress(),
            greenSeekBar.getProgress(),
            blueSeekBar.getProgress()));
  }

  public void onStartTrackingTouch(SeekBar seekBar) {}
  public void onStopTrackingTouch(SeekBar seekBar) {}

}
