package net.everythingandroid.smspopup.controls;

import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.provider.SmsMmsMessage;
import net.everythingandroid.smspopup.util.Log;
import net.everythingandroid.smspopup.util.SmsPopupUtils;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.ScrollView;
import android.widget.TextView;

public class SmsPopupView extends LinearLayout {
    private SmsMmsMessage message;
    private Context context;
    private boolean messageViewed = false;

    protected OnReactToMessage mOnReactToMessage;

    private TextView fromTV;
    private TextView timestampTV;
    private TextView messageTV;

    private TextView mmsSubjectTV = null;
    private ScrollView messageScrollView = null;

    private QuickContactBadge contactBadge = null;
    private Bitmap contactPhoto = null;

    private View mmsLayout = null;
    private View privacyLayout = null;

    public static final int PRIVACY_MODE_OFF = 0;
    public static final int PRIVACY_MODE_HIDE_MESSAGE = 1;
    public static final int PRIVACY_MODE_HIDE_ALL = 2;
    private static int privacyMode = PRIVACY_MODE_OFF;

    public SmsPopupView(Context _context, SmsMmsMessage newMessage) {
        super(_context);
        context = _context;
        setupLayout(context);
        message = newMessage;
        populateViews(message);
    }

    public SmsPopupView(Context _context, AttributeSet attrs) {
        super(_context, attrs);
        context = _context;
        setupLayout(context);
    }

    public void setOnReactToMessage(OnReactToMessage r) {
        mOnReactToMessage = r;
    }

    // Set privacy using mode
    public static void setPrivacy(int mode) {
        privacyMode = mode;
    }

    // Set privacy from preference boolean values
    public static void setPrivacy(boolean privacyMode, boolean privacySender) {
        setPrivacy(SmsPopupView.PRIVACY_MODE_OFF);
        if (privacyMode) {
            if (privacySender) {
                setPrivacy(SmsPopupView.PRIVACY_MODE_HIDE_ALL);
            } else {
                setPrivacy(SmsPopupView.PRIVACY_MODE_HIDE_MESSAGE);
            }
        }
    }

    private void refreshPrivacy(int privacyMode) {

        if (privacyMode == PRIVACY_MODE_OFF) {
            // set public mode
            if (Log.DEBUG)
                Log.v("refreshPrivacy(): set to public mode.");

            privacyLayout.setVisibility(View.GONE);
            messageScrollView.setVisibility(View.VISIBLE);
            fromTV.setVisibility(View.VISIBLE);
            // messageViewed = true;
            loadContactPhoto();

        } else {

            privacyLayout.setVisibility(View.VISIBLE);
            messageScrollView.setVisibility(View.GONE);

            if (privacyMode == PRIVACY_MODE_HIDE_ALL) {
                fromTV.setVisibility(View.GONE);
            } else {
                loadContactPhoto();
            }
        }
    }

    public void refreshPrivacy() {
        refreshPrivacy(false);
    }

    /*
     * This handles hiding and showing various views depending on the user privacy settings
     */
    private void refreshPrivacy(boolean forceView) {

        if (Log.DEBUG) Log.v("refreshPrivacy(): " + forceView);

        if (privacyMode == PRIVACY_MODE_OFF || forceView) {

            // set public mode
            if (Log.DEBUG) Log.v("refreshPrivacy(): set to public mode.");

            privacyLayout.setVisibility(View.GONE);
            messageScrollView.setVisibility(View.VISIBLE);
            fromTV.setVisibility(View.VISIBLE);
            messageViewed = true;
            loadContactPhoto();
            contactBadge.setClickable(true);
            final Uri contactUri = message.getContactLookupUri();
            if (contactUri != null) {
                contactBadge.assignContactUri(message.getContactLookupUri());
            } else {
                contactBadge.assignContactFromPhone(message.getAddress(), false);
            }

        } else {

            privacyLayout.setVisibility(View.VISIBLE);
            messageScrollView.setVisibility(View.GONE);

            if (privacyMode == PRIVACY_MODE_HIDE_ALL) {
                fromTV.setVisibility(View.GONE);
            } else {
                loadContactPhoto();
            }

            // // if message has been already shown, disable privacy mode
            // if (messageViewed == true) {
            // forceView = true;
            // }
            //
            // // We need to init the keyguard class so we can check if the
            // keyguard
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
        }

    }

    public void setMessageViewed(boolean viewed) {
        messageViewed = viewed;
    }

    public boolean getMessageViewed() {
        return messageViewed;
    }

    private void setupLayout(Context context) {
        View.inflate(context, R.layout.message, this);

        // Find the main textviews and layouts
        fromTV = (TextView) findViewById(R.id.FromTextView);
        messageTV = (TextView) findViewById(R.id.MessageTextView);
        timestampTV = (TextView) findViewById(R.id.TimestampTextView);
        messageScrollView = (ScrollView) findViewById(R.id.MessageScrollView);
        mmsLayout = findViewById(R.id.MmsLinearLayout);
        privacyLayout = findViewById(R.id.ViewButtonLinearLayout);
        mmsSubjectTV = (TextView) findViewById(R.id.MmsSubjectTextView);

        // Find the QuickContactBadge view that will show the contact photo
        contactBadge = (QuickContactBadge) findViewById(R.id.ContactBadge);

        // The ViewMMS button
        Button viewMmsButton = (Button) mmsLayout.findViewById(R.id.ViewMmsButton);
        viewMmsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnReactToMessage.onReplyToMessage();
            }
        });

        // The view button (if in privacy mode)
        Button viewButton = (Button) privacyLayout.findViewById(R.id.ViewButton);
        viewButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnReactToMessage.onViewMessage();
            }
        });
    }

    /*
     * Populate all the main SMS/MMS views with content from the actual SmsMmsMessage
     */
    private void populateViews(SmsMmsMessage message) {

        // Refresh privacy settings (hide/show message) depending on privacy
        // setting
        refreshPrivacy(false);

        // If it's a MMS message, just show the MMS layout
        if (message.getMessageType() == SmsMmsMessage.MESSAGE_TYPE_MMS) {

            messageScrollView.setVisibility(View.GONE);
            mmsLayout.setVisibility(View.VISIBLE);

            // If no MMS subject, hide the subject text view
            if (TextUtils.isEmpty(message.getMessageBody())) {
                mmsSubjectTV.setVisibility(View.GONE);
            } else {
                mmsSubjectTV.setVisibility(View.VISIBLE);
            }

        } else {

            // Otherwise hide MMS layout
            mmsLayout.setVisibility(View.GONE);

        }

        // Set the from, message and header views
        fromTV.setText(message.getContactName());
        if (message.getMessageType() == SmsMmsMessage.MESSAGE_TYPE_SMS) {
            messageTV.setText(message.getMessageBody());
        } else {
            mmsSubjectTV.setText(context.getString(R.string.mms_subject) + " "
                    + message.getMessageBody());
        }
        timestampTV.setText(message.getFormattedTimestamp());
    }

    private void loadContactPhoto() {
        // Fetch contact photo in background
        if (contactPhoto == null) {
            new FetchContactPhotoTask().execute(message.getContactLookupUri());
        }
    }

    /**
     * AsyncTask to fetch contact photo in background
     */
    private class FetchContactPhotoTask extends AsyncTask<Uri, Integer, Bitmap> {
        @Override
        protected Bitmap doInBackground(Uri... params) {
            if (Log.DEBUG) Log.v("Loading contact photo in background...");
            return SmsPopupUtils.getPersonPhoto(context, params[0]);
        }

        @Override
        protected void onPostExecute(Bitmap photo) {
            if (Log.DEBUG) Log.v("Done loading contact photo");
            if (photo != null) {
                contactPhoto = photo;
                TransitionDrawable mTd = new TransitionDrawable(new Drawable[] {
                        getResources().getDrawable(R.drawable.ic_contact_picture),
                        new BitmapDrawable(getResources(), contactPhoto)
                });
                contactBadge.setImageDrawable(mTd);
                mTd.setCrossFadeEnabled(true);                
                mTd.startTransition(500);
            }
        }
    }

    public static interface OnReactToMessage {
        abstract void onViewMessage();

        abstract void onReplyToMessage();
    }

}
