package net.everythingandroid.smspopup;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
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

public class ConfigQuickMessagesActivity extends ListActivity {
  private SmsPopupDbAdapter mDbAdapter;

  private static final int ADD_DIALOG = Menu.FIRST;
  private static final int EDIT_DIALOG = Menu.FIRST + 1;

  private static final int DIALOG_MENU_ADD_ID = Menu.FIRST;

  private static final int CONTEXT_MENU_DELETE_ID = Menu.FIRST;
  private static final int CONTEXT_MENU_EDIT_ID = Menu.FIRST + 1;
  private static final int CONTEXT_MENU_REORDER_ID = Menu.FIRST + 2;
  private static ListView mListView;

  private static long editId;
  private EditText qmEditText;
  private EditText addEditText;
  private View editQuickMessageLayout;
  private View addQuickMessageLayout;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mListView = getListView();
    registerForContextMenu(mListView);

    TextView tv = new TextView(getApplicationContext());
    tv.setText("Add New");
    tv.setTextSize(25);
    tv.setPadding(10, 10, 10, 10);

    mListView.addHeaderView(tv, null, true);
    mDbAdapter = new SmsPopupDbAdapter(getApplicationContext());

    LayoutInflater factory = LayoutInflater.from(this);
    addQuickMessageLayout = factory.inflate(R.layout.quickmessage, null);
    editQuickMessageLayout = factory.inflate(R.layout.quickmessage, null);
    qmEditText = (EditText) editQuickMessageLayout.findViewById(R.id.QuickReplyEditText);
    addEditText = (EditText) addQuickMessageLayout.findViewById(R.id.QuickReplyEditText);
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.v("SMSPopupConfigContactsActivity: onResume()");
    fillData();
  }

  @Override
  protected void onPause() {
    super.onPause();
    Log.v("SMSPopupConfigContactsActivity: onPause()");
  }

  @Override
  protected void onStop() {
    super.onStop();
    Log.v("SMSPopupConfigContactsActivity: onStop()");
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Log.v("SMSPopupConfigContactsActivity: onDestroy()");
    mDbAdapter.close();
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    // TODO: move top position to constant
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
    MenuItem m = menu.add(Menu.NONE, DIALOG_MENU_ADD_ID, Menu.NONE, "Add");
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
    Log.v("onCreateContextMenu()");
    menu.add(0, CONTEXT_MENU_EDIT_ID, 0, getString(R.string.contact_customization_edit));
    menu.add(0, CONTEXT_MENU_DELETE_ID, 0, getString(R.string.contact_customization_remove));
    menu.add(0, CONTEXT_MENU_REORDER_ID, 0, "Move to top");
  }

  /*
   * Context menu item selected
   */
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    Log.v("onContextItemSelected()");
    switch (item.getItemId()) {
      case CONTEXT_MENU_EDIT_ID:
        Log.v("Editing quick message " + info.id);
        editId = info.id;
        showDialog(EDIT_DIALOG);
        return true;
      case CONTEXT_MENU_DELETE_ID:
        Log.v("Deleting quickmessage " + info.id);
        deleteQuickMessage(info.id);
        return true;
      case CONTEXT_MENU_REORDER_ID:
        Log.v("Reordering quickmessage " + info.id);
        reorderQuickMessage(info.id);
        return true;
      default:
        return super.onContextItemSelected(item);
    }
  }

  /*
   * Create Dialogs
   */
  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case ADD_DIALOG:
        return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_email).setTitle(
        "Add Quick Message").setView(addQuickMessageLayout).setPositiveButton("Create",
            new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            createQuickMessage(addEditText.getText().toString());
          }
        }).setNegativeButton(android.R.string.cancel, null).create();

      case EDIT_DIALOG:
        return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_email).setTitle(
        "Edit Quick Message").setView(editQuickMessageLayout).setPositiveButton("Save",
            new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            updateQuickMessage(editId, qmEditText.getText().toString());
          }
        }).setNeutralButton(getString(R.string.contact_customization_remove),
            new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            deleteQuickMessage(editId);
          }
        }).setNegativeButton(android.R.string.cancel, null).create();
    }
    return null;
  }

  @Override
  protected void onPrepareDialog(int id, Dialog dialog) {
    super.onPrepareDialog(id, dialog);
    switch (id) {
      case ADD_DIALOG:
        addEditText.setText("");
        break;
      case EDIT_DIALOG:
        updateEditText(editId);
        break;
    }
  }

  private void fillData() {
    // Get all of the notes from the database and create the item list
    mDbAdapter.open(true);
    Cursor c = mDbAdapter.fetchAllQuickMessages();
    startManagingCursor(c);
    if (c != null) {
      // if (c.getCount() > 0) {
      String[] from =
        new String[] {SmsPopupDbAdapter.KEY_QUICKMESSAGE, SmsPopupDbAdapter.KEY_ROWID};
      // int[] to = new int[] { android.R.id.text1, android.R.id.text2 };
      int[] to = new int[] {android.R.id.text1};

      // Now create an array adapter and set it to display using our row
      SimpleCursorAdapter quickmessages =
        new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, c, from, to);
      setListAdapter(quickmessages);
      // }
    }
  }

  private void updateEditText(long id) {
    Cursor c = mDbAdapter.fetchQuickMessage(id);
    if (c != null) {
      CharSequence message = c.getString(SmsPopupDbAdapter.KEY_QUICKMESSAGE_NUM);
      qmEditText.setText(message);
      qmEditText.setSelection(message.length());
      c.close();
    } else {
      qmEditText.setText("");
    }
  }

  private boolean updateQuickMessage(long id, String message) {
    boolean result = mDbAdapter.updateQuickMessage(id, message);
    fillData();
    if (result) {
      myToast("Updated");
    } else {
      myToast("Error");
    }
    return result;
  }

  private boolean deleteQuickMessage(long id) {
    boolean result = mDbAdapter.deleteQuickMessage(id);
    fillData();
    if (result) {
      myToast("Removed");
    } else {
      myToast("Error");
    }
    return result;
  }

  private long createQuickMessage(String message) {
    long result = mDbAdapter.createQuickMessage(message);
    fillData();
    if (result == -1) {
      myToast("Error");
    } else {
      myToast("Created");
    }
    return result;
  }

  private boolean reorderQuickMessage(long id) {
    boolean result = mDbAdapter.reorderQuickMessage(id);
    fillData();
    if (result) {
      myToast("Reordered");
    } else {
      myToast("Error");
    }
    return result;
  }

  private void myToast(CharSequence toast) {
    Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
  }
}
