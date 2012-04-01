package net.everythingandroid.smspopup.controls;

import java.lang.ref.WeakReference;

import net.everythingandroid.smspopup.BuildConfig;
import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.preferences.ButtonListPreference;
import net.everythingandroid.smspopup.provider.SmsMmsMessage;
import net.everythingandroid.smspopup.util.Log;
import net.everythingandroid.smspopup.util.SmsPopupUtils;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class SmsPopupFragment extends Fragment {
    private SmsMmsMessage message;
    private boolean messageViewed = false;

    private SmsPopupButtonsListener mButtonsListener;

    private TextView fromTv;
    private TextView timestampTv;
    private TextView messageTv;
    private ViewFlipper contentFlipper;

    private QuickContactBadge contactBadge = null;

    public static final int PRIVACY_MODE_OFF = 0;
    public static final int PRIVACY_MODE_HIDE_MESSAGE = 1;
    public static final int PRIVACY_MODE_HIDE_ALL = 2;
    
    private static final String EXTRA_PRIVACY_MODE = "net.everythingandroid.smspopup.privacy_mode";
    private static final String EXTRA_BUTTONS = "net.everythingandroid.smspopup.buttons";

    private static final int VIEW_SMS = 0;
    private static final int VIEW_MMS = 1;
    private static final int VIEW_PRIVACY_SMS = 2;
    private static final int VIEW_PRIVACY_MMS = VIEW_MMS;
    
    public static final int BUTTON_VIEW = 100;
    public static final int BUTTON_VIEW_MMS = 101;
    public static final int BUTTON_UNLOCK = 102;
    
    public int privacyMode = PRIVACY_MODE_OFF;

    private static final int CONTACT_IMAGE_FADE_DURATION = 300;
    
    public static SmsPopupFragment newInstance(SmsMmsMessage newMessage, int[] buttons, 
    		int privacyMode) {
    	
    	SmsPopupFragment newFragment = new SmsPopupFragment();
    	Bundle args = newMessage.toBundle();
    	args.putInt(EXTRA_PRIVACY_MODE, privacyMode);
    	args.putIntArray(EXTRA_BUTTONS, buttons);
    	newFragment.setArguments(args);
    	return newFragment;
    }
    
    public static SmsPopupFragment newInstance(SmsMmsMessage newMessage, int[] buttons) {
    	return newInstance(newMessage, buttons, PRIVACY_MODE_OFF);
    }
    
    public SmsPopupFragment() {};

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
    		Bundle savedInstanceState) {
    	
        privacyMode = getArguments().getInt(EXTRA_PRIVACY_MODE);
        final int[] buttons = getArguments().getIntArray(EXTRA_BUTTONS);
        message = new SmsMmsMessage(getActivity(), getArguments());
    	    	
    	View v = inflater.inflate(R.layout.popup_message_fragment, container, false);

        // Find the main textviews and layouts
        fromTv = (TextView) v.findViewById(R.id.fromTextView);
        messageTv = (TextView) v.findViewById(R.id.messageTextView);
        timestampTv = (TextView) v.findViewById(R.id.timestampTextView);
        contentFlipper = (ViewFlipper) v.findViewById(R.id.contentFlipper);

        // Find the QuickContactBadge view that will show the contact photo
        contactBadge = (QuickContactBadge) v.findViewById(R.id.contactBadge);

        final String[] buttonText = getResources().getStringArray(R.array.buttons_text);
        
        final Button button1 = (Button) v.findViewById(R.id.button1);
        final PopupButton button1Vals = new PopupButton(buttons[0], buttonText);
        button1.setOnClickListener(button1Vals);
        button1.setVisibility(button1Vals.buttonVisibility);
        button1.setText(button1Vals.buttonText);

        final Button button2 = (Button) v.findViewById(R.id.button2);
        final PopupButton button2Vals = new PopupButton(buttons[1], buttonText);
        button2.setOnClickListener(button2Vals);
        button2.setVisibility(button2Vals.buttonVisibility);
        button2.setText(button2Vals.buttonText);
        
        final Button button3 = (Button) v.findViewById(R.id.button3);
        final PopupButton button3Vals = new PopupButton(buttons[2], buttonText);
        button3.setOnClickListener(button3Vals);
        button3.setVisibility(button3Vals.buttonVisibility);
        button3.setText(button3Vals.buttonText);
        
        /*
         * This is really hacky. There are two types of reply buttons (quick reply and reply).
         * If the user has selected to show both the replies then the text on the buttons 
         * should be different. If they only use one then the text can just be "Reply".
         */
        int numReplyButtons = 0;
        if (button1Vals.isReplyButton)
            numReplyButtons++;
        if (button2Vals.isReplyButton)
            numReplyButtons++;
        if (button3Vals.isReplyButton)
            numReplyButtons++;

        if (numReplyButtons == 1) {
            if (button1Vals.isReplyButton)
                button1.setText(R.string.button_reply);
            if (button2Vals.isReplyButton)
                button2.setText(R.string.button_reply);
            if (button3Vals.isReplyButton)
                button3.setText(R.string.button_reply);
        }        

        // The ViewMMS button
        ((Button) v.findViewById(R.id.viewMmsButton)).setOnClickListener(
        		new PopupButton(BUTTON_VIEW_MMS, buttonText));
        
        ((Button) v.findViewById(R.id.unlockButton)).setOnClickListener(
        		new PopupButton(BUTTON_UNLOCK, buttonText));

        ((ImageButton) v.findViewById(R.id.viewButton)).setOnClickListener(
        		new OnClickListener() {
        			@Override
        			public void onClick(View v) {
        				setPrivacy(PRIVACY_MODE_OFF);
        				mButtonsListener.onButtonClicked(BUTTON_VIEW);
        			}
				});

//        // The view button (for privacy mode)
//        final ImageButton viewButton = (ImageButton) v.findViewById(R.id.viewButton);
//        viewButton.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (mOnReactToMessage != null) {
//                    mOnReactToMessage.onViewMessage(message);
//                }
//                setPrivacy(PRIVACY_MODE_OFF);
//            }
//        });
        
        populateViews();
        
        return v;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
        	mButtonsListener = (SmsPopupButtonsListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() 
            		+ " must implement SmsPopupButtonsListener");
        }
    }

    /*
     * Populate all the main SMS/MMS views with content from the actual SmsMmsMessage
     */
    private void populateViews() {
        if (message.isSms()) {
            messageTv.setText(message.getMessageBody());
        }

        // Set the from, message and header views
        fromTv.setText(message.getContactName());
        timestampTv.setText(message.getFormattedTimestamp());

        setPrivacy(privacyMode, true);
    }
    
    public void setPrivacy(int newMode) {
    	setPrivacy(newMode, false);
    }
    
    public void setPrivacy(int newMode, boolean initial) {
    	
    	if ((newMode != privacyMode || initial) && message != null && contentFlipper != null) {
	        final int viewPrivacy =
	                message.isSms() ? VIEW_PRIVACY_SMS : VIEW_PRIVACY_MMS;
	
	        final int viewPrivacyOff =
	                message.isSms() ? VIEW_SMS : VIEW_MMS;
	        
	        final int currentView = contentFlipper.getDisplayedChild();
	
	        if (newMode == PRIVACY_MODE_OFF) {
	
	            if (currentView != viewPrivacyOff) {
	                contentFlipper.setDisplayedChild(viewPrivacyOff);
	            }
	            fromTv.setVisibility(View.VISIBLE);
	            messageViewed = true;
	            
	            if (initial || privacyMode == PRIVACY_MODE_HIDE_ALL) {
	            	loadContactPhoto();
	            }
	
	        } else if (newMode == PRIVACY_MODE_HIDE_MESSAGE) {
	
	            if (currentView != viewPrivacy) {
	                contentFlipper.setDisplayedChild(viewPrivacy);
	            }
	            fromTv.setVisibility(View.VISIBLE);
	            loadContactPhoto();
	
	        } else if (newMode == PRIVACY_MODE_HIDE_ALL) {
	
	            if (currentView != viewPrivacy) {
	                contentFlipper.setDisplayedChild(viewPrivacy);
	            }
	            fromTv.setVisibility(View.GONE);
	        }
    	}
    	privacyMode = newMode;
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
    	boolean cacheHit = false;
    	if (mButtonsListener != null && message.getContactLookupUri() != null) {
    		final LruCache<Uri, Bitmap> cache = mButtonsListener.getCache();
    		if (cache != null) {
    			final Bitmap bitmap = cache.get(message.getContactLookupUri());
    			if (bitmap != null) {
    				if (BuildConfig.DEBUG) Log.v("loadContactPhoto() - bitmap cache hit");
    				contactBadge.setImageBitmap(bitmap);
    				cacheHit = true;
    			}
    		}
    	}
    	
    	if (!cacheHit) {
    		new FetchContactPhotoTask(contactBadge).execute(message.getContactLookupUri());
    	}
        
        contactBadge.setClickable(true);
        final Uri contactUri = message.getContactLookupUri();
        if (contactUri != null) {
            contactBadge.assignContactUri(message.getContactLookupUri());
        } else {
            contactBadge.assignContactFromPhone(message.getAddress(), false);
        }            
    }

    /**
     * AsyncTask to fetch contact photo in background
     */
    private class FetchContactPhotoTask extends AsyncTask<Uri, Integer, Bitmap> {
    	private final WeakReference<QuickContactBadge> viewReference;
    	
    	public FetchContactPhotoTask(QuickContactBadge badge) {
    		viewReference = new WeakReference<QuickContactBadge> (badge);
    	}
    	
        @Override
        protected Bitmap doInBackground(Uri... params) {
            if (BuildConfig.DEBUG)
                Log.v("Loading contact photo in background...");
            final Bitmap bitmap = SmsPopupUtils.getPersonPhoto(getActivity(), params[0]);
        	if (mButtonsListener != null && bitmap != null) {
        		final LruCache<Uri, Bitmap> cache = mButtonsListener.getCache();
        		if (cache != null) {
        			cache.put(params[0], bitmap);
        		}
        	}
            
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap photo) {
            if (BuildConfig.DEBUG)
                Log.v("Done loading contact photo");
            if (photo != null && viewReference != null) {
            	final QuickContactBadge badge = viewReference.get(); 
            	if (badge != null && isAdded()) {
	                TransitionDrawable mTd =
	                        new TransitionDrawable(new Drawable[] {
	                                getResources().getDrawable(R.drawable.ic_contact_picture),
	                                new BitmapDrawable(getResources(), photo) });
	                badge.setImageDrawable(mTd);
	                mTd.setCrossFadeEnabled(false);
	                mTd.startTransition(CONTACT_IMAGE_FADE_DURATION);
            	}
            }
        }
    }
    
    private class PopupButton implements OnClickListener {
        final private int buttonId;
        public boolean isReplyButton;
        public String buttonText;
        public int buttonVisibility = View.VISIBLE;

        public PopupButton(int id, String[] buttonTextArray) {
            buttonId = id;
            isReplyButton = false;
            if (buttonId == ButtonListPreference.BUTTON_REPLY
                    || buttonId == ButtonListPreference.BUTTON_QUICKREPLY
                    || buttonId == ButtonListPreference.BUTTON_REPLY_BY_ADDRESS) {
                isReplyButton = true;
            }
            
            if (buttonId < buttonTextArray.length) {
            	buttonText = buttonTextArray[buttonId];
            }

            if (buttonId == ButtonListPreference.BUTTON_DISABLED) { // Disabled
                buttonVisibility = View.GONE;
            }
        }

        @Override
        public void onClick(View v) {
        	mButtonsListener.onButtonClicked(buttonId);
        }
    }
    
    @Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	public static interface OnReactToMessage {
        abstract void onViewMessage(SmsMmsMessage message);

        abstract void onReplyToMessage(SmsMmsMessage message);
    }
    
    public static interface SmsPopupButtonsListener {
    	abstract void onButtonClicked(int buttonType);
    	abstract LruCache<Uri, Bitmap> getCache();
    }
    
}
