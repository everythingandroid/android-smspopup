package net.everythingandroid.smspopup.preferences;

import net.everythingandroid.smspopup.R;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class ButtonListPreference extends ListPreference {
  public static final int BUTTON_DISABLED = 0;
  public static final int BUTTON_CLOSE = 1;
  public static final int BUTTON_DELETE = 2;
  public static final int BUTTON_DELETE_NO_CONFIRM = 3;
  public static final int BUTTON_REPLY = 4;
  public static final int BUTTON_QUICKREPLY = 5;
  public static final int BUTTON_INBOX = 6;
  public static final int BUTTON_TTS = 7;
  public static final int BUTTON_REPLY_BY_ADDRESS = 8;

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
    setSummary(mContext.getString(R.string.pref_button_summary, getEntry()));
  }

  public boolean isReplyButton() {
    if (Integer.valueOf(getValue()) == BUTTON_REPLY
        || Integer.valueOf(getValue()) == BUTTON_QUICKREPLY) {
      return true;
    }
    return false;
  }
}
