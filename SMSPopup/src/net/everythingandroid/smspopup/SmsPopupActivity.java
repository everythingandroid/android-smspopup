package net.everythingandroid.smspopup;

import net.everythingandroid.smspopup.ManageKeyguard.LaunchOnKeyguardExit;
import net.everythingandroid.smspopup.controls.QmTextWatcher;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Contacts;
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
  private ScrollView messageScrollView = null;
  private ImageView photoImageView = null;
  private Drawable contactPhotoPlaceholderDrawable = null;
  private static Bitmap contactPhoto = null;
  private EditText qrEditText = null;
  private ProgressDialog mProgressDialog = null;

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

  private static final String TTS_PACKAGE_NAME = "com.google.tts";

  private static final double WIDTH = 0.8;
  private static final int DIALOG_DELETE          = Menu.FIRST;
  private static final int DIALOG_QUICKREPLY      = Menu.FIRST + 1;
  private static final int DIALOG_PRESET_MSG      = Menu.FIRST + 2;
  private static final int DIALOG_LOADING         = Menu.FIRST + 3;

  private static final int CONTEXT_CLOSE_ID       = Menu.FIRST;
  private static final int CONTEXT_DELETE_ID      = Menu.FIRST + 1;
  private static final int CONTEXT_REPLY_ID       = Menu.FIRST + 2;
  private static final int CONTEXT_QUICKREPLY_ID  = Menu.FIRST + 3;
  private static final int CONTEXT_INBOX_ID       = Menu.FIRST + 4;
  private static final int CONTEXT_TTS_ID         = Menu.FIRST + 5;
  private static final int CONTEXT_VIEWCONTACT_ID = Menu.FIRST + 6;

  private TextView quickreplyTextView;
  private static SmsMmsMessage quickReplySmsMessage;

  private SmsPopupDbAdapter mDbAdapter;
  private Cursor mCursor = null;

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

      //Button 1
      Button button1 = (Button) buttonsView.findViewById(R.id.button1);
      PopupButton button1Vals = new PopupButton(getApplicationContext(),
          Integer.parseInt(myPrefs.getString(getString(R.string.pref_button1_key),
              getString(R.string.pref_button1_default))));
      button1.setOnClickListener(button1Vals);
      button1.setText(button1Vals.buttonText);
      button1.setVisibility(button1Vals.buttonVisibility);

      //Button 2
      Button button2 = (Button) buttonsView.findViewById(R.id.button2);
      PopupButton button2Vals = new PopupButton(getApplicationContext(),
          Integer.parseInt(myPrefs.getString(getString(R.string.pref_button2_key),
              getString(R.string.pref_button2_default))));
      button2.setOnClickListener(button2Vals);
      button2.setText(button2Vals.buttonText);
      button2.setVisibility(button2Vals.buttonVisibility);

      //Button 3
      Button button3 = (Button) buttonsView.findViewById(R.id.button3);
      PopupButton button3Vals = new PopupButton(getApplicationContext(),
          Integer.parseInt(myPrefs.getString(getString(R.string.pref_button3_key),
              getString(R.string.pref_button3_default))));
      button3.setOnClickListener(button3Vals);
      button3.setText(button3Vals.buttonText);
      button3.setVisibility(button3Vals.buttonVisibility);

    }

    if (bundle == null) {
      recycleContactPhoto();
      populateViews(getIntent().getExtras());
    } else { // this activity was recreated after being destroyed (ie. on orientation change)
      populateViews(bundle);
    }

    mDbAdapter = new SmsPopupDbAdapter(getApplicationContext());

    wakeApp();
  }

  class PopupButton implements OnClickListener {
    private int buttonId;
    public String buttonText;
    public int buttonVisibility = View.VISIBLE;

    public PopupButton(Context mContext, int id) {
      buttonId = id;
      String[] buttonTextArray = mContext.getResources().getStringArray(R.array.buttons_text);
      buttonText = buttonTextArray[buttonId];

      if (buttonId == 0) { // Disabled
        buttonVisibility = View.GONE;
      }
    }

    public void onClick(View v) {
      switch (buttonId) {
        case 0: // Disabled
          break;
        case 1: // Close
          closeMessage();
          break;
        case 2: // Delete
          showDialog(DIALOG_DELETE);
          break;
        case 3: // Delete no confirmation
          deleteMessage();
          break;
        case 4: // Reply
          replyToMessage();
          break;
        case 5: // Quick Reply
          quickReply();
          break;
        case 6: // Inbox
          gotoInbox();
          break;
        case 7: // Text-to-Speech
          speakMessage();
          break;
      }
    }
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

    updateQuickReplyView(null);
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (Log.DEBUG) Log.v("SMSPopupActivity: onPause()");

    if (myTts != null) {
      myTts.shutdown();
    }

    if (mProgressDialog != null) {
      mProgressDialog.dismiss();
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
      mmsView.setVisibility(View.VISIBLE);

      // If no MMS subject, hide the subject text view
      if (TextUtils.isEmpty(message.getMessageBody())) {
        mmsSubjectTV.setVisibility(View.GONE);
      } else {
        mmsSubjectTV.setVisibility(View.VISIBLE);
      }
    } else {
      // Otherwise hide MMS layout
      if (mmsView != null) {
        mmsView.setVisibility(View.GONE);
      }

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

    // If only 1 unread message waiting
    if (message.getUnreadCount() <= 1) {
      if (unreadCountView != null) {
        unreadCountView.setVisibility(View.GONE);
      }
    } else { // More unread messages waiting, show the extra view
      if (unreadCountView == null) {
        unreadCountView = unreadCountViewStub.inflate();
      }
      unreadCountView.setVisibility(View.VISIBLE);
      TextView tv = (TextView) unreadCountView.findViewById(R.id.UnreadCountTextView);

      String textWaiting =
        getString(R.string.unread_text_waiting, message.getUnreadCount() - 1);
      tv.setText(textWaiting);

      //The inbox button
      Button inboxButton = (Button) unreadCountView.findViewById(R.id.InboxButton);
      inboxButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
          gotoInbox();
        }
      });
    }

    // Update TextView that contains the timestamp for the incoming message
    String headerText =
      getString(R.string.new_text_at, message.getFormattedTimestamp().toString());

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
    messageViewed = false;

    if (message.getMessageType() == SmsMmsMessage.MESSAGE_TYPE_SMS) {
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

        qrEditText.addTextChangedListener(new QmTextWatcher(this, qrCounterTextView));
        quickreplyTextView = (TextView) qrLayout.findViewById(R.id.QuickReplyTextView);

        qrCounterTextView.setText(
            QmTextWatcher.getQuickReplyCounterText(qrEditText.getText().toString()));

        return new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_email)
        .setTitle(R.string.quickreply_title)
        .setView(qrLayout)
        .setPositiveButton(R.string.quickreply_send_button, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            sendQuickReply(qrEditText.getText().toString());
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

        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_email)
        //.setCustomTitle(arg0)
        .setTitle(R.string.pref_message_presets_title)
        .setOnCancelListener(new OnCancelListener() {
          public void onCancel(DialogInterface dialog) {
            showDialog(DIALOG_QUICKREPLY);
          }
        });

        // If user has some presets defined ...
        if (mCursor != null && mCursor.getCount() > 0) {

          mDialogBuilder.setCursor(mCursor, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
              if (Log.DEBUG) Log.v("Item clicked = " + item);
              mCursor.moveToPosition(item);
              if (Log.DEBUG) Log.v("Item text = " + mCursor.getString(SmsPopupDbAdapter.KEY_QUICKMESSAGE_NUM));

              quickReply(mCursor.getString(SmsPopupDbAdapter.KEY_QUICKMESSAGE_NUM));
              //              quickReplyText = mCursor.getString(SmsPopupDbAdapter.KEY_QUICKMESSAGE_NUM);
              //              updateQuickReplyView();
              //              showDialog(DIALOG_QUICKREPLY);
            }
          }, SmsPopupDbAdapter.KEY_QUICKMESSAGE);
        } else { // Otherwise display a placeholder as user has no presets
          MatrixCursor emptyCursor = new MatrixCursor(new String[]{SmsPopupDbAdapter.KEY_ROWID, SmsPopupDbAdapter.KEY_QUICKMESSAGE});
          emptyCursor.addRow(new String[]{"0", getString(R.string.message_presets_empty_text)});
          mDialogBuilder.setCursor(emptyCursor, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
              //              startActivity(new Intent(
              //                  SmsPopupActivity.this.getApplicationContext(),
              //                  net.everythingandroid.smspopup.ConfigPresetMessagesActivity.class));
            }
          }, SmsPopupDbAdapter.KEY_QUICKMESSAGE);
        }

        return mDialogBuilder.create();

      case DIALOG_LOADING:
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.loading_message));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        return mProgressDialog;
    }

    return null;
  }

  @Override
  protected void onPrepareDialog(int id, Dialog dialog) {
    super.onPrepareDialog(id, dialog);

    if (Log.DEBUG) Log.v("onPrepareDialog()");
    // User interacted with phone, remove any held locks
    ClearAllReceiver.removeCancel(getApplicationContext());
    ClearAllReceiver.clearAll(false);

    switch (id) {
      case DIALOG_QUICKREPLY:
        updateQuickReplyView(null);
        break;
      case DIALOG_PRESET_MSG:
        break;
    }
  }

  /*
   * Create Context Menu (Long-press menu)
   */
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);

    menu.add(Menu.NONE, CONTEXT_VIEWCONTACT_ID, Menu.NONE, "View Contact");
    menu.add(Menu.NONE, CONTEXT_CLOSE_ID, Menu.NONE, getString(R.string.button_close));
    menu.add(Menu.NONE, CONTEXT_DELETE_ID, Menu.NONE, getString(R.string.button_delete));
    menu.add(Menu.NONE, CONTEXT_REPLY_ID, Menu.NONE, getString(R.string.button_reply));
    menu.add(Menu.NONE, CONTEXT_QUICKREPLY_ID, Menu.NONE, "Quick Reply");
    menu.add(Menu.NONE, CONTEXT_TTS_ID, Menu.NONE, getString(R.string.button_tts));
    menu.add(Menu.NONE, CONTEXT_INBOX_ID, Menu.NONE, getString(R.string.button_inbox));
  }

  /*
   * Context Menu Item Selected
   */
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case CONTEXT_CLOSE_ID:
        closeMessage();
        break;
      case CONTEXT_DELETE_ID:
        deleteMessage();
        break;
      case CONTEXT_REPLY_ID:
        replyToMessage();
        break;
      case CONTEXT_QUICKREPLY_ID:
        quickReply();
        break;
      case CONTEXT_INBOX_ID:
        gotoInbox();
        break;
      case CONTEXT_TTS_ID:
        speakMessage();
        break;
      case CONTEXT_VIEWCONTACT_ID:
        viewContact();
        break;
    }
    return super.onContextItemSelected(item);
  }

  /*
   * Text-to-speech InitListener
   */
  private final TTS.InitListener ttsInitListener = new TTS.InitListener() {
    public void onInit(int version) {
      mProgressDialog.dismiss();
      speakMessage();
    }
  };

  /*
   * Speak the message out loud using TTS library
   */
  private void speakMessage() {
    // TODO: we should really require the keyguard be unlocked here if we are in privacy mode

    //    exitingKeyguardSecurely = true;
    //    ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
    //      public void LaunchOnKeyguardExitSuccess() {
    //
    //        runOnUiThread(new Runnable() {
    //
    //          public void run() {
    if (myTts == null) {

      // If TTS package is installed then show a loading dialog while the library fires up
      try {
        getPackageManager().getPackageInfo(TTS_PACKAGE_NAME, 0);
        showDialog(DIALOG_LOADING);
      } catch (NameNotFoundException e) {
        // No need to do anything here, failing is fine
      }

      // User interacted so remove all locks
      ClearAllReceiver.removeCancel(getApplicationContext());
      ClearAllReceiver.clearAll(false);

      // We'll use update notification to stop the sound playing
      ManageNotification.update(getApplicationContext(), message);

      // Init the TTS library
      myTts = new TTS(SmsPopupActivity.this.getApplicationContext(), ttsInitListener, true);

    } else {
      // Speak the message!
      myTts.speak(message.getMessageBody(), 0, null);
    }
    //  }
    //        });
    //      }
    //
    //    });
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
   * Sends the actual quick reply message
   */
  private void sendQuickReply(String quickReplyMessage) {
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

  /*
   * Show the quick reply dialog, resetting the text in the edittext and storing
   * the current SmsMmsMessage in a static var (in case another message comes in)
   */
  private void quickReply() {
    quickReply("");
  }

  /*
   * Show the quick reply dialog, if text passed is null or empty then store the
   * current SmsMmsMessage in a static var (in case another message comes in)
   */
  private void quickReply(String text) {
    // If this is a MMS just use regular reply, TODO: need to work out how to reply to MMS myself
    if (message.getMessageType() == SmsMmsMessage.MESSAGE_TYPE_MMS) {
      replyToMessage();
    } else {  // Else show the quick reply dialog
      if (text == null || "".equals(text)) {
        quickReplySmsMessage = message;
      }
      updateQuickReplyView(text);
      showDialog(DIALOG_QUICKREPLY);
    }
  }

  /*
   * View contact that has the message address (or create if it doesn't exist)
   */
  private void viewContact() {
    Intent contactIntent = new Intent(Contacts.Intents.SHOW_OR_CREATE_CONTACT);
    if (message.getMessageType() == SmsMmsMessage.MESSAGE_TYPE_MMS) {
      contactIntent.setData(Uri.fromParts("mailto", message.getAddress(), null));
    } else {
      contactIntent.setData(Uri.fromParts("tel", message.getAddress(), null));
    }
    startActivity(contactIntent);
  }

  /*
   * Refresh the quick reply view - update a text field and the counter
   */
  private void updateQuickReplyView(String editText) {
    if (Log.DEBUG) Log.v("updateQuickReplyView - " + editText);
    if (qrEditText != null && editText != null) {
      qrEditText.setText(editText);
      qrEditText.setSelection(editText.length());
    }
    if (quickreplyTextView != null) {
      quickreplyTextView.setText(
          getString(R.string.quickreply_from_text, quickReplySmsMessage.getContactName()));
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
      // try { Thread.sleep(2000); } catch (InterruptedException e) {}
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