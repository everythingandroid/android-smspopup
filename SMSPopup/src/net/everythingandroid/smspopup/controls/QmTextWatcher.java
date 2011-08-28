package net.everythingandroid.smspopup.controls;

import android.content.Context;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class QmTextWatcher implements TextWatcher {
  private TextView mTextView;
  private Button mSendButton;
  private static final int CHARS_REMAINING_BEFORE_COUNTER_SHOWN = 30;

  public QmTextWatcher(Context context, TextView updateTextView, Button sendButton) {
    mTextView = updateTextView;
    mSendButton = sendButton;
  }

  public QmTextWatcher(Context context, TextView updateTextView) {
    mTextView = updateTextView;
    mSendButton = null;
  }

  public void afterTextChanged(Editable s) {}

  public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

  public void onTextChanged(CharSequence s, int start, int before, int count) {
    getQuickReplyCounterText(s, mTextView, mSendButton);
  }

  public static void getQuickReplyCounterText(CharSequence s, TextView mTextView, Button mSendButton) {
    if (mSendButton != null) {
      if (s.length() > 0) {
        mSendButton.setEnabled(true);
      } else {
        mSendButton.setEnabled(false);
      }
    }

    if (s.length() < (80 - CHARS_REMAINING_BEFORE_COUNTER_SHOWN)) {
      mTextView.setVisibility(View.GONE);
      return;
    }

    /*
     * SmsMessage.calculateLength returns an int[4] with: int[0] being the
     * number of SMS's required, int[1] the number of code units used, int[2] is
     * the number of code units remaining until the next message. int[3] is the
     * encoding type that should be used for the message.
     */
    int[] params = SmsMessage.calculateLength(s, false);
    int msgCount = params[0];
    int remainingInCurrentMessage = params[2];

    if (msgCount > 1 || remainingInCurrentMessage <= CHARS_REMAINING_BEFORE_COUNTER_SHOWN) {
      mTextView.setText(remainingInCurrentMessage + " / " + msgCount);
      mTextView.setVisibility(View.VISIBLE);
    } else {
      mTextView.setVisibility(View.GONE);
    }
  }
}