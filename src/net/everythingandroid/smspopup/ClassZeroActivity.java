package net.everythingandroid.smspopup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.Window;

/**
 * Display a class-zero SMS message to the user. Wait for the user to dismiss
 * it.
 */
public class ClassZeroActivity extends Activity {
  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    // getWindow().setBackgroundDrawableResource(R.drawable.class_zero_background);

    CharSequence messageChars =
      getIntent().getCharSequenceExtra(SmsReceiverService.CLASS_ZERO_BODY_KEY);

    new AlertDialog.Builder(this)
    .setMessage(messageChars)
    .setPositiveButton(android.R.string.ok, mOkListener)
    .setCancelable(false).show();
  }

  private final OnClickListener mOkListener = new OnClickListener() {
    public void onClick(DialogInterface dialog, int whichButton) {
      ClassZeroActivity.this.finish();
    }
  };
}