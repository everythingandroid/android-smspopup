package net.everythingandroid.smspopup;

import net.everythingandroid.smspopup.ManageKeyguard.LaunchOnKeyguardExit;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SMSPopupActivity extends Activity {
	private SmsMmsMessage message;

	private boolean exitingKeyguardSecurely = false;
	private Bundle bundle = null;
	private SharedPreferences myPrefs;
	private TextView headerTV; 
	private TextView messageTV;
	private TextView fromTV;
	private TextView mmsSubjectTV;
	private LinearLayout viewButtonLayout;
	private LinearLayout mmsLinearLayout;
	private ScrollView messageScrollView;
	private boolean wasVisible = false;
	
	private final double WIDTH = 0.8;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		Log.v("SMSPopupActivity: onCreate()");
		
		//First things first, acquire wakelock, otherwise the phone may sleep
		ManageWakeLock.acquirePartial(getApplicationContext());
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.popup);

		//Get shared prefs
		myPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		//Check preferences and then blur out background behind window
		if (myPrefs.getBoolean(getString(R.string.pref_blur_key),
				Boolean.valueOf(getString(R.string.pref_blur_default)))) {
	      getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
	      	WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		}
		
		//This sets the minimum width of the activity to 75% of the screen size
		//only needed because the theme of this activity is "dialog" so it looks
		//like it's floating and doesn't seem to fill_parent like a regular activity
		LinearLayout mainLL = (LinearLayout) findViewById(R.id.MainLinearLayout);		
		Display d = getWindowManager().getDefaultDisplay();
		int width = (int)(d.getWidth() * WIDTH);
		Log.v("setting width to: " + width);
		mainLL.setMinimumWidth(width);
		
		//Find the main textviews
		fromTV = (TextView) findViewById(R.id.FromTextView);
		messageTV = (TextView) findViewById(R.id.MessageTextView);
		headerTV = (TextView) findViewById(R.id.HeaderTextView);
		mmsSubjectTV = (TextView) findViewById(R.id.MmsSubjectTextView);
		
		viewButtonLayout = (LinearLayout) findViewById(R.id.ViewButtonLinearLayout);
		
		messageScrollView = (ScrollView) findViewById(R.id.MessageScrollView);
		
		mmsLinearLayout = (LinearLayout) findViewById(R.id.MmsLinearLayout);
				
		//The close button
		Button closeButton = (Button) findViewById(R.id.closeButton);
		closeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				Intent i = new Intent(SMSPopupActivity.this.getApplicationContext(),
				      SMSPopupUtilsService.class);
				// i.putExtra(SMSPopupUtilsService.EXTRAS_THREAD_ID,
				// message.getThreadId());
				i.setAction(SMSPopupUtilsService.ACTION_MARK_THREAD_READ);
				i.putExtras(message.toBundle());
				SMSPopupUtilsService.beginStartingService(
						SMSPopupActivity.this.getApplicationContext(), i);
				
				// Finish up this activity
				myFinish();
			}
		});
		
		//The inbox button
		Button inboxButton = (Button) findViewById(R.id.InboxButton);
		inboxButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				exitingKeyguardSecurely = true;
				ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
					public void LaunchOnKeyguardExitSuccess() {
						Intent i = SMSPopupUtils.getSmsIntent();
						SMSPopupActivity.this.getApplicationContext().startActivity(i);
               }
				});				
				myFinish();
			}
		});

		//The view button (if in privacy mode)
		Button viewButton = (Button) findViewById(R.id.ViewButton);
		viewButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				exitingKeyguardSecurely = true;
				ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
					public void LaunchOnKeyguardExitSuccess() {
						Intent i = getIntent();
						i.putExtra(SmsMmsMessage.EXTRAS_NOTIFY, false);
						startActivity(i);
						// finish();
               }
				});
			}
		});
		
		//The reply button
		Button replyButton = (Button) findViewById(R.id.replyButton);
		replyButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				exitingKeyguardSecurely = true;
				ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
					public void LaunchOnKeyguardExitSuccess() {
						Intent reply = message.getReplyIntent();
						SMSPopupActivity.this.getApplicationContext().startActivity(reply);
               }
				});
				ManageNotification.clearAll(SMSPopupActivity.this.getApplicationContext(), true);
				myFinish();
			}
		});		

		// The ViewMMS button
		Button viewMmsButton = (Button) findViewById(R.id.ViewMmsButton);
		viewMmsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				exitingKeyguardSecurely = true;
				ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
					public void LaunchOnKeyguardExitSuccess() {
						Intent reply = message.getReplyIntent();
						SMSPopupActivity.this.getApplicationContext().startActivity(reply);
					}
				});
				ManageNotification.clearAll(SMSPopupActivity.this.getApplicationContext(), true);
				myFinish();
			}
		});		

		
		if (bundle == null) {		
			populateViews(getIntent().getExtras());
		} else {
			populateViews(bundle);
		}
		
		wakeApp();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.v("SMSPopupActivity: onNewIntent()");

		//First things first, acquire wakelock, otherwise the phone may sleep
		ManageWakeLock.acquirePartial(getApplicationContext());
		
		setIntent(intent);
		
		//Re-populate views with new intent data (ie. new sms data) 
		populateViews(intent.getExtras());
		
		wakeApp();
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.v("SMSPopupActivity: onStart()");
		ManageWakeLock.acquirePartial(getApplicationContext());		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.v("SMSPopupActivity: onResume()");
		wasVisible = false;
		//Reset exitingKeyguardSecurely bool to false
		exitingKeyguardSecurely = false;		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		Log.v("SMSPopupActivity: onPause()");

		if (wasVisible) {
			//Cancel the receiver that will clear our locks
			ClearAllReceiver.removeCancel(getApplicationContext());
			ClearAllReceiver.clearAll(!exitingKeyguardSecurely);
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Log.v("SMSPopupActivity: onStop()");
		
		//Cancel the receiver that will clear our locks
		ClearAllReceiver.removeCancel(getApplicationContext());		
		ClearAllReceiver.clearAll(!exitingKeyguardSecurely);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		Log.v("SMSPopupActivity: onWindowFocusChanged(" + hasFocus + ")");
		if (hasFocus) {
			// This is really hacky, basically a flag that is set if the message
			// was at some point visible. I tried using onResume() or other methods
			// to prevent doing some things 2 times but this seemed to be the only
			// reliable way (?)
			wasVisible = true;
		}
	}

	@Override
	public void  onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.v("SMSPopupActivity: onSaveInstanceState()");
		
		// Save values from most recent bundle (ie. most recent message)
		outState.putAll(bundle);
	}
	
	private void myFinish() {
		// TODO: instead of clearing all notifications here, I should really
		// update the notifications instead (in the case the user cleared 1
		// notification but still has other unread messages waiting
		
		// Clear all notifications
		ManageNotification.clearAll(getApplicationContext());
		
		// Cancel any reminder notifications
		ReminderReceiver.cancelReminder(getApplicationContext());
		
		finish();
	}
	
	private void populateViews(Bundle b) {
		// Store bundle
		bundle = b;

		// Regenerate the SmsMmsMessage from the extras bundle
		message = new SmsMmsMessage(getApplicationContext(), bundle);

		// Refresh privacy settings (hide/show message)
		refreshPrivacy();
		
		// Find the ImageView that will show the contact photo
		ImageView iv = (ImageView) findViewById(R.id.FromImageView);
		
		// See if we have a contact photo, if so set it to the IV, if not, show a
		// generic dialog info icon
		Bitmap contactPhoto = message.getContactPhoto();
		if (contactPhoto != null) {
			iv.setImageBitmap(contactPhoto);
		} else {
			iv.setImageDrawable(
				getResources().getDrawable(android.R.drawable.ic_dialog_info));
		}

		// Show/hide the LinearLayout and update the unread message count
		// if there are >1 unread messages waiting
		LinearLayout mLL = (LinearLayout) findViewById(R.id.UnreadCountLinearLayout);
		TextView tv = (TextView) findViewById(R.id.UnreadCountTextView);
		if (message.getUnreadCount() <= 1) {
			mLL.setVisibility(View.GONE);
			tv.setText("");
		} else {	
			String textWaiting = String.format(
			      getString(R.string.unread_text_waiting), message
			      .getUnreadCount() - 1);
			tv.setText(textWaiting);
			mLL.setVisibility(View.VISIBLE);
		}
		
		// Update TextView that contains the timestamp for the incoming message
		String headerText = getString(R.string.new_text_at);
		headerText = headerText.replaceAll("%s", message.getFormattedTimestamp());
		
		//Set the from, message and header views
		fromTV.setText(message.getContactName());
		if (message.getMessageType() == SmsMmsMessage.MESSAGE_TYPE_SMS) {
			messageTV.setText(message.getMessageBody());
		} else {
			mmsSubjectTV.setText(getString(R.string.mms_subject) + " " + message.getMessageBody());
		}
		headerTV.setText(headerText);		
	}
	
	private void refreshPrivacy() {	
		
		//We need to init the keyguard class so we can check if the keyguard is on
		ManageKeyguard.initialize(getApplicationContext());

		//Fetch privacy mode		
		boolean privacyMode = myPrefs.getBoolean(
				getString(R.string.pref_privacy_key),
				Boolean.valueOf(getString(R.string.pref_privacy_default)));
		
		// If it's a MMS message, just show the MMS layout
		if (message.getMessageType() == SmsMmsMessage.MESSAGE_TYPE_MMS) {
			viewButtonLayout.setVisibility(View.GONE);
			messageScrollView.setVisibility(View.GONE);
			mmsLinearLayout.setVisibility(View.VISIBLE);
			
			// If no MMS subject, hide the subject text view
			boolean subjectAvailable = true;
			if (message.getMessageBody() == null) {
				subjectAvailable = false;
			} else {
				if (message.getMessageBody().length() == 0) {
					subjectAvailable = false;
				}
			}
			if (!subjectAvailable) {
				mmsSubjectTV.setVisibility(View.GONE);
			} else {
				mmsSubjectTV.setVisibility(View.VISIBLE);
			}
		} else {
			// Otherwise hide MMS layout and show either the view button if in
			// privacy mode or the message body textview if not
			mmsLinearLayout.setVisibility(View.GONE);
			if (privacyMode && ManageKeyguard.inKeyguardRestrictedInputMode()) {
				viewButtonLayout.setVisibility(View.VISIBLE);
				messageScrollView.setVisibility(View.GONE);
			} else {
				viewButtonLayout.setVisibility(View.GONE);
				messageScrollView.setVisibility(View.VISIBLE);
			}
		}
	}
	
	private void wakeApp() {

		// Time to acquire a full WakeLock (turn on screen)
		ManageWakeLock.acquireFull(getApplicationContext());
		
		// See if a notification has been played for this message...
		if (message.getNotify()) {
			// Store extra to signify we have already notified for this message
			bundle.putBoolean(SmsMmsMessage.EXTRAS_NOTIFY, false);

			// Reset the reminderCount to 0 just to be sure
			message.updateReminderCount(0);

			// Schedule a reminder notification
			ReminderReceiver.scheduleReminder(getApplicationContext(), message);
			
			// Run the notification
			ManageNotification.show(getApplicationContext(), message);			
		}
	}

	@Override
	protected void onDestroy() {
		Log.v("onDestroy()");

		// ClearAllReceiver.clearAll(!exitingKeyguardSecurely);
		super.onDestroy();
	}
}