package net.everythingandroid.smspopup.preferences;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class ButtonListPreference extends ListPreference {
  public String prefId;

  public ButtonListPreference(Context context) {
    super(context);
  }

  public ButtonListPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    if (positiveResult) {
      refreshSummary();
    }
  }

  public void refreshSummary() {
    setSummary(getEntry());
  }
}
