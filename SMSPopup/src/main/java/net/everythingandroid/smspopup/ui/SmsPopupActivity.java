package net.everythingandroid.smspopup.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v4.util.LruCache;
import android.support.v4.view.PagerAdapter;
import android.telephony.PhoneNumberUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.viewpagerindicator.CirclePageIndicator;

import net.everythingandroid.smspopup.BuildConfig;
import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.controls.FragmentStatePagerAdapter;
import net.everythingandroid.smspopup.controls.QmTextWatcher;
import net.everythingandroid.smspopup.controls.SmsPopupPager;
import net.everythingandroid.smspopup.controls.SmsPopupPager.MessageCountChanged;
import net.everythingandroid.smspopup.preferences.ButtonListPreference;
import net.everythingandroid.smspopup.provider.SmsMmsMessage;
import net.everythingandroid.smspopup.provider.SmsPopupContract.QuickMessages;
import net.everythingandroid.smspopup.receiver.ClearAllReceiver;
import net.everythingandroid.smspopup.service.ReminderService;
import net.everythingandroid.smspopup.service.SmsPopupUtilsService;
import net.everythingandroid.smspopup.ui.SmsPopupFragment.SmsPopupButtonsListener;
import net.everythingandroid.smspopup.util.Eula;
import net.everythingandroid.smspopup.util.Log;
import net.everythingandroid.smspopup.util.ManageKeyguard;
import net.everythingandroid.smspopup.util.ManageKeyguard.LaunchOnKeyguardExit;
import net.everythingandroid.smspopup.util.ManageNotification;
import net.everythingandroid.smspopup.util.ManagePreferences;
import net.everythingandroid.smspopup.util.ManagePreferences.Defaults;
import net.everythingandroid.smspopup.util.ManageWakeLock;
import net.everythingandroid.smspopup.util.RetainFragment;
import net.everythingandroid.smspopup.util.SmsPopupUtils;

import java.util.ArrayList;
import java.util.List;

public class SmsPopupActivity extends FragmentActivity implements SmsPopupButtonsListener {

    private boolean exitingKeyguardSecurely = false;
    private SharedPreferences mPrefs;
    private InputMethodManager inputManager;
    private View inputView;

    private EditText qrEditText;
    private ProgressDialog mProgressDialog;

    private SmsPopupPager smsPopupPager;
    private SmsPopupPagerAdapter smsPopupPagerAdapter;
    private CirclePageIndicator pagerIndicator;
    private LruCache<Uri, Bitmap> mBitmapCache = null;

    private boolean wasVisible = false;
    private boolean replying = false;
    private boolean inbox = false;
    private int privacyMode;
    private boolean privacyAlways = false;
    private boolean showUnlockButton = false;
    private boolean showButtons = true;
    private String signatureText;
    private boolean hasNotified = false;

    private static final int DIALOG_DELETE = Menu.FIRST;
    private static final int DIALOG_QUICKREPLY = Menu.FIRST + 1;
    private static final int DIALOG_PRESET_MSG = Menu.FIRST + 2;
    private static final int DIALOG_LOADING = Menu.FIRST + 3;

    private static final int CONTEXT_CLOSE_ID = Menu.FIRST;
    private static final int CONTEXT_DELETE_ID = Menu.FIRST + 1;
    private static final int CONTEXT_REPLY_ID = Menu.FIRST + 2;
    private static final int CONTEXT_QUICKREPLY_ID = Menu.FIRST + 3;
    private static final int CONTEXT_INBOX_ID = Menu.FIRST + 4;
    private static final int CONTEXT_TTS_ID = Menu.FIRST + 5;
    private static final int CONTEXT_VIEWCONTACT_ID = Menu.FIRST + 6;

    private static final int VOICE_RECOGNITION_REQUEST_CODE = 8888;

    private static final int BITMAP_CACHE_SIZE = 8;

    private TextView quickreplyTextView;
    private SmsMmsMessage quickReplySmsMessage;

    private Cursor mCursor = null;

    private int[] buttonTypes;

    private TextToSpeech androidTts = null;

    /*
     * *****************************************************************************
     * Main onCreate override
     * *****************************************************************************
     */
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.popup);

        setupPreferences();
        setupViews();

        if (bundle == null) { // new activity
            initializeMessagesAndWake(getIntent().getExtras());
        } else { // this activity was recreated after being destroyed
            initializeMessagesAndWake(bundle);
        }

        Eula.show(this);
    }

    /*
     * *****************************************************************************
     * Setup methods - these will mostly be run one time only
     * *****************************************************************************
     */

    private void setupPreferences() {
        // Get shared prefs
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Check if screen orientation should be "user" or "behind" based on prefs
        if (mPrefs.getBoolean(getString(R.string.pref_autorotate_key), Defaults.PREFS_AUTOROTATE)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_BEHIND);
        }

        // Fetch privacy mode
        final boolean privacyMessage =
                mPrefs.getBoolean(getString(R.string.pref_privacy_key), Defaults.PREFS_PRIVACY);
        final boolean privacySender = mPrefs.getBoolean(getString(R.string.pref_privacy_sender_key),
                Defaults.PREFS_PRIVACY_SENDER);
        privacyAlways = mPrefs.getBoolean(getString(R.string.pref_privacy_always_key),
                Defaults.PREFS_PRIVACY_ALWAYS);

        if (privacySender && privacyMessage) {
            privacyMode = SmsPopupFragment.PRIVACY_MODE_HIDE_ALL;
        } else if (privacyMessage) {
            privacyMode = SmsPopupFragment.PRIVACY_MODE_HIDE_MESSAGE;
        } else {
            privacyMode = SmsPopupFragment.PRIVACY_MODE_OFF;
        }

        showUnlockButton = mPrefs.getBoolean(
                getString(R.string.pref_useUnlockButton_key), Defaults.PREFS_USE_UNLOCK_BUTTON);

        // Fetch quick reply signature
        signatureText = mPrefs.getString(getString(R.string.pref_notif_signature_key), "");
        if (signatureText.length() > 0)
            signatureText = " " + signatureText;
    }

    private void setupViews() {

        // Find main views
        smsPopupPager = (SmsPopupPager) findViewById(R.id.SmsPopupPager);
        smsPopupPagerAdapter = new SmsPopupPagerAdapter(getSupportFragmentManager());
        smsPopupPager.setAdapter(smsPopupPagerAdapter);
        pagerIndicator = (CirclePageIndicator) findViewById(R.id.indicator);
        pagerIndicator.setViewPager(smsPopupPager);
        smsPopupPager.setIndicator(pagerIndicator);
        smsPopupPager.setGestureListener(new SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                smsPopupPager.showContextMenu();
            }
        });
        registerForContextMenu(smsPopupPager);

        RetainFragment mRetainFragment =
                RetainFragment.findOrCreateRetainFragment(getSupportFragmentManager());

        mBitmapCache = (LruCache<Uri, Bitmap>) mRetainFragment.getObject();

        // Init cache
        if (mBitmapCache == null) {
            mBitmapCache = new LruCache<Uri, Bitmap>(BITMAP_CACHE_SIZE);
            mRetainFragment.setObject(mBitmapCache);
        }

        smsPopupPager.setOnMessageCountChanged(new MessageCountChanged() {

            @Override
            public void onChange(int current, int total) {
                smsPopupPagerAdapter.notifyDataSetChanged();
                if (total == 1) {
                    pagerIndicator.setVisibility(View.INVISIBLE);
                } else if (total >= 2) {
                    pagerIndicator.setVisibility(View.VISIBLE);
                }

                if (hasNotified) {
                    ManageNotification.update(SmsPopupActivity.this,
                            smsPopupPager.getMessage(current), total);
                }
            }
        });

        // See if user wants to show buttons on the popup
        if (!mPrefs.getBoolean(getString(R.string.pref_show_buttons_key),
                Defaults.PREFS_SHOW_BUTTONS)) {
            showButtons = false;
        } else {

            buttonTypes = new int[] {
                    Integer.parseInt(mPrefs.getString(
                            getString(R.string.pref_button1_key), Defaults.PREFS_BUTTON1)),
                    Integer.parseInt(mPrefs.getString(
                            getString(R.string.pref_button2_key), Defaults.PREFS_BUTTON2)),
                    Integer.parseInt(mPrefs.getString(
                            getString(R.string.pref_button3_key), Defaults.PREFS_BUTTON3)),
            };
        }

        refreshViews();
        resizeLayout();
    }

    private void initializeMessagesAndWake(Bundle b) {
        initializeMessagesAndWake(b, false);
    }

    /**
     * Setup messages within the popup given an intent bundle
     *
     * @param b
     *            the incoming intent bundle
     * @param newIntent
     *            if this is from onNewIntent or not
     */
    private void initializeMessagesAndWake(Bundle b, boolean newIntent) {

        // Create message from bundle
        SmsMmsMessage message = new SmsMmsMessage(getApplicationContext(), b);
        message.locateMessageId();

        if (newIntent) {
            smsPopupPager.addMessage(message);
            wakeApp();
        } else {
            if (message != null) {
                new LoadUnreadMessagesAsyncTask().execute(message);
            }
        }
    }

    private class LoadUnreadMessagesAsyncTask extends AsyncTask<SmsMmsMessage,
            Void, ArrayList<SmsMmsMessage>> {

        ProgressBar mProgressBar;

        @Override
        protected void onPreExecute() {
            mProgressBar = (ProgressBar) findViewById(R.id.progress);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<SmsMmsMessage> doInBackground(SmsMmsMessage... args) {

            final SmsMmsMessage newMessage = args[0];

            // Get all unread messages
            ArrayList<SmsMmsMessage> messages =
                    SmsPopupUtils.getUnreadMessages(SmsPopupActivity.this);

            if (messages == null) {
                // If no messages found, add the new message and we're done
                messages = new ArrayList<SmsMmsMessage>(1);
                messages.add(newMessage);
            } else {
                // Otherwise we now have an array of unread messages
                final long messageId = newMessage.getMessageId();
                // We have to deal with a system race condition now, what if the new message isn't
                // yet in the system database? In that case, messageId will be 0 still and we need
                // to manually add the message to our list.
                if (messageId == 0) {
                    // Add the new message as it wouldn't have been found in the system database
                    messages.add(newMessage);
                } else {
                    // Otherwise we need to check if getUnreadMessages() found the new message,
                    // there's a chance it didn't.
                    boolean found = false;
                    for (int i = 0; i < messages.size(); i++) {
                        if (messages.get(i).getMessageId() == messageId) {
                            // It was found in the getUnreadMessages() query
                            found = true;
                            messages.get(i).setNotify(true);
                            break;
                        }
                    }

                    // See {@link SmsPopupUtils.updateSmscTimestampDrift} for why this is needed.
                    final long smscTimeDrift = mPrefs.getLong(ManagePreferences.SMSC_TIME_DRIFT, 0);
                    if (smscTimeDrift > 0) {
                        for (int i = 0; i < messages.size(); i++) {
                            messages.get(i).adjustTimestamp(smscTimeDrift);
                        }
                    }

                    if (!found) {
                        // If it wasn't found, add it manually to the list
                        messages.add(newMessage);
                    }
                }
            }

            return messages;
        }

        @Override
        protected void onPostExecute(ArrayList<SmsMmsMessage> result) {
            mProgressBar.setVisibility(View.GONE);
            smsPopupPager.addMessages(result);
            smsPopupPager.showLast();
            wakeApp();
        }
    }

    /*
     * *****************************************************************************
     * Methods that will be called several times throughout the life of the activity
     * *****************************************************************************
     */
    private void refreshViews() {
        ManageKeyguard.initialize(this);
        if ((ManageKeyguard.inKeyguardRestrictedInputMode() && showUnlockButton)
                || privacyMode != SmsPopupFragment.PRIVACY_MODE_OFF) {
            unregisterForContextMenu(smsPopupPager);
        } else {
            showUnlockButton = false;
            // Enable long-press context menu
            registerForContextMenu(smsPopupPager);
            smsPopupPagerAdapter.unlockScreen();
        }
    }

    private void resizeLayout() {
        final int width = (int) getResources().getDimension(R.dimen.smspopup_pager_width);
        final int height = (int) getResources().getDimension(R.dimen.smspopup_pager_height);
        final int screenWidth = getResources().getDisplayMetrics().widthPixels;
        final int bottom_padding = (int) getResources().getDimension(R.dimen.smspopup_bottom_margin);
        final View marginView = findViewById(R.id.popup_bottom_margin_view);
        LinearLayout.LayoutParams marginParams =
                (LinearLayout.LayoutParams) marginView.getLayoutParams();
        marginParams.height = bottom_padding;
        marginView.setLayoutParams(marginParams);
        smsPopupPagerAdapter.resizeFragments(width, screenWidth);
        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) smsPopupPager.getLayoutParams();
        params.height = height;
        smsPopupPager.setLayoutParams(params);
        smsPopupPager.invalidate();
    }

    /**
     * Wake up the activity, this will acquire the wakelock (turn on the screen) and sound the
     * notification if needed. This is called once all preparation is done for this activity (end
     * of onCreate()).
     */
    private void wakeApp() {

        // Time to acquire a full WakeLock (turn on screen)
        ManageWakeLock.acquireFull(getApplicationContext());
        ManageWakeLock.releasePartial();

        replying = false;
        inbox = false;

        SmsMmsMessage notifyMessage = smsPopupPager.shouldNotify();

        // See if a notification is needed for this set of messages
        if (notifyMessage != null) {

            // Schedule a reminder notification
            ReminderService.scheduleReminder(this, notifyMessage);

            // Run the notification
            ManageNotification.show(this, notifyMessage, smsPopupPager.getPageCount());

            hasNotified = true;
        }
    }

    /**
     * Customized activity finish. Ensures the notification is in sync and cancels any scheduled
     * reminders (as the user has interrupted the app).
     */
    private void myFinish() {
        if (BuildConfig.DEBUG)
            Log.v("myFinish()");

        if (inbox) {
            ManageNotification.clearAll(getApplicationContext());
        } else {

            // Start a service that will update the notification in the status bar
            Intent intent = new Intent(getApplicationContext(), SmsPopupUtilsService.class);
            intent.setAction(SmsPopupUtilsService.ACTION_UPDATE_NOTIFICATION);

            if (replying) {
                // Convert current message to bundle
                intent.putExtras(smsPopupPager.getActiveMessage().toBundle());

                // We need to know if the user is replying - if so, the entire thread id should
                // be ignored when working out the message tally in the notification bar.
                // We can't rely on the system database as it may take a little while for the
                // reply intent to fire and load up the messaging up (after which the messages
                // will be marked read in the database).
                intent.putExtra(SmsMmsMessage.EXTRAS_REPLYING, replying);
            }

            // Start the service
            WakefulBroadcastReceiver.startWakefulService(getApplicationContext(), intent);
        }

        // Cancel any reminder notifications
        ReminderService.cancelReminder(getApplicationContext());

        // Finish up the activity
        finish();
    }

    /*
     * *****************************************************************************
     * Method overrides from Activity class
     * *****************************************************************************
     */
    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);
        if (BuildConfig.DEBUG)
            Log.v("SMSPopupActivity: onNewIntent()");

        hasNotified = false;

        // Update intent held by activity
        setIntent(intent);

        // Setup messages
        initializeMessagesAndWake(intent.getExtras(), true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (BuildConfig.DEBUG)
            Log.v("SMSPopupActivity: onStart()");
        // ManageWakeLock.acquirePartial(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BuildConfig.DEBUG)
            Log.v("SMSPopupActivity: onResume()");
        wasVisible = false;
        // Reset exitingKeyguardSecurely bool to false
        exitingKeyguardSecurely = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (BuildConfig.DEBUG)
            Log.v("SMSPopupActivity: onPause()");

        // Hide the soft keyboard in case it was shown via quick reply
        hideSoftKeyboard();

        // Shutdown Android TTS
        if (androidTts != null) {
            androidTts.shutdown();
        }

        // Dismiss loading dialog
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }

        if (wasVisible) {
            // Cancel the receiver that will clear our locks
            ClearAllReceiver.removeCancel(getApplicationContext());
            ClearAllReceiver.clearAll(!exitingKeyguardSecurely);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (BuildConfig.DEBUG)
            Log.v("SMSPopupActivity: onStop()");

        // Cancel the receiver that will clear our locks
        ClearAllReceiver.removeCancel(getApplicationContext());
        ClearAllReceiver.clearAll(!exitingKeyguardSecurely);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Create Dialog
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        if (BuildConfig.DEBUG)
            Log.v("onCreateDialog()");

        switch (id) {

        /*
         * Delete message dialog
         */
        case DIALOG_DELETE:
            return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getString(R.string.pref_show_delete_button_dialog_title))
                    .setMessage(getString(R.string.pref_show_delete_button_dialog_text))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            deleteMessage();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();

            /*
             * Quick Reply Dialog
             */
        case DIALOG_QUICKREPLY:
            LayoutInflater factory = getLayoutInflater();
            final View qrLayout = factory.inflate(R.layout.message_quick_reply, null);
            qrEditText = (EditText) qrLayout.findViewById(R.id.QuickReplyEditText);
            final TextView qrCounterTextView =
                    (TextView) qrLayout.findViewById(R.id.QuickReplyCounterTextView);
            final Button qrSendButton = (Button) qrLayout.findViewById(R.id.send_button);

            final ImageButton voiceRecognitionButton =
                    (ImageButton) qrLayout.findViewById(R.id.SpeechRecogButton);

            voiceRecognitionButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

                    // Check if the device has the ability to do speech
                    // recognition
                    final PackageManager packageManager = SmsPopupActivity.this.getPackageManager();
                    List<ResolveInfo> list = packageManager.queryIntentActivities(intent, 0);

                    if (list.size() > 0) {
                        // TODO: really should allow voice input here without unlocking first
                        // (quick replies without unlock are OK anyway)
                        exitingKeyguardSecurely = true;
                        ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
                            @Override
                            public void LaunchOnKeyguardExitSuccess() {
                                SmsPopupActivity.this.startActivityForResult(
                                        intent, VOICE_RECOGNITION_REQUEST_CODE);
                            }
                        });
                    } else {
                        Toast.makeText(SmsPopupActivity.this, R.string.error_no_voice_recognition,
                                Toast.LENGTH_LONG).show();
                        view.setEnabled(false);
                    }
                }
            });

            qrEditText.addTextChangedListener(new QmTextWatcher(this, qrCounterTextView,
                    qrSendButton));
            qrEditText.setOnEditorActionListener(new OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                    // event != null means enter key pressed
                    if (event != null) {
                        // if shift is not pressed then move focus to send button
                        if (!event.isShiftPressed()) {
                            if (v != null) {
                                View focusableView = v.focusSearch(View.FOCUS_RIGHT);
                                if (focusableView != null) {
                                    focusableView.requestFocus();
                                    return true;
                                }
                            }
                        }

                        // otherwise allow keypress through
                        return false;
                    }

                    if (actionId == EditorInfo.IME_ACTION_SEND) {
                        if (v != null) {
                            sendQuickReply(v.getText().toString());
                        }
                        return true;
                    }

                    // else consume
                    return true;
                }
            });

            quickreplyTextView = (TextView) qrLayout.findViewById(R.id.QuickReplyTextView);
            QmTextWatcher.getQuickReplyCounterText(
                    qrEditText.getText().toString(),
                    qrCounterTextView,
                    qrSendButton);

            qrSendButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendQuickReply(qrEditText.getText().toString());
                }
            });

            // Construct basic AlertDialog using AlertDialog.Builder
            final AlertDialog qrAlertDialog = new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_email)
                    .setTitle(R.string.quickreply_title)
                    .create();

            // Set the custom layout with no spacing at the bottom
            qrAlertDialog.setView(qrLayout, 0, SmsPopupUtils.pixelsToDip(getResources(), 5), 0, 0);

            qrAlertDialog.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    storeQuickReplyText(qrEditText.getText().toString());
                    removeDialog(DIALOG_QUICKREPLY);
                }
            });

            // Preset messages button
            Button presetButton = (Button) qrLayout.findViewById(R.id.PresetMessagesButton);
            presetButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDialog(DIALOG_PRESET_MSG);
                }
            });

            // Cancel button
            Button cancelButton = (Button) qrLayout.findViewById(R.id.CancelButton);
            cancelButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (qrAlertDialog != null) {
                        hideSoftKeyboard();
                        qrAlertDialog.dismiss();
                        removeDialog(DIALOG_QUICKREPLY);
                    }
                }
            });

            // Ensure this dialog is counted as "editable" (so soft keyboard
            // will always show on top)
            qrAlertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

            qrAlertDialog.setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (BuildConfig.DEBUG)
                        Log.v("Quick Reply Dialog: onDissmiss()");
                    storeQuickReplyText(qrEditText.getText().toString());
                }
            });

            // Update quick reply views now that they have been created
            if (quickReplySmsMessage != null) {
                updateQuickReplyView(quickReplySmsMessage.getReplyText());
            } else {
                updateQuickReplyView("");
            }

            return qrAlertDialog;

            /*
             * Preset messages dialog
             */
        case DIALOG_PRESET_MSG:
            mCursor = getContentResolver().query(QuickMessages.CONTENT_URI, null, null, null, null);
            startManagingCursor(mCursor);

            AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_email)
                    .setTitle(R.string.pref_message_presets_title);

            // If user has some presets defined ...
            if (mCursor != null && mCursor.getCount() > 0) {

                mDialogBuilder.setCursor(mCursor, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (BuildConfig.DEBUG)
                            Log.v("Item clicked = " + item);
                        mCursor.moveToPosition(item);
                        quickReply(mCursor.getString(
                                mCursor.getColumnIndexOrThrow(QuickMessages.QUICKMESSAGE)));
                    }
                }, QuickMessages.QUICKMESSAGE);
            } else { // Otherwise display a placeholder as user has no presets
                MatrixCursor emptyCursor =
                        new MatrixCursor(new String[] { QuickMessages._ID,
                                QuickMessages.QUICKMESSAGE });
                emptyCursor.addRow(new String[] { "0",
                        getString(R.string.message_presets_empty_text) });
                mDialogBuilder.setCursor(emptyCursor, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {}
                }, QuickMessages.QUICKMESSAGE);
            }

            return mDialogBuilder.create();

            /*
             * Loading Dialog
             */
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

        if (BuildConfig.DEBUG)
            Log.v("onPrepareDialog()");

        // User interacted so remove all locks and cancel reminders
        ClearAllReceiver.removeCancel(getApplicationContext());
        ClearAllReceiver.clearAll(false);
        ReminderService.cancelReminder(getApplicationContext());

        switch (id) {
        case DIALOG_QUICKREPLY:
            // Update dialog width to same size as popup message, this is incase the screen
            // orientation changes and the dialog is left filling the whole width of the screen.
            final LayoutParams quickreplyLP = dialog.getWindow().getAttributes();
            quickreplyLP.width = (int) getResources().getDimension(R.dimen.smspopup_pager_width);
            dialog.getWindow().setAttributes(quickreplyLP);
            showSoftKeyboard(qrEditText);
            break;
        case DIALOG_PRESET_MSG:
            // Update dialog width to same size as popup message, this is incase the screen
            // orientation changes and the dialog is left filling the whole width of the screen.
            final LayoutParams presetLP = dialog.getWindow().getAttributes();
            presetLP.width = (int) getResources().getDimension(R.dimen.smspopup_pager_width);
            dialog.getWindow().setAttributes(presetLP);
            break;
        }
    }

    /**
     * Handle the results from the recognition activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (BuildConfig.DEBUG)
            Log.v("onActivityResult");
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (BuildConfig.DEBUG)
                Log.v("Voice recog text: " + matches.get(0));
            quickReply(matches.get(0));
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (BuildConfig.DEBUG)
            Log.v("SMSPopupActivity: onWindowFocusChanged(" + hasFocus + ")");
        if (hasFocus) {
            // This is really hacky, basically a flag that is set if the message was at some
            // point visible. I tried using onResume() or other methods to prevent doing some
            // things 2 times but this seemed to be the only reliable way (?)
            wasVisible = true;
            refreshViews();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (BuildConfig.DEBUG)
            Log.v("SMSPopupActivity: onSaveInstanceState()");

        // Save values from most recent bundle (ie. most recent message)
//        outState.putAll(bundle);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (BuildConfig.DEBUG)
            Log.v("SMSPopupActivity: onRestoreInstanceState()");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (BuildConfig.DEBUG)
            Log.v("SMSPopupActivity: onConfigurationChanged()");
        resizeLayout();
    }

    /**
     * Create Context Menu (Long-press menu)
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(Menu.NONE, CONTEXT_VIEWCONTACT_ID, Menu.NONE, getString(R.string.view_contact));
        menu.add(Menu.NONE, CONTEXT_CLOSE_ID, Menu.NONE, getString(R.string.button_close));
        menu.add(Menu.NONE, CONTEXT_DELETE_ID, Menu.NONE, getString(R.string.button_delete));
        menu.add(Menu.NONE, CONTEXT_REPLY_ID, Menu.NONE, getString(R.string.button_reply));
        menu.add(Menu.NONE, CONTEXT_QUICKREPLY_ID, Menu.NONE, getString(R.string.button_quickreply));
        menu.add(Menu.NONE, CONTEXT_TTS_ID, Menu.NONE, getString(R.string.button_tts));
        menu.add(Menu.NONE, CONTEXT_INBOX_ID, Menu.NONE, getString(R.string.button_inbox));
    }

    /**
     * Context Menu Item Selected
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case CONTEXT_CLOSE_ID:
            closeMessage();
            break;
        case CONTEXT_DELETE_ID:
            showDialog(DIALOG_DELETE);
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

    // The Android text-to-speech library OnInitListener (via wrapper class)
    private final OnInitListener androidTtsListener = new OnInitListener() {
        @Override
        public void onInit(int status) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            if (status == TextToSpeech.SUCCESS) {
                speakMessage();
            } else {
                Toast.makeText(SmsPopupActivity.this, R.string.error_message, Toast.LENGTH_SHORT);
            }
        }
    };

    /*
     * *****************************************************************************
     * Methods to handle messages (speak, close, reply, quick reply etc.)
     * *****************************************************************************
     */

    /**
     * Speak the message out loud using text-to-speech (either via Android text-to-speech or via the
     * free eyes-free text-to-speech library)
     */
    private void speakMessage() {
        // TODO: we should really require the keyguard be unlocked here if we are in privacy mode

        // If not previously initialized...
        if (androidTts == null) {

            // Show a loading dialog
            showDialog(DIALOG_LOADING);

            // User interacted so remove all locks and cancel reminders
            ClearAllReceiver.removeCancel(getApplicationContext());
            ClearAllReceiver.clearAll(false);
            ReminderService.cancelReminder(getApplicationContext());

            // We'll use update notification to stop the sound playing
            ManageNotification.update(
                    this, smsPopupPager.getActiveMessage(), smsPopupPager.getPageCount());

            androidTts = new TextToSpeech(SmsPopupActivity.this, androidTtsListener);

        } else {
            androidTts.speak(smsPopupPager.getActiveMessage().getMessageBody(),
                    TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    /**
     * Close the message window/popup, mark the message read if the user has this option on
     */
    private void closeMessage() {
        Intent intent = new Intent(getApplicationContext(), SmsPopupUtilsService.class);
        /*
         * Switched back to mark messageId as read for >v1.0.6 (marking thread as read is slow for
         * really large threads)
         */
        intent.setAction(SmsPopupUtilsService.ACTION_MARK_MESSAGE_READ);
        intent.putExtras(smsPopupPager.getActiveMessage().toBundle());
        WakefulBroadcastReceiver.startWakefulService(getApplicationContext(), intent);

        removeActiveMessage();
    }

    /**
     * Reply to the current message, start the reply intent
     */
    private void replyToMessage(final SmsMmsMessage message, final boolean replyToThread) {
        exitingKeyguardSecurely = true;
        ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
            @Override
            public void LaunchOnKeyguardExitSuccess() {
                startActivity(message.getReplyIntent());
                replying = true;
                myFinish();
            }
        });
    }

    private void replyToMessage(SmsMmsMessage message) {
        replyToMessage(message, true);
    }

    private void replyToMessage(boolean replyToThread) {
        replyToMessage(smsPopupPager.getActiveMessage(), replyToThread);
    }

    private void replyToMessage() {
        replyToMessage(smsPopupPager.getActiveMessage());
    }

    /**
     * View the private message (this basically just unlocks the keyg   uard and then updates the
     * privacy of the messages).
     */
    private void unlockScreen() {
        exitingKeyguardSecurely = true;
        ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
            @Override
            public void LaunchOnKeyguardExitSuccess() {
                SmsPopupActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setPrivacy(SmsPopupFragment.PRIVACY_MODE_OFF);
                    }
                });
            }
        });
    }

    /**
     * Take the user to the messaging app inbox
     */
    private void gotoInbox() {
        exitingKeyguardSecurely = true;
        ManageKeyguard.exitKeyguardSecurely(new LaunchOnKeyguardExit() {
            @Override
            public void LaunchOnKeyguardExitSuccess() {
                Intent i = SmsPopupUtils.getSmsInboxIntent(SmsPopupActivity.this);
                SmsPopupActivity.this.getApplicationContext().startActivity(i);
                inbox = true;
                myFinish();
            }
        });
    }

    /**
     * Delete the current message from the system database
     */
    private void deleteMessage() {
        Intent intent = new Intent(SmsPopupActivity.this.getApplicationContext(),
                SmsPopupUtilsService.class);
        intent.setAction(SmsPopupUtilsService.ACTION_DELETE_MESSAGE);
        intent.putExtras(smsPopupPager.getActiveMessage().toBundle());
        WakefulBroadcastReceiver.startWakefulService(getApplicationContext(), intent);
        removeActiveMessage();
    }

    /**
     * Sends the actual quick reply message
     */
    private void sendQuickReply(String quickReplyMessage) {
        hideSoftKeyboard();
        if (quickReplyMessage != null) {
            if (quickReplyMessage.length() > 0 && quickReplySmsMessage != null) {
                Intent i = new Intent(SmsPopupActivity.this.getApplicationContext(),
                        SmsPopupUtilsService.class);
                i.setAction(SmsPopupUtilsService.ACTION_QUICKREPLY);
                i.putExtras(quickReplySmsMessage.toBundle());
                i.putExtra(SmsMmsMessage.EXTRAS_QUICKREPLY, quickReplyMessage);
                if (BuildConfig.DEBUG)
                    Log.v("Sending message to " + quickReplySmsMessage.getContactName());
                WakefulBroadcastReceiver.startWakefulService(getApplicationContext(), i);
                Toast.makeText(this, R.string.quickreply_sending_toast, Toast.LENGTH_LONG).show();
                dismissDialog(DIALOG_QUICKREPLY);
                removeActiveMessage();
            } else {
                Toast.makeText(this, R.string.quickreply_nomessage_toast, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Show the quick reply dialog, resetting the text in the edittext and storing the current
     * SmsMmsMessage (in case another message comes in)
     */
    private void quickReply() {
        quickReplySmsMessage = smsPopupPager.getActiveMessage();
        quickReply(quickReplySmsMessage.getReplyText());
    }

    /**
     * Show the quick reply dialog, if text passed is null or empty then store the current
     * SmsMmsMessage (in case another message comes in)
     */
    private void quickReply(String text) {
        // If this is a MMS or a SMS from email gateway or we're on KitKat then use regular reply
        if (quickReplySmsMessage != null) {
            if (quickReplySmsMessage.isMms() || quickReplySmsMessage.isEmail()) {
                replyToMessage();
            } else { // Else show the quick reply dialog
                updateQuickReplyView(text);
                showDialog(DIALOG_QUICKREPLY);
            }
        }
    }

    /**
     * View contact that has the message address (or create if it doesn't exist)
     */
    private void viewContact() {
        Intent contactIntent = new Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT);

        final String address = smsPopupPager.getActiveMessage().getAddress();
        final boolean fromEmail = smsPopupPager.getActiveMessage().isEmail();
        if (address != null) {
            if (PhoneNumberUtils.isWellFormedSmsAddress(address)) {
                contactIntent.setData(Uri.fromParts("tel", address, null));
            } else if (fromEmail) {
                contactIntent.setData(Uri.fromParts("mailto", address, null));
            }
        }

        if (contactIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(contactIntent);
        }
    }

    /**
     * Refresh the quick reply view - update the edittext and the counter
     */
    private void updateQuickReplyView(String editText) {
        if (BuildConfig.DEBUG) Log.v("updateQuickReplyView - '" + editText + "'");
        if (qrEditText != null && editText != null) {
            qrEditText.setText(editText + signatureText);
            qrEditText.setSelection(editText.length());
        }
        if (quickreplyTextView != null && quickReplySmsMessage != null) {
            quickreplyTextView.setText(getString(R.string.quickreply_from_text,
                    quickReplySmsMessage.getContactName()));
        }
    }

    private void storeQuickReplyText(String text) {
        if (text != null) {
            // Remove signature if present
            if (signatureText != null && !"".equals(signatureText) &&
                    text.endsWith(signatureText)) {
                smsPopupPager.getActiveMessage().setReplyText(
                        text.substring(0, text.lastIndexOf(signatureText)));
            } else {
                smsPopupPager.getActiveMessage().setReplyText(text);
            }
        }
    }

    /**
     * Removes the active message
     */
    private void removeActiveMessage() {
        final int status = smsPopupPager.removeActiveMessage();
        if (status == SmsPopupPager.STATUS_NO_MESSAGES_REMAINING)  {
            myFinish();
        }
    }

    /*
     * *****************************************************************************
     * Misc methods
     * *****************************************************************************
     */

    /**
     * Show the soft keyboard and store the view that triggered it
     */
    private void showSoftKeyboard(View triggerView) {
        if (BuildConfig.DEBUG) Log.v("showSoftKeyboard()");
        if (inputManager == null) {
            inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        inputView = triggerView;
        inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    /**
     * Hide the soft keyboard
     */
    private void hideSoftKeyboard() {
        if (inputView == null)
            return;
        if (BuildConfig.DEBUG) Log.v("hideSoftKeyboard()");
        if (inputManager == null) {
            inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        inputManager.hideSoftInputFromWindow(inputView.getApplicationWindowToken(), 0);
        inputView = null;
    }

    private class SmsPopupPagerAdapter extends FragmentStatePagerAdapter {

        public SmsPopupPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return smsPopupPager.getPageCount();
        }

        @Override
        public Fragment getItem(int position) {
            return SmsPopupFragment.newInstance(
                    smsPopupPager.getMessage(position), buttonTypes, privacyMode,
                    showUnlockButton, showButtons);
        }

        public void setPrivacy(int privacyMode) {
            for (int i=0; i<mFragments.size(); i++) {
                if (mFragments.get(i) != null) {
                    ((SmsPopupFragment) mFragments.get(i)).setPrivacy(privacyMode);
                }
            }
        }

        public void unlockScreen() {
            for (int i=0; i<mFragments.size(); i++) {
                if (mFragments.get(i) != null) {
                    ((SmsPopupFragment) mFragments.get(i)).setShowUnlockButton(false);
                }
            }
        }

        @Override
        public int getItemPosition(Object object) {
            int idx = smsPopupPager.getMessages().indexOf(object);
            if (idx == -1) {
                return PagerAdapter.POSITION_NONE;
            }
            return idx;
        }

        public void resizeFragments(int width, int height) {
            for (int i=0; i<mFragments.size(); i++) {
                if (mFragments.get(i) != null) {
                    ((SmsPopupFragment) mFragments.get(i)).resizeLayout(width, height);
                }
            }
        }
    }

    private void setPrivacy(int mode) {
        privacyMode = mode;
        smsPopupPagerAdapter.setPrivacy(privacyMode);
        refreshViews();
    }

    @Override
    public void onButtonClicked(int buttonType) {
        switch (buttonType) {
        case ButtonListPreference.BUTTON_DISABLED: // Disabled
            break;
        case ButtonListPreference.BUTTON_CLOSE: // Close
            closeMessage();
            break;
        case ButtonListPreference.BUTTON_DELETE: // Delete
            showDialog(DIALOG_DELETE);
            break;
        case ButtonListPreference.BUTTON_DELETE_NO_CONFIRM:
            // Delete no confirmation
            deleteMessage();
            break;
        case ButtonListPreference.BUTTON_REPLY: // Reply
            replyToMessage(true);
            break;
        case ButtonListPreference.BUTTON_QUICKREPLY: // Quick Reply
            quickReply();
            break;
        case ButtonListPreference.BUTTON_REPLY_BY_ADDRESS: // Quick Reply
            replyToMessage(false);
            break;
        case ButtonListPreference.BUTTON_INBOX: // Inbox
            gotoInbox();
            break;
        case ButtonListPreference.BUTTON_TTS: // Text-to-Speech
            speakMessage();
            break;
        case SmsPopupFragment.BUTTON_VIEW:
            unlockScreen();
            break;
        case SmsPopupFragment.BUTTON_VIEW_MMS:
            replyToMessage();
            break;
        case SmsPopupFragment.BUTTON_UNLOCK:
            unlockScreen();
            break;
        }
    }

    @Override
    public LruCache<Uri, Bitmap> getCache() {
        return mBitmapCache;
    }
}
