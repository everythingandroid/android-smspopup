package net.everythingandroid.smspopup;

import net.everythingandroid.smspopup.ManageKeyguard.LaunchOnKeyguardExit;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.tts.TTS;

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
	private boolean replying = false;
	private boolean inbox = false;
	private boolean privacyMode = false;
	private boolean messageViewed = true;
	
	private static final double WIDTH = 0.8;
	private static final int DELETE_DIALOG = 0;

	private static final int CONTEXT_TTS_ID 		= Menu.FIRST;
	private static final int CONTEXT_CLOSE_ID 	= Menu.FIRST + 1;
	private static final int CONTEXT_REPLY_ID 	= Menu.FIRST + 2;
	private static final int CONTEXT_DELETE_ID 	= Menu.FIRST + 3;
	
//	private static final int CONTEXT_VIEW_CONTACT_ID = Menu.FIRST + 1;
//	private static final int CONTEXT_ADD_CONTACT_ID = Menu.FIRST + 2;
	
	private TTS myTts = null;
	
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
		
		//Fetch privacy mode		
		privacyMode = myPrefs.getBoolean(
				getString(R.string.pref_privacy_key),
				Boolean.valueOf(getString(R.string.pref_privacy_default)));
		
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
		
		// Enable long-press context menu
		registerForContextMenu(findViewById(R.id.MainLinearLayout));
		
		//The close button
		Button closeButton = (Button) findViewById(R.id.closeButton);
		closeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				closeMessage();
			}
		});
		
		//The inbox button
		Button inboxButton = (Button) findViewById(R.id.InboxButton);
		inboxButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				gotoInbox();
			}
		});

		//The view button (if in privacy mode)
		Button viewButton = (Button) findViewById(R.id.ViewButton);
		viewButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				viewMessage();
			}
		});
		
		//The reply button
		Button replyButton = (Button) findViewById(R.id.replyButton);
		replyButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				replyToMessage();
			}
		});		

		// The ViewMMS button
		Button viewMmsButton = (Button) findViewById(R.id.ViewMmsButton);
		viewMmsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				replyToMessage();
			}
		});		

		// The Delete button
		Button deleteButton = (Button) findViewById(R.id.deleteButton);
		
		deleteButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showDialog(DELETE_DIALOG);
			}
		});
		
		// See if user wants all buttons hidden
		if (!myPrefs.getBoolean(getString(R.string.pref_show_buttons_key),
				Boolean.valueOf(getString(R.string.pref_show_buttons_default)))) {
			LinearLayout mButtonLinearLayout =
				(LinearLayout) findViewById(R.id.ButtonLinearLayout);
			mButtonLinearLayout.setVisibility(View.GONE);
		} else { // Otherwise see if user wants the delete button visible
			if (myPrefs.getBoolean(getString(R.string.pref_show_delete_button_key),
					Boolean.valueOf(getString(R.string.pref_show_delete_button_default)))) {
				deleteButton.setVisibility(View.VISIBLE);
			} else {
				deleteButton.setVisibility(View.GONE);
			}
		}

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

		if (myTts != null) {
			myTts.shutdown();
		}
		
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
			refreshPrivacy();
		}
	}

	@Override
	public void  onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.v("SMSPopupActivity: onSaveInstanceState()");
		
		// Save values from most recent bundle (ie. most recent message)
		outState.putAll(bundle);
	}
	
	/*
	 * Customized activity finish.  Ensures the notification is in sync and cancels
	 * any scheduled reminders (as the user has interrupted the app.
	 */
	private void myFinish() {
		Log.v("myFinish()");
		
		if (inbox) {
			ManageNotification.clearAll(getApplicationContext());
		} else {
		
			// Start a service that will update the notification in the status bar
			Intent i = new Intent(
					getApplicationContext(),
					SMSPopupUtilsService.class);
			i.setAction(SMSPopupUtilsService.ACTION_UPDATE_NOTIFICATION);
			
			// Convert current message to bundle
			i.putExtras(message.toBundle());
			
			// We need to know if the user is replying - if so, the entire thread id should
			// be ignored when working out the message tally in the notification bar.  We
			// can't rely on the system database as it may take a little while for the reply
			// intent to fire and load up the messaging up (after which the messages will be
			// marked read in the database).
			i.putExtra(SmsMmsMessage.EXTRAS_REPLYING, replying);
			
			// Start the service
			SMSPopupUtilsService.beginStartingService(
					SMSPopupActivity.this.getApplicationContext(), i);
		}
			
		// Cancel any reminder notifications
		ReminderReceiver.cancelReminder(getApplicationContext());
		
		// Finish up the activity
		finish();
	}
	
	/*
	 * Populate all the main SMS/MMS views with content from the actual SmsMmsMessage
	 */
	private void populateViews(Bundle b) {
		// Store bundle
		bundle = b;

		// Regenerate the SmsMmsMessage from the extras bundle
		message = new SmsMmsMessage(getApplicationContext(), bundle);

		// If it's a MMS message, just show the MMS layout
		if (message.getMessageType() == SmsMmsMessage.MESSAGE_TYPE_MMS) {
			viewButtonLayout.setVisibility(View.GONE);
			messageScrollView.setVisibility(View.GONE);
			mmsLinearLayout.setVisibility(View.VISIBLE);
			
			// If no MMS subject, hide the subject text view
			if (TextUtils.isEmpty(message.getMessageBody())) {
				mmsSubjectTV.setVisibility(View.GONE);
			} else {
				mmsSubjectTV.setVisibility(View.VISIBLE);
			}
		} else {
			// Otherwise hide MMS layout
			mmsLinearLayout.setVisibility(View.GONE);
			
			//messageScrollView.setVisibility(View.VISIBLE);

			// Refresh privacy settings (hide/show message) depending on privacy setting
			refreshPrivacy();			
		}
		
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
		ImageView dividerIV = (ImageView) findViewById(R.id.ImageView1);
		TextView tv = (TextView) findViewById(R.id.UnreadCountTextView);
		if (message.getUnreadCount() <= 1) {
			mLL.setVisibility(View.GONE);
			dividerIV.setVisibility(View.GONE);
			tv.setText("");
		} else {	
			String textWaiting = String.format(
			      getString(R.string.unread_text_waiting), 
			      message.getUnreadCount() - 1);
			tv.setText(textWaiting);
			mLL.setVisibility(View.VISIBLE);
			dividerIV.setVisibility(View.VISIBLE);
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
	
	/*
	 * This handles hiding and showing various views depending on the privacy settings
	 * of the app and the current state of the phone (keyguard on or off) 
	 */
	final private void refreshPrivacy() {
		Log.v("refreshPrivacy()");
		messageViewed = true;
		if (privacyMode) {
			//We need to init the keyguard class so we can check if the keyguard is on
			ManageKeyguard.initialize(getApplicationContext());

			if (ManageKeyguard.inKeyguardRestrictedInputMode()) {
				messageViewed = false;
				viewButtonLayout.setVisibility(View.VISIBLE);
				messageScrollView.setVisibility(View.GONE);
			} else {
				viewButtonLayout.setVisibility(View.GONE);
				messageScrollView.setVisibility(View.VISIBLE);
			}
		} else {
			viewButtonLayout.setVisibility(View.GONE);
			messageScrollView.setVisibility(View.VISIBLE);
		}
		
//		// If it's a MMS message, just show the MMS layout
//		if (message.getMessageType() == SmsMmsMessage.MESSAGE_TYPE_MMS) {
//			viewButtonLayout.setVisibility(View.GONE);
//			messageScrollView.setVisibility(View.GONE);
//			mmsLinearLayout.setVisibility(View.VISIBLE);
//			
//			// If no MMS subject, hide the subject text view
//			if (TextUtils.isEmpty(message.getMessageBody())) {
//				mmsSubjectTV.setVisibility(View.GONE);
//			} else {
//				mmsSubjectTV.setVisibility(View.VISIBLE);
//			}
//		} else {
//			// Otherwise hide MMS layout and show either the view button if in
//			// privacy mode or the message body textview if not
//			mmsLinearLayout.setVisibility(View.GONE);
//
//			if (privacyMode && ManageKeyguard.inKeyguardRestrictedInputMode()) {
//			//if (privacyMode && myKM.inKeyguardRestrictedInputMode()) {
//				Log.v("PRIVACY: HIDING MESSAGE");
//				viewButtonLayout.setVisibility(View.VISIBLE);
//				messageScrollView.setVisibility(View.GONE);
//			} else {
//				Log.v("NO PRIVACY: SHOW MESSAGE");
//				viewButtonLayout.setVisibility(View.GONE);
//				messageScrollView.setVisibility(View.VISIBLE);
//			}
//		}
	}

	/*
	 * Wake up the activity, this will acquire the wakelock (turn on the screen) and
	 * sound the notification if needed.  This is called once all preparation is done
	 * for this activity (end of onCreate()).
	 */
	private void wakeApp() {
		// Time to acquire a full WakeLock (turn on screen)
		ManageWakeLock.acquireFull(getApplicationContext());
		
		replying = false;
		inbox = false;
		
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

	/*
	 * Create Dialog
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DELETE_DIALOG:
			return new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(getString(R.string.pref_show_delete_button_dialog_title))
				.setMessage(getString(R.string.pref_show_delete_button_dialog_text))
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						deleteMessage();
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.create();			
		}
		return null;
	}

	/*
	 * Create Context Menu (Long-press menu)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		menu.add(Menu.NONE, CONTEXT_TTS_ID, Menu.NONE, getString(R.string.button_tts));

		// If all buttons are hidden then lets add all options to the context menu
		if (!myPrefs.getBoolean(getString(R.string.pref_show_buttons_key),
				Boolean.valueOf(getString(R.string.pref_show_buttons_default)))) {
			
			// TODO: make a "mark read" check box here			
			menu.add(Menu.NONE, CONTEXT_CLOSE_ID, Menu.NONE, getString(R.string.button_close));
			menu.add(Menu.NONE, CONTEXT_REPLY_ID, Menu.NONE, getString(R.string.button_reply));			
			menu.add(Menu.NONE, CONTEXT_DELETE_ID, Menu.NONE, getString(R.string.button_delete));
//			.setCheckable(true)
//			.setIcon(android.R.drawable.ic_menu_delete);
		}		
	}

	/*
	 * Context Menu Item Selected
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case CONTEXT_TTS_ID:
				if (myTts == null) {
					myTts = new TTS(this, ttsInitListener, true);
				} else {
					speakMessage();
				}
				break;
			case CONTEXT_CLOSE_ID:
				closeMessage();
				break;
			case CONTEXT_REPLY_ID:
				replyToMessage();
				break;
			case CONTEXT_DELETE_ID:
				//showDialog(DELETE_DIALOG);
				deleteMessage();
				break;
		}		
		return super.onContextItemSelected(item);
	}
	
	/*
	 * Text-to-speech: this works, but needs some refining
	 */	
	private TTS.InitListener ttsInitListener = new TTS.InitListener() {
		public void onInit(int version) {
			speakMessage();
		}
	};
	
	/*
	 * Speak the message out loud using TTS library
	 */
	private void speakMessage() {
		ClearAllReceiver.removeCancel(getApplicationContext());
		
		// We'll use update notification to stop the sound playing
		ManageNotification.update(getApplicationContext(), message);
		
		// Speak the message!
		myTts.speak(message.getMessageBody(), 0, null);
	}

	/*
	 * Close the message window/popup, mark the message read if the user has this option on
	 */
	private void closeMessage() {
		if (messageViewed) {
			Intent i = new Intent(SMSPopupActivity.this.getApplicationContext(),
					SMSPopupUtilsService.class);
			i.setAction(SMSPopupUtilsService.ACTION_MARK_MESSAGE_READ);
			i.putExtras(message.toBundle());
			SMSPopupUtilsService.beginStartingService(
					SMSPopupActivity.this.getApplicationContext(), i);
		}
		// Finish up this activity
		myFinish();

	}
	
	/*
	 * Reply to the current message, start the reply intent
	 */
	private void replyToMessage() {
		exitingKeyguardSecurely = true;
		ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
			public void LaunchOnKeyguardExitSuccess() {
				Intent reply = message.getReplyIntent();
				SMSPopupActivity.this.getApplicationContext().startActivity(reply);
				replying = true;
				myFinish();
			}
		});
	}
	
	/*
	 * View the private message (this basically just unlocks the keyguard and then
	 * reloads the activity).
	 */
	private void viewMessage() {
		exitingKeyguardSecurely = true;
		ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
			public void LaunchOnKeyguardExitSuccess() {				
				// Yet another fix for the View button in privacy mode :(
				// This will remotely call refreshPrivacy in case the user doesn't have the security pattern on
				// (so the screen will not refresh and therefore the popup will not come out of privacy mode)
				runOnUiThread(new Runnable() {
					public void run() {
						refreshPrivacy();
					}
				});
			}
		});
	}
	
	/*
	 * Take the user to the messaging app inbox
	 */
	private void gotoInbox() {
		exitingKeyguardSecurely = true;
		ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
			public void LaunchOnKeyguardExitSuccess() {
				Intent i = SMSPopupUtils.getSmsIntent();
				SMSPopupActivity.this.getApplicationContext().startActivity(i);
				inbox = true;
				myFinish();
			}
		});
	}

	/*
	 * Delete the current message from the system database
	 */
	private void deleteMessage() {
		Intent i = new Intent(
				SMSPopupActivity.this.getApplicationContext(),
				SMSPopupUtilsService.class);
		i.setAction(SMSPopupUtilsService.ACTION_DELETE_MESSAGE);
		i.putExtras(message.toBundle());
		SMSPopupUtilsService.beginStartingService(
				SMSPopupActivity.this.getApplicationContext(), i);
		myFinish();
	}
}