package net.everythingandroid.smspopup.preferences;

import net.everythingandroid.smspopup.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

public class ButtonListPreference extends ListPreference {
  public String prefId;
  private Context mContext;

  public ButtonListPreference(Context context) {
    super(context);
    mContext = context;
  }

  public ButtonListPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    mContext = context;
  }

  @Override
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    if (positiveResult) {
      refreshSummary();
    }
  }

  public void refreshSummary() {
    SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

    String buttonVal = myPrefs.getString(prefId, "");
    if ("".equals(buttonVal)) {
      setSummary("?");
    } else {
      String[] buttonEntries = mContext.getResources().getStringArray(R.array.pref_buttons_entries);
      setSummary(buttonEntries[Integer.parseInt(buttonVal)]);
    }

  }

}
