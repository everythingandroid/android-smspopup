package net.everythingandroid.smspopup;

import net.everythingandroid.smspopup.ManageKeyguard.LaunchOnKeyguardExit;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.tts.TTS;

public class SmsPopupActivity extends Activity {
  private SmsMmsMessage message;

  private boolean exitingKeyguardSecurely = false;
  private Bundle bundle = null;
  private SharedPreferences myPrefs;

  private TextView fromTV;
  private TextView messageReceivedTV;
  private TextView messageTV;

  private TextView mmsSubjectTV = null;
  //private LinearLayout privacyLayout = null;
  //private LinearLayout mmsLinearLayout = null;
  private ScrollView messageScrollView = null;
  private ImageView photoImageView = null;
  private Drawable contactPhotoPlaceholderDrawable = null;
  private static Bitmap contactPhoto = null;
  private EditText qrEditText = null;

  private ViewStub unreadCountViewStub;
  private View unreadCountView = null;
  private ViewStub mmsViewStub;
  private View mmsView = null;
  private ViewStub privacyViewStub;
  private View privacyView = null;
  private ViewStub buttonsViewStub;
  private View buttonsView = null;

  private boolean wasVisible = false;
  private boolean replying = false;
  private boolean inbox = false;
  private boolean privacyMode = false;
  private boolean messageViewed = true;

  private static final double WIDTH = 0.8;
  private static final int DIALOG_DELETE         = Menu.FIRST;
  private static final int DIALOG_QUICKREPLY     = Menu.FIRST + 1;
  private static final int DIALOG_PRESET_MSG     = Menu.FIRST + 2;

  private static final int CONTEXT_TTS_ID        = Menu.FIRST;
  private static final int CONTEXT_CLOSE_ID      = Menu.FIRST + 1;
  private static final int CONTEXT_REPLY_ID      = Menu.FIRST + 2;
  private static final int CONTEXT_DELETE_ID     = Menu.FIRST + 3;
  private static final int CONTEXT_QUICKREPLY_ID = Menu.FIRST + 4;

  private TextView quickreplyTextView;
  private static SmsMmsMessage quickReplySmsMessage;

  private SmsPopupDbAdapter mDbAdapter;
  private Cursor mCursor = null;
  // private int quickMessageSelected = -1;
  private String quickReplyText = "";

  //	private static final int CONTEXT_VIEW_CONTACT_ID = Menu.FIRST + 1;
  //	private static final int CONTEXT_ADD_CONTACT_ID = Menu.FIRST + 2;

  private TTS myTts = null;

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    if (Log.DEBUG) Log.v("SMSPopupActivity: onCreate()");

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
    mainLL.setMinimumWidth(width);

    //Find the main textviews
    fromTV = (TextView) findViewById(R.id.FromTextView);
    messageTV = (TextView) findViewById(R.id.MessageTextView);
    messageReceivedTV = (TextView) findViewById(R.id.HeaderTextView);
    messageScrollView = (ScrollView) findViewById(R.id.MessageScrollView);

    // Find the ImageView that will show the contact photo
    photoImageView = (ImageView) findViewById(R.id.FromImageView);
    contactPhotoPlaceholderDrawable = getResources().getDrawable(SmsPopupUtils.CONTACT_PHOTO_PLACEHOLDER);

    //viewButtonLayout = (LinearLayout) findViewById(R.id.ViewButtonLinearLayout);
    //mmsLinearLayout = (LinearLayout) findViewById(R.id.MmsLinearLayout);

    //    if (privacyMode) {
    //      privacyViewStub = (ViewStub) findViewById(R.id.PrivacyViewStub);
    //      privacyView = privacyViewStub.inflate();
    //
    //      //The view button (if in privacy mode)
    //      Button viewButton = (Button) privacyView.findViewById(R.id.ViewButton);
    //      viewButton.setOnClickListener(new OnClickListener() {
    //        public void onClick(View v) {
    //          viewMessage();
    //        }
    //      });
    //    }

    // Enable long-press context menu
    registerForContextMenu(findViewById(R.id.MainLinearLayout));

    // Assign view stubs
    unreadCountViewStub = (ViewStub) findViewById(R.id.UnreadCountViewStub);
    mmsViewStub = (ViewStub) findViewById(R.id.MmsViewStub);
    privacyViewStub = (ViewStub) findViewById(R.id.PrivacyViewStub);
    buttonsViewStub = (ViewStub) findViewById(R.id.ButtonsViewStub);

    // See if user wants to show buttons on the popup
    if (myPrefs.getBoolean(getString(R.string.pref_show_buttons_key),
        Boolean.valueOf(getString(R.string.pref_show_buttons_default)))) {

      // Check if the ViewStub has been inflated and if not, inflate it
      if (buttonsView == null) {
        buttonsView = buttonsViewStub.inflate();
      }

      //The close button
      Button closeButton = (Button) buttonsView.findViewById(R.id.closeButton);
      closeButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          closeMessage();
        }
      });

      //The reply button
      Button replyButton = (Button) buttonsView.findViewById(R.id.replyButton);
      replyButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          replyToMessage();
        }
      });

      // The Delete button
      Button deleteButton = (Button) buttonsView.findViewById(R.id.deleteButton);

      if (myPrefs.getBoolean(getString(R.string.pref_show_delete_button_key),
          Boolean.valueOf(getString(R.string.pref_show_delete_button_default)))) {
        //deleteButton.setVisibility(View.VISIBLE);
        deleteButton.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
            showDialog(DIALOG_DELETE);
          }
        });
      } else {
        deleteButton.setVisibility(View.GONE);
      }
    }

    if (bundle == null) {
      recycleContactPhoto();
      populateViews(getIntent().getExtras());
    } else {
      populateViews(bundle);
    }

    mDbAdapter = new SmsPopupDbAdapter(getApplicationContext());

    wakeApp();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (Log.DEBUG) Log.v("SMSPopupActivity: onNewIntent()");

    //First things first, acquire wakelock, otherwise the phone may sleep
    ManageWakeLock.acquirePartial(getApplicationContext());

    setIntent(intent);

    recycleContactPhoto();

    //Re-populate views with new intent data (ie. new sms data)
    populateViews(intent.getExtras());

    wakeApp();
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (Log.DEBUG) Log.v("SMSPopupActivity: onStart()");
    ManageWakeLock.acquirePartial(getApplicationContext());
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (Log.DEBUG) Log.v("SMSPopupActivity: onResume()");
    wasVisible = false;
    //Reset exitingKeyguardSecurely bool to false
    exitingKeyguardSecurely = false;
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (Log.DEBUG) Log.v("SMSPopupActivity: onPause()");

    if (myTts != null) {
      myTts.shutdown();
    }

    if (wasVisible) {
      //Cancel the receiver that will clear our locks
      ClearAllReceiver.removeCancel(getApplicationContext());
      ClearAllReceiver.clearAll(!exitingKeyguardSecurely);
    }

    mDbAdapter.close();
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (Log.DEBUG) Log.v("SMSPopupActivity: onStop()");

    //Cancel the receiver that will clear our locks
    ClearAllReceiver.removeCancel(getApplicationContext());
    ClearAllReceiver.clearAll(!exitingKeyguardSecurely);
  }

  @Override
  protected void onDestroy() {
    recycleContactPhoto();
    super.onDestroy();
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    //Log.v("SMSPopupActivity: onWindowFocusChanged(" + hasFocus + ")");
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
    if (Log.DEBUG) Log.v("SMSPopupActivity: onSaveInstanceState()");

    // Save values from most recent bundle (ie. most recent message)
    outState.putAll(bundle);
  }

  /*
   * Customized activity finish.  Ensures the notification is in sync and cancels
   * any scheduled reminders (as the user has interrupted the app.
   */
  private void myFinish() {
    if (Log.DEBUG) Log.v("myFinish()");

    if (inbox) {
      ManageNotification.clearAll(getApplicationContext());
    } else {

      // Start a service that will update the notification in the status bar
      Intent i = new Intent(
          getApplicationContext(),
          SmsPopupUtilsService.class);
      i.setAction(SmsPopupUtilsService.ACTION_UPDATE_NOTIFICATION);

      // Convert current message to bundle
      i.putExtras(message.toBundle());

      // We need to know if the user is replying - if so, the entire thread id should
      // be ignored when working out the message tally in the notification bar.  We
      // can't rely on the system database as it may take a little while for the reply
      // intent to fire and load up the messaging up (after which the messages will be
      // marked read in the database).
      i.putExtra(SmsMmsMessage.EXTRAS_REPLYING, replying);

      // Start the service
      SmsPopupUtilsService.beginStartingService(
          SmsPopupActivity.this.getApplicationContext(), i);
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
      if (mmsView == null) {
        mmsView = mmsViewStub.inflate();
        mmsSubjectTV = (TextView) mmsView.findViewById(R.id.MmsSubjectTextView);

        // The ViewMMS button
        Button viewMmsButton = (Button) mmsView.findViewById(R.id.ViewMmsButton);
        viewMmsButton.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
            replyToMessage();
          }
        });
      }
      messageScrollView.setVisibility(View.GONE);
      //privacyViewStub.setVisibility(View.GONE);
      mmsViewStub.setVisibility(View.VISIBLE);

      // If no MMS subject, hide the subject text view
      if (TextUtils.isEmpty(message.getMessageBody())) {
        mmsSubjectTV.setVisibility(View.GONE);
      } else {
        mmsSubjectTV.setVisibility(View.VISIBLE);
      }
    } else {
      // Otherwise hide MMS layout
      mmsViewStub.setVisibility(View.GONE);

      // Refresh privacy settings (hide/show message) depending on privacy setting
      refreshPrivacy();
    }

    // Fetch contact photo in background
    if (contactPhoto == null) {
      photoImageView.setImageDrawable(contactPhotoPlaceholderDrawable);
      new FetchContactPhotoTask().execute(message.getContactId());
    } else {
      photoImageView.setImageBitmap(contactPhoto);
    }

    // See if we have a contact photo, if so set it to the IV, if not, show a
    // generic dialog info icon
    //    Bitmap contactPhoto = message.getContactPhoto();
    //    if (contactPhoto != null) {
    //      photoImageView.setImageBitmap(contactPhoto);
    //    } else {
    //      photoImageView.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_dialog_info));
    //    }

    if (message.getUnreadCount() <= 1) {
      if (unreadCountView != null) {
        unreadCountView.setVisibility(View.GONE);
      }
      //      mLL.setVisibility(View.GONE);
      //      dividerIV.setVisibility(View.GONE);
      //      tv.setText("");
    } else {
      if (unreadCountView == null) {
        unreadCountView = unreadCountViewStub.inflate();
      }
      unreadCountView.setVisibility(View.VISIBLE);
      //View mView = ((ViewStub) findViewById(R.id.UnreadCountLinearLayout)).inflate();
      //LinearLayout mLL = (LinearLayout) findViewById(R.id.UnreadCountLinearLayout);
      //ImageView dividerIV = (ImageView) findViewById(R.id.ImageView1);
      TextView tv = (TextView) unreadCountView.findViewById(R.id.UnreadCountTextView);

      String textWaiting = String.format(
          getString(R.string.unread_text_waiting),
          message.getUnreadCount() - 1);
      tv.setText(textWaiting);

      //The inbox button
      Button inboxButton = (Button) unreadCountView.findViewById(R.id.InboxButton);
      inboxButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          gotoInbox();
        }
      });
      //mLL.setVisibility(View.VISIBLE);
      //dividerIV.setVisibility(View.VISIBLE);
    }

    // Update TextView that contains the timestamp for the incoming message
    String headerText =
      String.format(getString(R.string.new_text_at, message.getFormattedTimestamp().toString()));
    //headerText = String.format(format, args)
    //headerText = headerText.replaceAll("%s", message.getFormattedTimestamp().toString());

    //Set the from, message and header views
    fromTV.setText(message.getContactName());
    if (message.getMessageType() == SmsMmsMessage.MESSAGE_TYPE_SMS) {
      messageTV.setText(message.getMessageBody());
    } else {
      mmsSubjectTV.setText(getString(R.string.mms_subject) + " " + message.getMessageBody());
    }
    messageReceivedTV.setText(headerText);
  }

  /*
   * This handles hiding and showing various views depending on the privacy settings
   * of the app and the current state of the phone (keyguard on or off)
   */
  final private void refreshPrivacy() {
    if (Log.DEBUG) Log.v("refreshPrivacy()");
    messageViewed = true;
    if (privacyMode) {
      //We need to init the keyguard class so we can check if the keyguard is on
      ManageKeyguard.initialize(getApplicationContext());

      if (ManageKeyguard.inKeyguardRestrictedInputMode()) {
        messageViewed = false;

        if (privacyView == null) {
          privacyView = privacyViewStub.inflate();

          //The view button (if in privacy mode)
          Button viewButton = (Button) privacyView.findViewById(R.id.ViewButton);
          viewButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
              viewMessage();
            }
          });
        }
        messageScrollView.setVisibility(View.GONE);
      } else {
        if (privacyView != null) {
          privacyView.setVisibility(View.GONE);
        }
        messageScrollView.setVisibility(View.VISIBLE);
      }
    } else {
      if (privacyView != null) {
        privacyView.setVisibility(View.GONE);
      }
      messageScrollView.setVisibility(View.VISIBLE);
    }
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
      case DIALOG_DELETE:
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
      case DIALOG_QUICKREPLY:
        LayoutInflater factory = LayoutInflater.from(this);
        final View qrLayout = factory.inflate(R.layout.message_quick_reply, null);
        qrEditText = (EditText) qrLayout.findViewById(R.id.QuickReplyEditText);
        final TextView qrCounterTextView = (TextView) qrLayout.findViewById(R.id.QuickReplyCounterTextView);

        qrEditText.addTextChangedListener(new QMTextWatcher(this, qrCounterTextView));

        quickreplyTextView = (TextView) qrLayout.findViewById(R.id.QuickReplyTextView);
        //quickreplyTextView.setText("Message to " + message.getContactName());

        //updateQuickReplyView();
        qrCounterTextView.setText(
            QMTextWatcher.getQuickReplyCounterText(qrEditText.getText().toString()));

        return new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_email)
        .setTitle(R.string.quickreply_title)
        //.setMessage("QR description here")
        //.setView((new EditText(this)).setId(5))
        .setView(qrLayout)
        .setPositiveButton(R.string.quickreply_send_button, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            quickReply(qrEditText.getText().toString());
          }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .setNeutralButton(R.string.quickreply_preset_button, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            showDialog(DIALOG_PRESET_MSG);
          }
        })
        .create();

      case DIALOG_PRESET_MSG:
        mDbAdapter.open(true);
        mCursor = mDbAdapter.fetchAllQuickMessages();
        startManagingCursor(mCursor);

        return new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_email)
        .setTitle(R.string.pref_message_presets_title)
        .setCursor(mCursor, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int item) {
            if (Log.DEBUG) Log.v("Item clicked = " + item);
            mCursor.moveToPosition(item);
            if (Log.DEBUG) Log.v("Item text = " + mCursor.getString(SmsPopupDbAdapter.KEY_QUICKMESSAGE_NUM));
            quickReplyText = mCursor.getString(SmsPopupDbAdapter.KEY_QUICKMESSAGE_NUM);
            updateQuickReplyView();
            showDialog(DIALOG_QUICKREPLY);
          }
        }, SmsPopupDbAdapter.KEY_QUICKMESSAGE)
        .setOnCancelListener(new OnCancelListener() {
          public void onCancel(DialogInterface dialog) {
            showDialog(DIALOG_QUICKREPLY);
          }

        })
        .create();
    }

    return null;
  }

  @Override
  protected void onPrepareDialog(int id, Dialog dialog) {
    super.onPrepareDialog(id, dialog);

    // User interacted with phone, remove any held locks
    ClearAllReceiver.removeCancel(getApplicationContext());
    ClearAllReceiver.clearAll(false);

    switch (id) {
      case DIALOG_QUICKREPLY:
        updateQuickReplyView();
        break;
      case DIALOG_PRESET_MSG:
        if (Log.DEBUG) Log.v("onPrepareDialog for QUICK REPLY");
        //			mDbAdapter.open(true);
        //			mCursor = mDbAdapter.fetchAllQuickMessages();
        //			startManagingCursor(mCursor);
        //			if (mCursor == null) {
        //				Log.v("mCursor is null");
        //			}
        break;
    }
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
    menu.add(Menu.NONE, CONTEXT_QUICKREPLY_ID, Menu.NONE, "Quick Reply");
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
        deleteMessage();
        break;
      case CONTEXT_QUICKREPLY_ID:
        startQuickReply();
        break;
    }
    return super.onContextItemSelected(item);
  }

  /*
   * Text-to-speech: this works, but needs some refining
   */
  private final TTS.InitListener ttsInitListener = new TTS.InitListener() {
    public void onInit(int version) {
      speakMessage();
    }
  };

  /*
   * Speak the message out loud using TTS library
   */
  private void speakMessage() {
    ClearAllReceiver.removeCancel(getApplicationContext());
    ClearAllReceiver.clearAll(false);

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
      Intent i = new Intent(SmsPopupActivity.this.getApplicationContext(),
          SmsPopupUtilsService.class);
      i.setAction(SmsPopupUtilsService.ACTION_MARK_MESSAGE_READ);
      i.putExtras(message.toBundle());
      SmsPopupUtilsService.beginStartingService(
          SmsPopupActivity.this.getApplicationContext(), i);
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
        SmsPopupActivity.this.getApplicationContext().startActivity(reply);
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
        Intent i = SmsPopupUtils.getSmsIntent();
        SmsPopupActivity.this.getApplicationContext().startActivity(i);
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
        SmsPopupActivity.this.getApplicationContext(),
        SmsPopupUtilsService.class);
    i.setAction(SmsPopupUtilsService.ACTION_DELETE_MESSAGE);
    i.putExtras(message.toBundle());
    SmsPopupUtilsService.beginStartingService(
        SmsPopupActivity.this.getApplicationContext(), i);
    myFinish();
  }

  /*
   * Delete the current message from the system database
   */
  private void quickReply(String quickReplyMessage) {
    if (quickReplyMessage != null) {
      if (quickReplyMessage.length() > 0) {
        Intent i = new Intent(
            SmsPopupActivity.this.getApplicationContext(),
            SmsPopupUtilsService.class);
        i.setAction(SmsPopupUtilsService.ACTION_QUICKREPLY);
        i.putExtras(quickReplySmsMessage.toBundle());
        i.putExtra(SmsMmsMessage.EXTRAS_QUICKREPLY, quickReplyMessage);
        if (Log.DEBUG) Log.v("Sending message to " + quickReplySmsMessage.getContactName());
        SmsPopupUtilsService.beginStartingService(
            SmsPopupActivity.this.getApplicationContext(), i);
        Toast.makeText(this, R.string.quickreply_sending_toast, Toast.LENGTH_LONG).show();
        myFinish();
      } else {
        Toast.makeText(this, R.string.quickreply_nomessage_toast, Toast.LENGTH_LONG).show();
      }
    }
  }

  private void startQuickReply() {
    quickReplySmsMessage = message;
    quickReplyText = "";
    updateQuickReplyView();
    showDialog(DIALOG_QUICKREPLY);
  }

  private void updateQuickReplyView() {
    updateQuickReplyView(quickReplyText);
  }

  private void updateQuickReplyView(String editText) {
    if (Log.DEBUG) Log.v("updateQuickReplyView - " + editText);
    if (qrEditText != null) {
      qrEditText.setText(editText);
      qrEditText.setSelection(editText.length());
    }
    if (quickreplyTextView != null) {
      quickreplyTextView.setText(
          String.format(
              getString(R.string.quickreply_from_text), quickReplySmsMessage.getContactName()));
    }
  }

  /**
   * 
   * AsyncTask to fetch contact photo in background
   * 
   */
  private class FetchContactPhotoTask extends AsyncTask<String, Integer, Bitmap> {
    @Override
    protected Bitmap doInBackground(String... params) {
      if (Log.DEBUG) Log.v("Loading contact photo in background...");
      //      try {
      //        Thread.sleep(2000);
      //      } catch (InterruptedException e) {
      //        // TODO Auto-generated catch block
      //        e.printStackTrace();
      //      }
      return SmsPopupUtils.getPersonPhoto(SmsPopupActivity.this.getApplicationContext(), params[0]);
    }

    @Override
    protected void onPostExecute(Bitmap result) {
      if (Log.DEBUG) Log.v("Done loading contact photo");
      contactPhoto = result;
      if (result != null) {
        photoImageView.setImageBitmap(contactPhoto);
      }
    }
  }

  private void recycleContactPhoto() {
    if (contactPhoto != null) {
      contactPhoto.recycle();
    }
    contactPhoto = null;
  }
}