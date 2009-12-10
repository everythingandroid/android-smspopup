package net.everythingandroid.smspopupdonate;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SmsPopupDonateActivity extends Activity {
  private static final String ACTION_SMSPOPUP_DONATED = "net.everythingandroid.smspopup.DONATED";
  private static final Uri PACKAGE_URI = Uri.parse("package:net.everythingandroid.smspopupdonate");
  private static final String[] AUTHOR_CONTACT_INFO_DONATE = { "Adam K <smspopup+donate@everythingandroid.net>" };

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    Button uninstallButton = (Button) findViewById(R.id.UninstallButton);
    uninstallButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Uri packageURI = PACKAGE_URI;
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
        startActivity(uninstallIntent);
      }
    });

    Button emailButton = (Button) findViewById(R.id.EmailButton);
    emailButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        launchEmailToIntent("");
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    disableDonatePreference();
  }

  private void disableDonatePreference() {
    if (Log.DEBUG) Log.v("Sending broadcast to SMS Popup");
    // Send a broadcast to switch off donate option in SMS Popup
    Intent i = new Intent(ACTION_SMSPOPUP_DONATED);
    i.setClassName("net.everythingandroid.smspopup", "net.everythingandroid.smspopup.ExternalEventReceiver");
    sendBroadcast(i);
  }

  public void launchEmailToIntent(String subject) {
    Intent msg = new Intent(Intent.ACTION_SEND);

    StringBuilder body = new StringBuilder();
    msg.putExtra(Intent.EXTRA_EMAIL, AUTHOR_CONTACT_INFO_DONATE);
    msg.putExtra(Intent.EXTRA_SUBJECT, "SMS Popup Donate");
    msg.putExtra(Intent.EXTRA_TEXT, body.toString());

    msg.setType("message/rfc822");
    startActivity(Intent.createChooser(msg, "Send E-mail"));
  }

}
