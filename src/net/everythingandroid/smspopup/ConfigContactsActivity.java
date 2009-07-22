package net.everythingandroid.smspopup;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.Contacts.PeopleColumns;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
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
  private static final int DIALOG_PROGRESS = 1;

  /*
   * Eek, 4 static vars, not ideal but only way I could get the ProgressDialog sync'ing nicely
   * with the AsyncTask on orientation changes (whole activity will get destroyed and created
   * again)
   */
  private static ListView mListView;
  private static ProgressDialog mProgressDialog;
  private static int totalCount;
  private static SynchronizeContactNames mSyncContactNames = null;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    requestWindowFeature(Window.FEATURE_PROGRESS);

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

    if (mSyncContactNames == null) {
      mSyncContactNames = new SynchronizeContactNames();
      mSyncContactNames.execute(new Object());
    }
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

    // Create menu if top item is not selected
    if (((AdapterContextMenuInfo)menuInfo).id != -1) {
      menu.add(0, CONTEXT_MENU_EDIT_ID, 0, getString(R.string.contact_customization_edit));
      menu.add(0, CONTEXT_MENU_DELETE_ID, 0, getString(R.string.contact_customization_remove));
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

    Log.v("onContextItemSelected()");

    if (info.id != -1) {
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
    return false;
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

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_PROGRESS:
        mProgressDialog = new ProgressDialog(ConfigContactsActivity.this);
        // TODO: move strings to res
        mProgressDialog.setTitle("Loading contacts...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMax(totalCount);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
        mProgressDialog.setOnCancelListener(new OnCancelListener() {
          public void onCancel(DialogInterface dialog) {
            mSyncContactNames.cancel(true);
          }
        });
        return mProgressDialog;
    }
    return null;
  }

  /**
   * 
   * AsyncTask to sync contact names from our database with those from the system database
   * 
   */
  private class SynchronizeContactNames extends AsyncTask<Object, Integer, Object> {
    private SmsPopupDbAdapter mDbAdapter;
    private Cursor mCursor, sysContactCursor;
    private ContentResolver mContentResolver;

    @Override
    protected Bitmap doInBackground(Object... params) {
      if (mCursor != null) {
        int count = 0;
        long contactId;
        String contactName;
        String sysContactName;

        // loop through the local sms popup contacts table
        while (mCursor.moveToNext()) {
          count++;

          contactName = mCursor.getString(SmsPopupDbAdapter.KEY_CONTACT_NAME_NUM);
          contactId = mCursor.getLong(SmsPopupDbAdapter.KEY_CONTACT_ID_NUM);
          //Log.v("Name("+count+"): " + contactName);

          // fetch the system db contact name
          sysContactCursor = mContentResolver.query(
              Uri.withAppendedPath(Contacts.People.CONTENT_URI, String.valueOf(contactId)),
              new String[] { PeopleColumns.DISPLAY_NAME },
              null, null, null);

          if (sysContactCursor != null) {
            if (sysContactCursor.moveToFirst()) {
              sysContactName = sysContactCursor.getString(0);
              if (!contactName.equals(sysContactName)) {
                // if different, update the local db
                mDbAdapter.updateContact(
                    contactId, SmsPopupDbAdapter.KEY_CONTACT_NAME, sysContactName);
              }
            } else {
              // if this contact has been removed from the system db then delete from the local db
              mDbAdapter.deleteContact(contactId, false);
            }
            sysContactCursor.close();
          }

          //try { Thread.sleep(1000); } catch (InterruptedException e) {}

          // update progress dialog
          publishProgress(count);
        }
      }

      // fill (refresh) the listview with latest data (must run on UI thread)
      runOnUiThread(new Runnable() {
        public void run() {
          fillData();
        }
      });

      return null;
    }

    @Override
    protected void onPreExecute() {
      mDbAdapter = new SmsPopupDbAdapter(getApplicationContext());
      mDbAdapter.open(true);
      mCursor = mDbAdapter.fetchAllContacts();
      if (mCursor == null) {
        totalCount = 0;
      } else {
        totalCount = mCursor.getCount();
        mContentResolver = ConfigContactsActivity.this.getContentResolver();
        showDialog(DIALOG_PROGRESS);
      }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
      mProgressDialog.setProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Object result) {
      if (mCursor != null) mCursor.close();
      if (mDbAdapter != null) mDbAdapter.close();

      mProgressDialog.setProgress(totalCount);
      mProgressDialog.dismiss();
      //dismissDialog(DIALOG_PROGRESS);
    }

    @Override
    protected void onCancelled() {
      if (mCursor != null) mCursor.close();
      if (mDbAdapter != null) mDbAdapter.close();
    }
  }

}
