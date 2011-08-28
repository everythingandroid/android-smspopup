package net.everythingandroid.smspopup.preferences;

import net.everythingandroid.smspopup.Log;
import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;

public class QuickReplyCheckBoxPreference extends CheckBoxPreference {

  public QuickReplyCheckBoxPreference(Context context) {
    super(context);
  }

  public QuickReplyCheckBoxPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public QuickReplyCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected boolean callChangeListener(Object newValue) {
    return super.callChangeListener(newValue);
  }

  public void refresh(String val1, String val2, String val3) {
    Log.v(val1 + ", " + val2 + ", " + val3);
    if (Integer.valueOf(val1) == ButtonListPreference.BUTTON_QUICKREPLY
        || Integer.valueOf(val2) == ButtonListPreference.BUTTON_QUICKREPLY
        || Integer.valueOf(val3) == ButtonListPreference.BUTTON_QUICKREPLY) {
      Log.v("Quick Reply enabled");
      setChecked(true);
    }
    setChecked(false);
  }
}