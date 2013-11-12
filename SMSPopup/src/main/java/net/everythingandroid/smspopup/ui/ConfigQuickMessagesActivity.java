package net.everythingandroid.smspopup.ui;

import net.everythingandroid.smspopup.BuildConfig;
import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.controls.QmTextWatcher;
import net.everythingandroid.smspopup.provider.SmsPopupContract.QuickMessages;
import net.everythingandroid.smspopup.util.Log;
import net.everythingandroid.smspopup.util.SmsPopupUtils;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class ConfigQuickMessagesActivity extends ListActivity implements OnEditorActionListener {
    private static final int ADD_DIALOG = Menu.FIRST;
    private static final int EDIT_DIALOG = Menu.FIRST + 1;

    private static final int CONTEXT_MENU_DELETE_ID = Menu.FIRST;
    private static final int CONTEXT_MENU_EDIT_ID = Menu.FIRST + 1;
    private static final int CONTEXT_MENU_REORDER_ID = Menu.FIRST + 2;

    private static String editId;
    private EditText addQMEditText;
    private EditText editQMEditText;
    private View addQMLayout;
    private View editQMLayout;
    private TextView addQMTextView;
    private TextView editQMTextView;
    private InputMethodManager inputManager;

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        registerForContextMenu(getListView());
        
        if (SmsPopupUtils.hasHoneycomb()) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (!SmsPopupUtils.hasICS()) {
            final TextView tv = new TextView(this);
            tv.setText(R.string.message_presets_add);
            tv.setTextSize(SmsPopupUtils.pixelsToDip(getResources(), 13));
            final int tvPadding = SmsPopupUtils.pixelsToDip(getResources(), 10);
            tv.setPadding(tvPadding, tvPadding, tvPadding, tvPadding);
            getListView().addHeaderView(tv, null, true);
        }

        LayoutInflater factory = LayoutInflater.from(this);

        addQMLayout = factory.inflate(R.layout.message_presets_configure, null);
        editQMLayout = factory.inflate(R.layout.message_presets_configure, null);

        addQMEditText = (EditText) addQMLayout.findViewById(R.id.QuickReplyEditText);
        editQMEditText = (EditText) editQMLayout.findViewById(R.id.QuickReplyEditText);

        addQMTextView = (TextView) addQMLayout.findViewById(R.id.QuickReplyCounterTextView);
        editQMTextView = (TextView) editQMLayout.findViewById(R.id.QuickReplyCounterTextView);

        addQMEditText.addTextChangedListener(new QmTextWatcher(this, addQMTextView));
        editQMEditText.addTextChangedListener(new QmTextWatcher(this, editQMTextView));

        addQMEditText.setOnEditorActionListener(this);
        editQMEditText.setOnEditorActionListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fillData();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (position == 0 && !SmsPopupUtils.hasICS()) { // Top item = Add
            showDialog(ADD_DIALOG);
        } else {
            editId = String.valueOf(id);
            showDialog(EDIT_DIALOG);
        }
    }

    /*
     * Create options menu (shown when user presses "menu")
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.config_quickmessages, menu);
        return true;
    }

    /*
     * Options menu item selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        case R.id.add_menu_item:
            showDialog(ADD_DIALOG);
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * Create context menu (long-press menu)
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (BuildConfig.DEBUG)
            Log.v("onCreateContextMenu()");

        // Create menu if top item is not selected
        if (((AdapterContextMenuInfo) menuInfo).id != -1) {
            menu.add(0, CONTEXT_MENU_EDIT_ID, 0, R.string.message_presets_edit_text);
            menu.add(0, CONTEXT_MENU_DELETE_ID, 0, R.string.message_presets_delete_text);
            menu.add(0, CONTEXT_MENU_REORDER_ID, 0, R.string.message_presets_reorder_text);
        }
    }

    /*
     * Context menu item selected
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        if (BuildConfig.DEBUG)
            Log.v("onContextItemSelected()");
        if (info.id != -1) {
            final String id = String.valueOf(info.id);
            switch (item.getItemId()) {
            case CONTEXT_MENU_EDIT_ID:
                if (BuildConfig.DEBUG)
                    Log.v("Editing quick message " + id);
                editId = id;
                showDialog(EDIT_DIALOG);
                return true;
            case CONTEXT_MENU_DELETE_ID:
                if (BuildConfig.DEBUG)
                    Log.v("Deleting quickmessage " + id);
                deleteQuickMessage(id);
                return true;
            case CONTEXT_MENU_REORDER_ID:
                if (BuildConfig.DEBUG)
                    Log.v("Reordering quickmessage " + id);
                reorderQuickMessage(id);
                return true;
            default:
                return super.onContextItemSelected(item);
            }
        }
        return false;
    }

    /*
     * Create Dialogs
     */
    @Override
    protected Dialog onCreateDialog(int id) {
    	OnDismissListener dismissListener = new OnDismissListener () {

			@Override
			public void onDismiss(DialogInterface dialog) {
				Log.v("dismissed!");
				hideSoftKeyboard();
			}
    	};
    	
        switch (id) {
        case ADD_DIALOG:
        	final AlertDialog addDialog = new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_email)
                    .setTitle(R.string.message_presets_add)
                    .setView(addQMLayout)
                    .setPositiveButton(R.string.message_presets_add_text,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    createQuickMessage(addQMEditText.getText().toString());
                                }
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setOnCancelListener(new OnCancelListener() {

						@Override
						public void onCancel(DialogInterface dialog) {
							Log.v("canceled!");
//							hideSoftKeyboard();							
						}
                    	
                    })
                    .create();
        	addDialog.setOnDismissListener(dismissListener);
        	return addDialog;

        case EDIT_DIALOG:
        	final AlertDialog editDialog = new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_email)
                    .setTitle(R.string.message_presets_edit)
                    .setView(editQMLayout)
                    .setPositiveButton(R.string.message_presets_save_text,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    updateQuickMessage(editId, editQMEditText.getText().toString());
                                }
                            })
                    .setNeutralButton(getString(R.string.message_presets_delete_text),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    deleteQuickMessage(editId);
                                }
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setOnCancelListener(new OnCancelListener() {

						@Override
						public void onCancel(DialogInterface dialog) {
							Log.v("canceled!");
//							hideSoftKeyboard();							
						}
                    	
                    })
                    .create();
        	editDialog.setOnDismissListener(dismissListener);
        	return editDialog;
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        switch (id) {
        case ADD_DIALOG:
            addQMEditText.setText("");
            addQMEditText.requestFocus();
            showSoftKeyboard();
            break;
        case EDIT_DIALOG:
            updateEditText(editId);
            editQMEditText.requestFocus();
            showSoftKeyboard();
            break;
        }
    }

    private void fillData() {
        Cursor c = getContentResolver().query(QuickMessages.CONTENT_URI, null, null, null, null);
        startManagingCursor(c);
        if (c != null) {
            String[] from = new String[] { QuickMessages.QUICKMESSAGE, QuickMessages._ID };
            int[] to = new int[] { android.R.id.text1 };

            // Now create an array adapter and set it to display using our row
            SimpleCursorAdapter mCursorAdapter =
                    new SimpleCursorAdapter(this, R.layout.simple_list_item_1_medium, c, from, to);

            setListAdapter(mCursorAdapter);
        }
    }

    private void updateEditText(String id) {
        Cursor c = getContentResolver().query(
                QuickMessages.buildQuickMessageUri(id), null, null, null, null);
        if (c != null && c.moveToFirst()) {
            CharSequence message = c.getString(c.getColumnIndexOrThrow(QuickMessages.QUICKMESSAGE));
            editQMEditText.setText(message);
            editQMEditText.setSelection(message.length());
        } else {
            editQMEditText.setText("");
        }
        
        if (c != null) {
            c.close();
        }
    }

    private boolean updateQuickMessage(String id, String message) {
        if (message.trim().length() == 0)
            return false;

        final ContentValues vals = new ContentValues();
        vals.put(QuickMessages.QUICKMESSAGE, message);
        final int rows = getContentResolver().update(
        		QuickMessages.buildQuickMessageUri(id), vals, null, null);
        final boolean result = rows == 1 ? true : false;
        if (result) {
            myToast(R.string.message_presets_save_toast);
        } else {
            myToast(R.string.message_presets_error_toast);
        }
        return result;
    }

    private boolean deleteQuickMessage(String id) {
        final int rows =
                getContentResolver().delete(QuickMessages.buildQuickMessageUri(id), null, null);
        final boolean result = rows == 1 ? true : false;
        if (result) {
            myToast(R.string.message_presets_delete_toast);
        } else {
            myToast(R.string.message_presets_error_toast);
        }
        return result;
    }

    private String createQuickMessage(String message) {
        if (message.trim().length() == 0)
            return null;

        final ContentValues vals = new ContentValues();
        vals.put(QuickMessages.QUICKMESSAGE, message);
        final Uri resultUri = getContentResolver().insert(QuickMessages.CONTENT_URI, vals);
        if (resultUri == null) {
            myToast(R.string.message_presets_error_toast);
            return null;
        } else {
            myToast(R.string.message_presets_add_toast);
        }
        return QuickMessages.getQuickMessageId(resultUri);
    }

    private boolean reorderQuickMessage(String id) {
    	if (1 == getContentResolver().update(QuickMessages.buildQuickMessageOrderUpdateUri(id), null, null, null)) {
    		myToast(R.string.message_presets_reorder_toast);
    		return true;
    	}
    	myToast(R.string.message_presets_error_toast);
    	return false;
    }

    private void myToast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        // event != null means enter key pressed
        if (event != null) {
            // if shift is not pressed then move focus to send button
            if (!event.isShiftPressed()) {
                if (v != null) {
                    View focusableView = v.focusSearch(View.FOCUS_DOWN);
                    if (focusableView != null) {
                        focusableView.requestFocus();
                        return true;
                    }
                }
            }
        }

        // otherwise allow keypress through
        return false;
    }
    
    /**
     * Show the soft keyboard and store the view that triggered it
     */
    private void showSoftKeyboard() {
        if (inputManager == null) {
            inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }
    
    /**
     * Hide the soft keyboard
     */
    private void hideSoftKeyboard() {
    	getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
}