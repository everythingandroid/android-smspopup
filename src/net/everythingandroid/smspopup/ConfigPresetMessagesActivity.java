package net.everythingandroid.smspopup;

import net.everythingandroid.smspopup.controls.QmTextWatcher;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView.OnEditorActionListener;

public class ConfigPresetMessagesActivity extends ListActivity implements OnEditorActionListener {
  private SmsPopupDbAdapter mDbAdapter;

  private static final int ADD_DIALOG = Menu.FIRST;
  private static final int EDIT_DIALOG = Menu.FIRST + 1;

  private static final int DIALOG_MENU_ADD_ID = Menu.FIRST;

  private static final int CONTEXT_MENU_DELETE_ID = Menu.FIRST;
  private static final int CONTEXT_MENU_EDIT_ID = Menu.FIRST + 1;
  private static final int CONTEXT_MENU_REORDER_ID = Menu.FIRST + 2;
  private static ListView mListView;

  private static long editId;
  private EditText addQMEditText;
  private EditText editQMEditText;
  private View addQMLayout;
  private View editQMLayout;
  private TextView addQMTextView;
  private TextView editQMTextView;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mListView = getListView();
    registerForContextMenu(mListView);

    TextView tv = new TextView(getApplicationContext());

    // TODO: make this look better
    tv.setText(R.string.message_presets_add);
    tv.setTextSize(22);
    tv.setPadding(10, 10, 10, 10);

    mListView.addHeaderView(tv, null, true);
    mDbAdapter = new SmsPopupDbAdapter(getApplicationContext());
    mDbAdapter.open(true);

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
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    mDbAdapter.close();
    super.onDestroy();
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    if (position == 0) { // Top item = Add
      showDialog(ADD_DIALOG);
    } else {
      editId = id;
      showDialog(EDIT_DIALOG);
    }
  }

  /*
   * Create options menu (shown when user presses "menu")
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuItem m = menu.add(Menu.NONE, DIALOG_MENU_ADD_ID, Menu.NONE, R.string.message_presets_add);
    m.setIcon(android.R.drawable.ic_menu_add);
    return super.onCreateOptionsMenu(menu);
  }

  /*
   * Options menu item selected
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case DIALOG_MENU_ADD_ID:
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
    if (Log.DEBUG) Log.v("onCreateContextMenu()");

    // Create menu if top item is not selected
    if (((AdapterContextMenuInfo)menuInfo).id != -1) {
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
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    if (Log.DEBUG) Log.v("onContextItemSelected()");
    if (info.id != -1) {
      switch (item.getItemId()) {
        case CONTEXT_MENU_EDIT_ID:
          if (Log.DEBUG) Log.v("Editing quick message " + info.id);
          editId = info.id;
          showDialog(EDIT_DIALOG);
          return true;
        case CONTEXT_MENU_DELETE_ID:
          if (Log.DEBUG) Log.v("Deleting quickmessage " + info.id);
          deleteQuickMessage(info.id);
          return true;
        case CONTEXT_MENU_REORDER_ID:
          if (Log.DEBUG) Log.v("Reordering quickmessage " + info.id);
          reorderQuickMessage(info.id);
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
    switch (id) {
      case ADD_DIALOG:
        return new AlertDialog.Builder(this)
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
        .create();

      case EDIT_DIALOG:
        return new AlertDialog.Builder(this)
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
        .create();
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
        break;
      case EDIT_DIALOG:
        updateEditText(editId);
        editQMEditText.requestFocus();
        break;
    }
  }

  private void fillData() {
    Cursor c = mDbAdapter.fetchAllQuickMessages();
    startManagingCursor(c);
    if (c != null) {
      String[] from =
        new String[] {SmsPopupDbAdapter.KEY_QUICKMESSAGE, SmsPopupDbAdapter.KEY_ROWID};
      int[] to = new int[] {android.R.id.text1};
      // int[] to = new int[] { android.R.id.text1, android.R.id.text2 };


      // Now create an array adapter and set it to display using our row
      SimpleCursorAdapter mCursorAdapter =
        new SimpleCursorAdapter(this, R.layout.list_view, c, from, to);
      //new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, c, from, to);
      //android.R.layout.simple_list_item_1

      setListAdapter(mCursorAdapter);
    }
  }

  private void updateEditText(long id) {
    Cursor c = mDbAdapter.fetchQuickMessage(id);
    if (c != null) {
      CharSequence message = c.getString(SmsPopupDbAdapter.KEY_QUICKMESSAGE_NUM);
      editQMEditText.setText(message);
      editQMEditText.setSelection(message.length());
      c.close();
    } else {
      editQMEditText.setText("");
    }
  }

  private boolean updateQuickMessage(long id, String message) {
    if (message.trim().length() == 0) return false;

    boolean result = mDbAdapter.updateQuickMessage(id, message);
    fillData();
    if (result) {
      myToast(R.string.message_presets_save_toast);
    } else {
      myToast(R.string.message_presets_error_toast);
    }
    return result;
  }

  private boolean deleteQuickMessage(long id) {
    boolean result = mDbAdapter.deleteQuickMessage(id);
    fillData();
    if (result) {
      myToast(R.string.message_presets_delete_toast);
    } else {
      myToast(R.string.message_presets_error_toast);
    }
    return result;
  }

  private long createQuickMessage(String message) {
    if (message.trim().length() == 0) return -1;

    long result = mDbAdapter.createQuickMessage(message);
    fillData();
    if (result == -1) {
      myToast(R.string.message_presets_error_toast);
    } else {
      myToast(R.string.message_presets_add_toast);
    }
    return result;
  }

  private boolean reorderQuickMessage(long id) {
    boolean result = mDbAdapter.reorderQuickMessage(id);
    fillData();
    if (result) {
      myToast(R.string.message_presets_reorder_toast);
    } else {
      myToast(R.string.message_presets_error_toast);
    }
    return result;
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
}