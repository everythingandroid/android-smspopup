package net.everythingandroid.smspopup;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ConfigContactsActivity extends ListActivity {
  private SmsPopupDbAdapter mDbAdapter;

  private static final int DIALOG_MENU_ADD_ID = Menu.FIRST;
  private static final int CONTEXT_MENU_DELETE_ID = Menu.FIRST;
  private static final int CONTEXT_MENU_EDIT_ID = Menu.FIRST + 1;

  private static final int REQ_CODE_CHOOSE_CONTACT = 0;

  private static final int DIALOG_ADD = 1;

  private static ListView mListView;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_PROGRESS);
    setContentView(R.layout.config_contacts_activity);

    ContentResolver content = getContentResolver();
    Cursor cursor =
      content.query(
          Contacts.CONTENT_URI,
          new String[] { Contacts._ID, Contacts.DISPLAY_NAME },
          null,
          null,
          null);
    ContactListAdapter adapter = new ContactListAdapter(this, cursor);

    final AutoCompleteTextView contactsAutoComplete =
      (AutoCompleteTextView) findViewById(R.id.ContactsAutoCompleteTextView);
    contactsAutoComplete.setAdapter(adapter);

    contactsAutoComplete.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivity(getConfigPerContactIntent(id));
        contactsAutoComplete.setText("");
      }
    });

    mListView = getListView();
    mListView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
      public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(0, CONTEXT_MENU_EDIT_ID, 0, R.string.contact_customization_edit);
        menu.add(0, CONTEXT_MENU_DELETE_ID, 0, R.string.contact_customization_remove);
      }
    });
    // registerForContextMenu(mListView);

    mDbAdapter = new SmsPopupDbAdapter(getApplicationContext());

    SynchronizeContactNames mSyncContactNames = new SynchronizeContactNames();
    mSyncContactNames.execute(new Object());
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
        new SimpleCursorAdapter(this, R.layout.simple_list_item_2, c, from, to);
      setListAdapter(contacts);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case REQ_CODE_CHOOSE_CONTACT:
        if (resultCode == -1) { // Success, contact chosen
          Uri contactUri = data.getData();
          List<String> list = contactUri.getPathSegments();
          //if (Log.DEBUG) Log.v("onActivityResult() - " + contactUri);
          if (Log.DEBUG) Log.v("onActivityResult() - " + data.getDataString() + ", " + list.get(list.size() - 1));
          long contactId = Long.parseLong(list.get(list.size() - 1));
          startActivity(getConfigPerContactIntent(contactId));
        }
        break;
    }
  }

  private void selectContact() {
    // Intent i = new Intent(Intent.ACTION_PICK,
    // Uri.parse("content://contacts/people/with_phones_filter/*"));
    // Intent i = new Intent(Intent.ACTION_PICK,
    // Contacts.People.CONTENT_URI.buildUpon().appendEncodedPath("with_phones_filter").build());
    // Contacts.Groups.
    // new Intent(Intent.ACTION_PICK, Contacts.Phones.CONTENT_URI)
    // new Intent(Intent.ACTION_PICK,
    // Uri.withAppendedPath(Contacts.People.CONTENT_URI,"with_phones_filter"))
    // new Intent(Intent.ACTION_PICK,
    // Uri.withAppendedPath(Contacts.Phones.CONTENT_FILTER_URL, "m"))
    // .setType("vnd.android.cursor.dir/phone")
    // .setType("vnd.android.cursor.dir/person")

    // TODO: So ideally we just want to show contacts with phone numbers here
    // but I couldn't
    // work out a way to filter the results using an ACTION_PICK intent

    //final Uri CONTENT_URI = Uri.parse("content://contacts/people_with_phones");

    //    Intent i = new Intent(Intent.ACTION_PICK, Contacts.People.CONTENT_URI);
    //
    //    //i.putExtra(PHONE, "mobile");
    //    startActivityForResult(i, REQ_CODE_CHOOSE_CONTACT);

    startActivityForResult(
        new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI), REQ_CODE_CHOOSE_CONTACT);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuItem m =
      menu.add(Menu.NONE, DIALOG_MENU_ADD_ID, Menu.NONE, R.string.contact_customization_add);
    m.setIcon(android.R.drawable.ic_menu_add);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case DIALOG_MENU_ADD_ID:
        //startActivity(getConfigPerContactIntent());
        selectContact();
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

  // Changed to add context menu only to Activity ListView
  /*
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    if (Log.DEBUG) Log.v("onCreateContextMenu()");

    // Create menu if top item is not selected
    if (((AdapterContextMenuInfo) menuInfo).id != -1) {
      menu.add(0, CONTEXT_MENU_EDIT_ID, 0, R.string.contact_customization_edit);
      menu.add(0, CONTEXT_MENU_DELETE_ID, 0, R.string.contact_customization_remove);
    }
  }
   */

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

    if (Log.DEBUG) Log.v("onContextItemSelected()");

    if (info.id != -1) {
      switch (item.getItemId()) {
        case CONTEXT_MENU_EDIT_ID:
          if (Log.DEBUG) Log.v("Editing contact " + info.id);
          startActivity(getConfigPerContactIntent(info.id));
          return true;
        case CONTEXT_MENU_DELETE_ID:
          if (Log.DEBUG) Log.v("Deleting contact " + info.id);
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
    //    if (position == 0) { // Top item = Add
    //      //startActivity(getConfigPerContactIntent());
    //      showDialog(DIALOG_ADD);
    //    } else {
    startActivity(getConfigPerContactIntent(id));
    //    }
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_ADD:
        return new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setTitle("Add")
        .create();
    }
    return null;
  }

  /**
   * 
   * AsyncTask to sync contact names from our database with those from the
   * system database
   * 
   */
  private class SynchronizeContactNames extends AsyncTask<Object, Integer, Object> {
    private SmsPopupDbAdapter mDbAdapter;
    private Cursor mCursor, sysContactCursor;
    private ContentResolver mContentResolver;
    private int totalCount;

    @Override
    protected void onPreExecute() {
      mDbAdapter = new SmsPopupDbAdapter(getApplicationContext());
      mDbAdapter.open(true);
      mCursor = mDbAdapter.fetchAllContacts();
      if (mCursor == null) {
        totalCount = 0;
      } else {
        ConfigContactsActivity.this.startManagingCursor(mCursor);
        totalCount = mCursor.getCount();
        mContentResolver = ConfigContactsActivity.this.getContentResolver();
      }
    }

    @Override
    protected Bitmap doInBackground(Object... params) {
      if (mCursor != null) {
        int count = 0;
        long contactId;
        String contactName;
        String sysContactName;
        String rawSysContactName;

        // loop through the local sms popup contacts table
        while (mCursor.moveToNext()) {
          count++;

          contactName = mCursor.getString(SmsPopupDbAdapter.KEY_CONTACT_NAME_NUM);
          contactId = mCursor.getLong(SmsPopupDbAdapter.KEY_CONTACT_ID_NUM);
          // Log.v("Name("+count+"): " + contactName);

          // fetch the system db contact name
          sysContactCursor =
            mContentResolver.query(
                Uri.withAppendedPath(Contacts.CONTENT_URI, String.valueOf(contactId)),
                new String[] { Contacts.DISPLAY_NAME },
                null, null, null);

          if (sysContactCursor != null) {
            ConfigContactsActivity.this.startManagingCursor(sysContactCursor);
            if (sysContactCursor.moveToFirst()) {
              rawSysContactName = sysContactCursor.getString(0);
              if (rawSysContactName != null) {
                sysContactName = rawSysContactName.trim();
                if (!contactName.equals(sysContactName)) {
                  // if different, update the local db
                  mDbAdapter.updateContact(contactId, SmsPopupDbAdapter.KEY_CONTACT_NAME,
                      sysContactName);
                }
              }
            } else {
              // if this contact has been removed from the system db then delete
              // from the local db
              mDbAdapter.deleteContact(contactId, false);
            }
            sysContactCursor.close();
          }

          // try { Thread.sleep(3000); } catch (InterruptedException e) {}

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
    protected void onProgressUpdate(Integer... values) {
      getWindow().setFeatureInt(Window.FEATURE_PROGRESS,
          Window.PROGRESS_END * values[0] / totalCount);
    }

    @Override
    protected void onPostExecute(Object result) {
      getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_END);
      if (mCursor != null) mCursor.close();
      if (mDbAdapter != null) mDbAdapter.close();
    }

    @Override
    protected void onCancelled() {
      if (mCursor != null) mCursor.close();
      if (mDbAdapter != null) mDbAdapter.close();
    }
  }

  // XXX compiler bug in javac 1.5.0_07-164, we need to implement Filterable
  // to make compilation work
  public static class ContactListAdapter extends CursorAdapter implements Filterable {
    private ContentResolver mContent;

    public ContactListAdapter(Context context, Cursor c) {
      super(context, c);
      mContent = context.getContentResolver();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
      final LayoutInflater inflater = LayoutInflater.from(context);
      final TextView view =
        (TextView) inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
      view.setText(cursor.getString(1));
      return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
      ((TextView) view).setText(cursor.getString(1));
    }

    @Override
    public String convertToString(Cursor cursor) {
      return cursor.getString(1);
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
      if (getFilterQueryProvider() != null) {
        return getFilterQueryProvider().runQuery(constraint);
      }

      StringBuilder buffer = null;
      String[] args = null;
      if (constraint != null) {
        buffer = new StringBuilder();
        buffer.append("UPPER(");
        buffer.append(Contacts.DISPLAY_NAME);
        buffer.append(") GLOB ?");
        args = new String[] {"*" + constraint.toString().toUpperCase() + "*"};
      }

      return mContent.query(Contacts.CONTENT_URI,
          new String[] { Contacts._ID, Contacts.DISPLAY_NAME },
          buffer == null ? null : buffer.toString(), args, null);
    }
  }
}
