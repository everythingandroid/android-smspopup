package net.everythingandroid.smspopup;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ConfigContactsActivity extends ListActivity {
  private SmsPopupDbAdapter mDbAdapter;

  private static final int DIALOG_MENU_ADD_ID = Menu.FIRST;

  private static final int CONTEXT_MENU_DELETE_ID = Menu.FIRST;
  private static final int CONTEXT_MENU_EDIT_ID = Menu.FIRST + 1;
  private static ListView mListView;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mListView = getListView();
    registerForContextMenu(mListView);

    // TODO: make this look better!
    TextView tv = new TextView(getApplicationContext());
    tv.setText(getString(R.string.contact_customization_add));
    tv.setTextSize(25);
    tv.setPadding(10, 10, 10, 10);

    mListView.addHeaderView(tv, null, true);
    mDbAdapter = new SmsPopupDbAdapter(getApplicationContext());
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

  private void fillData() {
    // Get all of the contacts from the database and create the item list
    mDbAdapter.open(true);
    Cursor c = mDbAdapter.fetchAllContacts();
    startManagingCursor(c);
    // mDbAdapter.close();
    if (c != null) {
      String[] from =
        new String[] {SmsPopupDbAdapter.KEY_CONTACT_NAME, SmsPopupDbAdapter.KEY_SUMMARY};
      int[] to = new int[] {android.R.id.text1, android.R.id.text2};

      // Now create an array adapter and set it to display using our row
      SimpleCursorAdapter contacts =
        new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, c, from, to);
      setListAdapter(contacts);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuItem m =
      menu.add(Menu.NONE, DIALOG_MENU_ADD_ID, Menu.NONE,
          getString(R.string.contact_customization_add));
    m.setIcon(android.R.drawable.ic_menu_add);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case DIALOG_MENU_ADD_ID:
        startActivity(getConfigPerContactIntent());
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * Get intent that starts the notification config for a contact - no params
   * means add new (ie. it will prompt for the user to pick a contact.
   * 
   * @return the intent that can be started
   */
  private Intent getConfigPerContactIntent() {
    Intent i = new Intent(getApplicationContext(), ConfigPerContactActivity.class);
    // i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
    // Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    return i;
  }

  /**
   * Get intent that starts the notification config for a contact. contactId is
   * the system id of the contact to edit
   * 
   * @return the intent that can be started
   */
  private Intent getConfigPerContactIntent(long contactId) {
    Intent i = getConfigPerContactIntent();
    i.putExtra(ConfigPerContactActivity.EXTRA_CONTACT_ID, contactId);
    return i;
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    Log.v("onCreateContextMenu()");

    menu.add(0, CONTEXT_MENU_EDIT_ID, 0, getString(R.string.contact_customization_edit));
    menu.add(0, CONTEXT_MENU_DELETE_ID, 0, getString(R.string.contact_customization_remove));
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

    Log.v("onContextItemSelected()");

    switch (item.getItemId()) {
      case CONTEXT_MENU_EDIT_ID:
        Log.v("Editing contact " + info.id);
        startActivity(getConfigPerContactIntent(info.id));
        return true;
      case CONTEXT_MENU_DELETE_ID:
        Log.v("Deleting contact " + info.id);
        mDbAdapter.deleteContact(info.id);
        fillData();
        return true;
      default:
        return super.onContextItemSelected(item);
    }
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    if (position == 0) { // Top item = Add
      startActivity(getConfigPerContactIntent());
    } else {
      startActivity(getConfigPerContactIntent(id));
    }
  }

}
