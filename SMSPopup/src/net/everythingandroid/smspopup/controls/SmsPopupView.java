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
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class SmsPopupView extends LinearLayout {
    private SmsMmsMessage message;
    private Context mContext;
    private boolean messageViewed = false;

    protected OnReactToMessage mOnReactToMessage;

    private TextView fromTv;
    private TextView timestampTv;
    private TextView messageTv;
    private ViewFlipper contentFlipper;

    private QuickContactBadge contactBadge = null;
    private boolean fetchedContactPhoto = false;

    public static final int PRIVACY_MODE_OFF = 0;
    public static final int PRIVACY_MODE_HIDE_MESSAGE = 1;
    public static final int PRIVACY_MODE_HIDE_ALL = 2;

    private static final int VIEW_SMS = 0;
    private static final int VIEW_MMS = 1;
    private static final int VIEW_PRIVACY_SMS = 2;
    private static final int VIEW_PRIVACY_MMS = VIEW_MMS;

    private int privacyMode = PRIVACY_MODE_OFF;

    private static final int CONTACT_IMAGE_FADE_DURATION = 500;

    public SmsPopupView(Context context, SmsMmsMessage newMessage) {
        super(context);
        init(context, newMessage, PRIVACY_MODE_OFF);
    }

    public SmsPopupView(Context context, SmsMmsMessage newMessage, int initialPrivacyMode) {
        super(context);
        init(context, newMessage, initialPrivacyMode);
    }

    public SmsPopupView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setupLayout(mContext);
    }

    private void init(Context context, SmsMmsMessage newMessage, int initialPrivacyMode) {
        privacyMode = initialPrivacyMode;
        mContext = context;
        setupLayout(mContext);
        message = newMessage;
        populateViews(message);
    }

    public void setOnReactToMessage(OnReactToMessage r) {
        mOnReactToMessage = r;
    }

    private void setupLayout(Context context) {
        View.inflate(context, R.layout.message, this);

        // Find the main textviews and layouts
        fromTv = (TextView) findViewById(R.id.fromTextView);
        messageTv = (TextView) findViewById(R.id.messageTextView);
        timestampTv = (TextView) findViewById(R.id.timestampTextView);
        contentFlipper = (ViewFlipper) findViewById(R.id.contentFlipper);

        // Find the QuickContactBadge view that will show the contact photo
        contactBadge = (QuickContactBadge) findViewById(R.id.contactBadge);

        // The ViewMMS button
        final Button viewMmsButton = (Button) findViewById(R.id.viewMmsButton);
        viewMmsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnReactToMessage != null) {
                    mOnReactToMessage.onReplyToMessage(message);
                }
            }
        });

        // The view button (for privacy mode)
        final ImageButton viewButton = (ImageButton) findViewById(R.id.viewButton);
        viewButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnReactToMessage != null) {
                    mOnReactToMessage.onViewMessage(message);
                }
                setPrivacy(PRIVACY_MODE_OFF);
            }
        });
    }

    /*
     * Populate all the main SMS/MMS views with content from the actual SmsMmsMessage
     */
    private void populateViews(SmsMmsMessage message) {

        if (message.isSms()) {
            messageTv.setText(message.getMessageBody());
        }

        // Set the from, message and header views
        fromTv.setText(message.getContactName());
        timestampTv.setText(message.getFormattedTimestamp());

        setPrivacy(privacyMode);
    }

    // Set privacy using mode
    public void setPrivacy(int mode) {
        privacyMode = mode;

        final int viewPrivacy =
                message.isSms() ? VIEW_PRIVACY_SMS : VIEW_PRIVACY_MMS;

        final int viewPrivacyOff =
                message.isSms() ? VIEW_SMS : VIEW_MMS;
        
        final int currentView = contentFlipper.getDisplayedChild();

        if (privacyMode == PRIVACY_MODE_OFF) {

            if (currentView != viewPrivacyOff) {
                contentFlipper.setDisplayedChild(viewPrivacyOff);
            }
            fromTv.setVisibility(View.VISIBLE);
            messageViewed = true;
            loadContactPhoto();

        } else if (privacyMode == PRIVACY_MODE_HIDE_MESSAGE) {

            if (currentView != viewPrivacy) {
                contentFlipper.setDisplayedChild(viewPrivacy);
            }
            fromTv.setVisibility(View.VISIBLE);
            loadContactPhoto();

        } else if (privacyMode == PRIVACY_MODE_HIDE_ALL) {

            if (currentView != viewPrivacy) {
                contentFlipper.setDisplayedChild(viewPrivacy);
            }
            fromTv.setVisibility(View.GONE);
        }
    }

    // Set privacy from preference boolean values
    public void setPrivacy(boolean privacyMode, boolean privacySender) {
        if (privacyMode && privacySender) {
            setPrivacy(PRIVACY_MODE_HIDE_ALL);
        } else if (privacyMode && !privacySender) {
            setPrivacy(PRIVACY_MODE_HIDE_MESSAGE);
        } else {
            setPrivacy(PRIVACY_MODE_OFF);
        }
    }

    public void setMessageViewed(boolean viewed) {
        messageViewed = viewed;
    }

    public boolean getMessageViewed() {
        return messageViewed;
    }

    private void loadContactPhoto() {
        // Fetch contact photo in background
        // if (contactPhoto == null || contactPhoto.get() == null) {
        if (!fetchedContactPhoto) {
            new FetchContactPhotoTask().execute(message.getContactLookupUri());
            
            contactBadge.setClickable(true);
            final Uri contactUri = message.getContactLookupUri();
            if (contactUri != null) {
                contactBadge.assignContactUri(message.getContactLookupUri());
            } else {
                contactBadge.assignContactFromPhone(message.getAddress(), false);
            }            
        }        
    }

    /**
     * AsyncTask to fetch contact photo in background
     */
    private class FetchContactPhotoTask extends AsyncTask<Uri, Integer, Bitmap> {
        @Override
        protected Bitmap doInBackground(Uri... params) {
            if (Log.DEBUG)
                Log.v("Loading contact photo in background...");
            return SmsPopupUtils.getPersonPhoto(mContext, params[0]);
        }

        @Override
        protected void onPostExecute(Bitmap photo) {
            if (Log.DEBUG)
                Log.v("Done loading contact photo");
            if (photo != null) {
                fetchedContactPhoto = true;
                TransitionDrawable mTd =
                        new TransitionDrawable(new Drawable[] {
                                getResources().getDrawable(R.drawable.ic_contact_picture),
                                new BitmapDrawable(getResources(), photo) });
                contactBadge.setImageDrawable(mTd);
                mTd.setCrossFadeEnabled(true);
                mTd.startTransition(CONTACT_IMAGE_FADE_DURATION);
            }
        }
    }

    public static interface OnReactToMessage {
        abstract void onViewMessage(SmsMmsMessage message);

        abstract void onReplyToMessage(SmsMmsMessage message);
    }

}
