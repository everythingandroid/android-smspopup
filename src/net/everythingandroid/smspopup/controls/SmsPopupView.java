package net.everythingandroid.smspopup.controls;

import net.everythingandroid.smspopup.Log;
import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.SmsMmsMessage;
import net.everythingandroid.smspopup.SmsPopupUtils;
import net.everythingandroid.smspopup.wrappers.ContactWrapper;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SmsPopupView extends LinearLayout {
  private SmsMmsMessage message;
  private Context context;
  private boolean messagePrivacy = false;
  private boolean messageViewed = false;

  protected OnReactToMessage mOnReactToMessage;

  private TextView fromTV;
  private TextView messageReceivedTV;
  private TextView messageTV;

  private TextView mmsSubjectTV = null;
  private ScrollView messageScrollView = null;

  private ImageView photoImageView = null;
  private Drawable contactPhotoPlaceholderDrawable = null;
  private Bitmap contactPhoto = null;
  private static int contactPhotoMargin = 3;
  private static int contactPhotoDefaultMargin = 10;

  private View mmsLL = null;
  private View privacyLL = null;
  private LinearLayout mainLL = null;

  private boolean wasVisible = false;
  private boolean replying = false;
  private boolean inbox = false;
  private boolean privacyMode = false;
  private boolean privacySender = false;
  private boolean privacyAlways = false;
  // private boolean messageViewed = false;
  private String signatureText;
  private Uri contactLookupUri = null;


  public SmsPopupView(Context _context, SmsMmsMessage message) {
    super(_context);
    context = _context;
    setupLayout(context);
    setSmsMmsMessage(message);
  }

  public SmsPopupView(Context _context, AttributeSet attrs) {
    super(_context, attrs);
    context = _context;
    setupLayout(context);
  }

  public void setOnReactToMessage(OnReactToMessage r) {
    mOnReactToMessage = r;
  }

  public void setPrivacy(boolean mode) {
    messagePrivacy = mode;
  }

  public void setMessageViewed(boolean viewed) {
    messageViewed = viewed;
  }

  public void setSmsMmsMessage(SmsMmsMessage newMessage) {

    // Check if new message sender, if not, we can re-use the same contact photo
    if (message != null && newMessage != null) {
      if (!TextUtils.equals(message.getFromAddress(), newMessage.getFromAddress())) {
        contactPhoto = null;
      }
    }

    message = newMessage;
    populateViews(message);
  }

  private void setupLayout(Context context) {
    View.inflate(context, R.layout.message, this);

    // Find the main textviews and layouts
    fromTV = (TextView) findViewById(R.id.FromTextView);
    messageTV = (TextView) findViewById(R.id.MessageTextView);
    messageReceivedTV = (TextView) findViewById(R.id.HeaderTextView);
    messageScrollView = (ScrollView) findViewById(R.id.MessageScrollView);
    mmsLL = findViewById(R.id.MmsLinearLayout);
    privacyLL = findViewById(R.id.ViewButtonLinearLayout);
    mmsSubjectTV = (TextView) findViewById(R.id.MmsSubjectTextView);

    // Find the ImageView that will show the contact photo
    photoImageView = (ImageView) findViewById(R.id.FromImageView);
    contactPhotoPlaceholderDrawable =
        getResources().getDrawable(SmsPopupUtils.CONTACT_PHOTO_PLACEHOLDER);

    // The ViewMMS button
    Button viewMmsButton = (Button) mmsLL.findViewById(R.id.ViewMmsButton);
    viewMmsButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        mOnReactToMessage.replyToMessage();
      }
    });

    // The view button (if in privacy mode)
    Button viewButton = (Button) privacyLL.findViewById(R.id.ViewButton);
    viewButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        mOnReactToMessage.viewMessage();
      }
    });
  }

  /*
   * Populate all the main SMS/MMS views with content from the actual SmsMmsMessage
   */
  private void populateViews(SmsMmsMessage message) {

    // Fetch contact photo in background
    if (contactPhoto == null) {
      setContactPhotoToDefault(photoImageView);
      new FetchContactPhotoTask().execute(message.getContactId());
      addQuickContactOnClick();
    }
//    } else {
//      setContactPhoto(photoImageView, contactPhoto);
//    }


    // If it's a MMS message, just show the MMS layout
    if (message.getMessageType() == SmsMmsMessage.MESSAGE_TYPE_MMS) {

      messageScrollView.setVisibility(View.GONE);
      mmsLL.setVisibility(View.VISIBLE);

      // If no MMS subject, hide the subject text view
      if (TextUtils.isEmpty(message.getMessageBody())) {
        mmsSubjectTV.setVisibility(View.GONE);
      } else {
        mmsSubjectTV.setVisibility(View.VISIBLE);
      }
    } else {

      // Otherwise hide MMS layout
      mmsLL.setVisibility(View.GONE);

      // Refresh privacy settings (hide/show message) depending on privacy
      // setting
      refreshPrivacy(false);
    }

    // Update TextView that contains the timestamp for the incoming message
    String headerText =
        context.getString(R.string.new_text_at, message.getFormattedTimestamp().toString());

    // Set the from, message and header views
    fromTV.setText(message.getContactName());
    if (message.getMessageType() == SmsMmsMessage.MESSAGE_TYPE_SMS) {
      messageTV.setText(message.getMessageBody());
    } else {
      mmsSubjectTV.setText(
          context.getString(R.string.mms_subject) + " " + message.getMessageBody());
    }
    messageReceivedTV.setText(headerText);
  }

  /*
   * This handles hiding and showing various views depending on the privacy
   * settings of the app and the current state of the phone (keyguard on or off)
   */
  final private void refreshPrivacy(boolean forceView) {

    if (Log.DEBUG) Log.v("refreshPrivacy(): " + forceView);

    if (message.getMessageType() != SmsMmsMessage.MESSAGE_TYPE_SMS) return;

    if (privacyMode) {

      // // if message has been already shown, disable privacy mode
      // if (messageViewed == true) {
      // forceView = true;
      // }
      //
      // // We need to init the keyguard class so we can check if the keyguard
      // is on
      // ManageKeyguard.initialize(getApplicationContext());
      //
      // if ((ManageKeyguard.inKeyguardRestrictedInputMode()
      // || privacyAlways == true) && forceView == false) {
      //
      // messageViewed = false;
      //
      // // set to privacy mode
      // if (Log.DEBUG) Log.v("refreshPrivacy(): set to privacy mode.");
      // messageScrollView.setVisibility(View.GONE);
      //
      // if (privacySender) {
      // fromTV.setVisibility(View.GONE);
      // }
      //
      // } else {
      //
      // // set public mode
      // if (Log.DEBUG) Log.v("refreshPrivacy(): set to public mode.");
      //
      // // Fetch contact photo
      // new FetchContactPhotoTask().execute(message.getContactId());
      //
      // // Add quick contact onClick to contact imageview
      // addQuickContactOnClick(true);
      //
      // if (privacyView != null) {
      // privacyView.setVisibility(View.GONE);
      // }
      //
      // messageScrollView.setVisibility(View.VISIBLE);
      // fromTV.setVisibility(View.VISIBLE);
      // messageViewed = true;
      // }

    } else {

      // set public mode
      if (Log.DEBUG) Log.v("refreshPrivacy(): set to public mode.");

      privacyLL.setVisibility(View.GONE);
      messageScrollView.setVisibility(View.VISIBLE);
      fromTV.setVisibility(View.VISIBLE);
      messageViewed = true;
    }

  }

  /*
   * Sets contact photo to a default placeholder image
   */
  private void setContactPhotoToDefault(ImageView photoImageView) {

    // Reset background and padding
    photoImageView.setBackgroundResource(0);
    photoImageView.setPadding(0, 0, 0, 0);

    // Set margins for placeholder image
    MarginLayoutParams mLP = (MarginLayoutParams) photoImageView.getLayoutParams();
    final int scaledMargin =
        (int) (contactPhotoDefaultMargin * this.getResources().getDisplayMetrics().density);

    mLP.setMargins(scaledMargin, scaledMargin, scaledMargin, scaledMargin);
    photoImageView.setLayoutParams(mLP);

    // Set placeholder image
    photoImageView.setImageDrawable(contactPhotoPlaceholderDrawable);
  }

  /*
   * Sets contact photo to the target imageview
   */
  private void setContactPhoto(ImageView photoImageView, Bitmap contactPhoto) {

    if (contactPhoto == null) {
      setContactPhotoToDefault(photoImageView);
      return;
    }

    // Update background and padding
    if (SmsPopupUtils.PRE_ECLAIR) {
      photoImageView.setBackgroundResource(android.R.drawable.picture_frame);
    } else {
      photoImageView.setBackgroundResource(R.drawable.quickcontact_badge_small);
    }

    // Set margins for image
    MarginLayoutParams mLP = (MarginLayoutParams) photoImageView.getLayoutParams();
    final int scaledMargin =
        (int) (contactPhotoMargin * this.getResources().getDisplayMetrics().density);
    mLP.setMargins(scaledMargin, scaledMargin, scaledMargin, scaledMargin);
    photoImageView.setLayoutParams(mLP);

    // Set contact photo image
    photoImageView.setImageBitmap(contactPhoto);
  }

  /**
   * AsyncTask to fetch contact photo in background
   */
  private class FetchContactPhotoTask extends AsyncTask<String, Integer, Bitmap> {
    @Override
    protected Bitmap doInBackground(String... params) {
      if (Log.DEBUG) Log.v("Loading contact photo in background...");
      // try { Thread.sleep(2000); } catch (InterruptedException e) {}
      return SmsPopupUtils.getPersonPhoto(context, params[0]);
    }

    @Override
    protected void onPostExecute(Bitmap result) {
      if (Log.DEBUG) Log.v("Done loading contact photo");
      contactPhoto = result;
      if (result != null) {
        setContactPhoto(photoImageView, contactPhoto);
      }
    }
  }

  // Show QuickContact card on photo imageview click (only available on eclair+)
  private void addQuickContactOnClick() {
    addQuickContactOnClick(false);
  }

  private void addQuickContactOnClick(boolean force) {
    if (!SmsPopupUtils.PRE_ECLAIR && ((!privacyMode && !privacySender) || force)) {

      contactLookupUri = null;
      String contactId = message.getContactId();
      if (contactId != null) {
        contactLookupUri =
            ContactWrapper.getLookupUri(Long.valueOf(contactId), message.getContactLookupKey());
      }

      photoImageView.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          if (contactLookupUri != null) {
            ContactWrapper.showQuickContact(context, v, contactLookupUri,
                ContactWrapper.QUICKCONTACT_MODE_MEDIUM, null);
          }
        }
      });
    }
  }

  public static interface OnReactToMessage {
    abstract void viewMessage();

    abstract void replyToMessage();
  }

}
