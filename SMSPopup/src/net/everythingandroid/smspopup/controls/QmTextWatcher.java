package net.everythingandroid.smspopup.controls;

import net.everythingandroid.smspopup.R;
import android.content.Context;
import android.telephony.gsm.SmsMessage;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

public class QmTextWatcher implements TextWatcher {
  private TextView mTextView;
  private Context mContext;
  private static String formatString1 = null;
  private static String formatString2 = null;

  public QmTextWatcher(Context context, TextView mUpdateTextView) {
    mTextView = mUpdateTextView;
    mContext = context;
  }

  public void afterTextChanged(Editable s) {
    mTextView.setText(getQuickReplyCounterText(mContext, s.toString()));
  }

  public void beforeTextChanged(CharSequence s, int start, int count, int after) {
  }

  public void onTextChanged(CharSequence s, int start, int before, int count) {
  }

  public static String getQuickReplyCounterText(Context context, String message) {
    if (formatString1 == null) {
      formatString1 = context.getString(R.string.message_counter);
    }

    if (formatString2 == null) {
      formatString2 = context.getString(R.string.message_counter_multiple);
    }

    return getQuickReplyCounterText(message, formatString1, formatString2);
  }

  public static String getQuickReplyCounterText(String message, String format1, String format2) {
    int[] messageLength = SmsMessage.calculateLength(message, true);

    if (messageLength[0] > 1) {
      return String.format(format2, messageLength[2], messageLength[0]);
    } else {
      return String.format(format1, messageLength[2]);
    }
  }

}
