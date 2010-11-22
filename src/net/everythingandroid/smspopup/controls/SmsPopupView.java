package net.everythingandroid.smspopup.controls;

import net.everythingandroid.smspopup.ManagePreferences.Defaults;
import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.SmsMmsMessage;
import net.everythingandroid.smspopup.SmsPopupDbAdapter;
import net.everythingandroid.smspopup.SmsPopupUtils;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SmsPopupView extends LinearLayout {
  private SmsMmsMessage message;
  private SharedPreferences mPrefs;
  private Context context;

  private TextView fromTV;
  private TextView messageReceivedTV;
  private TextView messageTV;

  private TextView mmsSubjectTV = null;
  private ScrollView messageScrollView = null;
  private EditText qrEditText = null;
  private ProgressDialog mProgressDialog = null;

  private ImageView photoImageView = null;
  private Drawable contactPhotoPlaceholderDrawable = null;
  private Bitmap contactPhoto = null;
  private static int contactPhotoMargin = 3;
  private static int contactPhotoDefaultMargin = 10;

  private View unreadCountView = null;
  private TextView unreadCountTV = null;
  private View mmsView = null;
  private View privacyView = null;
  private View buttonsLL = null;
  private LinearLayout mainLL = null;

  private boolean wasVisible = false;
  private boolean replying = false;
  private boolean inbox = false;
  private boolean privacyMode = false;
  private boolean privacySender = false;
  private boolean privacyAlways = false;
  private boolean messageViewed = false;
  private String signatureText;
  private Uri contactLookupUri = null;


  public SmsPopupView(Context _context, SmsMmsMessage message) {
    super(_context);
    context = _context;
    setupViews(context);
    setSmsMmsMessage(message);
  }

  public SmsPopupView(Context _context, AttributeSet attrs) {
    super(_context, attrs);
    context = _context;
    setupViews(context);
  }

  private void setupViews(Context context) {
    View.inflate(context, R.layout.message, this);
  }

  public void setSmsMmsMessage(SmsMmsMessage _message) {
    message = _message;

    // Get shared prefs
    mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

    // Fetch privacy mode
    privacyMode =
      mPrefs.getBoolean(context.getString(R.string.pref_privacy_key), Defaults.PREFS_PRIVACY);
    privacySender =
        mPrefs.getBoolean(context.getString(R.string.pref_privacy_sender_key), Defaults.PREFS_PRIVACY_SENDER);
    privacyAlways =
        mPrefs.getBoolean(context.getString(R.string.pref_privacy_always_key), Defaults.PREFS_PRIVACY_ALWAYS);

    signatureText = mPrefs.getString(context.getString(R.string.pref_notif_signature_key), "");
    if (signatureText.length() > 0) signatureText = " " + signatureText;

    //resizeLayout();

    // Find the main textviews
    fromTV = (TextView) findViewById(R.id.FromTextView);
    messageTV = (TextView) findViewById(R.id.MessageTextView);
    messageReceivedTV = (TextView) findViewById(R.id.HeaderTextView);
    messageScrollView = (ScrollView) findViewById(R.id.MessageScrollView);

    // Find the ImageView that will show the contact photo
    photoImageView = (ImageView) findViewById(R.id.FromImageView);
    contactPhotoPlaceholderDrawable =
      getResources().getDrawable(SmsPopupUtils.CONTACT_PHOTO_PLACEHOLDER);

    // Enable long-press context menu
    registerForContextMenu(findViewById(R.id.MainLinearLayout));

    // Assign views
    unreadCountView = findViewById(R.id.UnreadCountLayout);
    mmsView = findViewById(R.id.MmsLinearLayout);
    privacyView = findViewById(R.id.ViewButtonLinearLayout);
    buttonsLL = findViewById(R.id.ButtonLinearLayout);

    mmsSubjectTV = (TextView) findViewById(R.id.MmsSubjectTextView);
    unreadCountTV = (TextView) findViewById(R.id.UnreadCountTextView);

    // The ViewMMS button
    Button viewMmsButton = (Button) mmsView.findViewById(R.id.ViewMmsButton);
    viewMmsButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        replyToMessage();
      }
    });

    // The view button (if in privacy mode)
    Button viewButton = (Button) privacyView.findViewById(R.id.ViewButton);
    viewButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        viewMessage();
      }
    });

    // See if user wants to show buttons on the popup
    if (!mPrefs.getBoolean(
        getString(R.string.pref_show_buttons_key), Defaults.PREFS_SHOW_BUTTONS)) {

      // Hide button layout
      buttonsLL.setVisibility(View.GONE);

    } else {

      // Button 1
      final Button button1 = (Button) findViewById(R.id.button1);
      PopupButton button1Vals =
        new PopupButton(getApplicationContext(), Integer.parseInt(mPrefs.getString(
            getString(R.string.pref_button1_key), Defaults.PREFS_BUTTON1)));
      button1.setOnClickListener(button1Vals);
      button1.setText(button1Vals.buttonText);
      button1.setVisibility(button1Vals.buttonVisibility);

      // Button 2
      final Button button2 = (Button) findViewById(R.id.button2);
      PopupButton button2Vals =
        new PopupButton(getApplicationContext(), Integer.parseInt(mPrefs.getString(
            getString(R.string.pref_button2_key), Defaults.PREFS_BUTTON2)));
      button2.setOnClickListener(button2Vals);
      button2.setText(button2Vals.buttonText);
      button2.setVisibility(button2Vals.buttonVisibility);

      // Button 3
      final Button button3 = (Button) findViewById(R.id.button3);
      PopupButton button3Vals =
        new PopupButton(getApplicationContext(), Integer.parseInt(mPrefs.getString(
            getString(R.string.pref_button3_key), Defaults.PREFS_BUTTON3)));
      button3.setOnClickListener(button3Vals);
      button3.setText(button3Vals.buttonText);
      button3.setVisibility(button3Vals.buttonVisibility);

      /*
       * This is really hacky. There are two types of reply buttons (quick reply
       * and reply). If the user has selected to show both the replies then the
       * text on the buttons should be different. If they only use one then the
       * text can just be "Reply".
       */
      int numReplyButtons = 0;
      if (button1Vals.isReplyButton) numReplyButtons++;
      if (button2Vals.isReplyButton) numReplyButtons++;
      if (button3Vals.isReplyButton) numReplyButtons++;

      if (numReplyButtons == 1) {
        if (button1Vals.isReplyButton) button1.setText(R.string.button_reply);
        if (button2Vals.isReplyButton) button2.setText(R.string.button_reply);
        if (button3Vals.isReplyButton) button3.setText(R.string.button_reply);
      }
    }

    if (bundle == null) {
      contactPhoto = null;
      populateViews(getIntent().getExtras());
    } else { // this activity was recreated after being destroyed (ie. on orientation change)
      populateViews(bundle);
    }

    mDbAdapter = new SmsPopupDbAdapter(getApplicationContext());

    wakeApp();

    // Eula.show(this);


  }

}
