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

  public void afterTextChanged(Editable s) {}

  public void beforeTextChanged(CharSequence s, int start, int count, int after) {
  }

  public void onTextChanged(CharSequence s, int start, int before, int count) {
    mTextView.setText(getQuickReplyCounterText(mContext, s));
  }

  public static String getQuickReplyCounterText(Context context, CharSequence message) {
    if (formatString1 == null) {
      formatString1 = context.getString(R.string.message_counter);
    }

    if (formatString2 == null) {
      formatString2 = context.getString(R.string.message_counter_multiple);
    }

    return getQuickReplyCounterText(message, formatString1, formatString2);
  }

  public static String getQuickReplyCounterText(CharSequence message, String format1, String format2) {
    int[] params = SmsMessage.calculateLength(message, true);
    
    /* SmsMessage.calculateLength returns an int[4] with:
     *   int[0] being the number of SMS's required,
     *   int[1] the number of code units used,
     *   int[2] is the number of code units remaining until the next message.
     *   int[3] is the encoding type that should be used for the message.
     */    

    if (params[0] > 1) {
      return String.format(format2, params[2], params[0]);
    } else {
      return String.format(format1, params[2]);
    }
  }

}
