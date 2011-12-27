package net.everythingandroid.smspopup.ui;

import java.util.List;

import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.provider.SmsPopupContract.ContactNotifications;
import net.everythingandroid.smspopup.util.Log;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import android.widget.TextView;

public class ConfigContactsActivity extends FragmentActivity {
    private static final int REQ_CODE_CHOOSE_CONTACT = 0;
    
    private static final String[] CONTACT_PROJECTION = new String[] {
        Contacts._ID,
        Contacts.DISPLAY_NAME,
        Contacts.LOOKUP_KEY
    };

    private static final int COLUMN_DISPLAY_NAME = 1;
    private static final int COLUMN_LOOKUP_KEY = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(android.R.id.content, ConfigContactsListFragment.newInstance());
        ft.commit();

//        new SynchronizeContactNames().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case REQ_CODE_CHOOSE_CONTACT:
            if (resultCode == -1) { // Success, contact chosen
                final List<String> segments = data.getData().getPathSegments();
                final String lookupKey = segments.get(segments.size() - 2);
                startActivity(getConfigPerContactIntent(getApplicationContext(), 
                        ContactNotifications.buildLookupUri(lookupKey)));
            }
            break;
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.config_contacts, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.add_menu_item:
            startContactPicker();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startContactPicker() {
        startActivityForResult(
                new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI), REQ_CODE_CHOOSE_CONTACT);
    }

    /**
     * Get intent that starts the notification config for a contact - no params means add new (ie.
     * it will prompt for the user to pick a contact).
     * 
     * @return the intent that can be started
     */
    private static Intent getConfigPerContactIntent(Context context) {
        Intent i = new Intent(context, ConfigContactActivity.class);
        return i;
    }

    /**
     * Get intent that starts the notification config for a contact. contactId is the system id of
     * the contact to edit
     * 
     * @return the intent that can be started
     */
    private static Intent getConfigPerContactIntent(Context context, long rowId) {
        Intent i = getConfigPerContactIntent(context);
        i.putExtra(ConfigContactActivity.EXTRA_CONTACT_URI, 
                ContactNotifications.buildContactUri(rowId));
        return i;
    }
    
    private static Intent getConfigPerContactIntent(Context context, Uri uri) {
        Intent i = getConfigPerContactIntent(context);
        i.putExtra(ConfigContactActivity.EXTRA_CONTACT_URI, uri);
        return i;
    }    

    /**
     * AsyncTask to sync contact names from our database with those from the system database
     */
    private class SynchronizeContactNames extends AsyncTask<Void, Integer, Void> {
        private Cursor mCursor, sysContactCursor;
        private ContentResolver mContentResolver;
        private int totalCount;

        @Override
        protected void onPreExecute() {
            mContentResolver = getContentResolver();
        }

        @Override
        protected Void doInBackground(Void... params) {
            mCursor = mContentResolver.query(
                    ContactNotifications.CONTENT_URI, null, null, null, null);

            totalCount = 0;
            if (mCursor != null) {
                totalCount = mCursor.getCount();
            }

            if (totalCount == 0) {
                return null;
            }

            // ConfigContactsActivity.this.startManagingCursor(mCursor);

            if (mCursor != null) {
                int count = 0;
                String contactId;
                String contactName;
                String sysContactName;
                String rawSysContactName;

                // loop through the local sms popup contacts table
                while (mCursor.moveToNext()) {
                    count++;

                    contactName = mCursor.getString(
                            mCursor.getColumnIndexOrThrow(ContactNotifications.CONTACT_NAME));
                    contactId = mCursor.getString(
                            mCursor.getColumnIndexOrThrow(ContactNotifications._ID));

                    // fetch the system db contact name
                    sysContactCursor = mContentResolver.query(
                            Uri.withAppendedPath(Contacts.CONTENT_URI, contactId),
                            new String[] { Contacts.DISPLAY_NAME }, null, null, null);

                    if (sysContactCursor != null) {
                        // ConfigContactsActivity.this.startManagingCursor(sysContactCursor);
                        if (sysContactCursor.moveToFirst()) {
                            rawSysContactName = sysContactCursor.getString(0);
                            if (rawSysContactName != null) {
                                sysContactName = rawSysContactName.trim();
                                if (!contactName.equals(sysContactName)) {
                                    ContentValues vals = new ContentValues();
                                    vals.put(ContactNotifications.CONTACT_NAME, sysContactName);
                                    mContentResolver.update(ContactNotifications
                                            .buildContactUri(contactId), vals, null, null);
                                }
                            }
                        } else {
                            // if this contact has been removed from the system db then delete from
                            // the local db
                            mContentResolver.delete(
                                    ContactNotifications.buildContactUri(contactId), null, null);
                        }
                        sysContactCursor.close();
                    }

                    // try { Thread.sleep(3000); } catch (InterruptedException e) {}

                    // update progress dialog
                    publishProgress(count);
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            getWindow().setFeatureInt(Window.FEATURE_PROGRESS,
                    Window.PROGRESS_END * values[0] / totalCount);
        }

        @Override
        protected void onPostExecute(Void result) {
            getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_END);
            if (mCursor != null)
                mCursor.close();
        }

        @Override
        protected void onCancelled() {
            if (mCursor != null)
                mCursor.close();
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
                    (TextView) inflater.inflate(android.R.layout.simple_dropdown_item_1line,
                            parent, false);
            view.setText(cursor.getString(COLUMN_DISPLAY_NAME));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view).setText(cursor.getString(COLUMN_DISPLAY_NAME));
        }

        @Override
        public String convertToString(Cursor cursor) {
            return cursor.getString(COLUMN_DISPLAY_NAME);
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            if (getFilterQueryProvider() != null) {
                return getFilterQueryProvider().runQuery(constraint);
            }
            
            return mContent.query(
                    Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, (String) constraint), 
                    CONTACT_PROJECTION, null, null, null);
        }
    }

    public static class ConfigContactsListFragment extends ListFragment implements
            LoaderCallbacks<Cursor> {

        private SimpleCursorAdapter mAdapter;

        private static final int CONTEXT_MENU_DELETE_ID = Menu.FIRST;
        private static final int CONTEXT_MENU_EDIT_ID = Menu.FIRST + 1;

        public ConfigContactsListFragment() {}

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            super.onListItemClick(l, v, position, id);
            startActivity(getConfigPerContactIntent(getActivity(), id));
        }

        public static ConfigContactsListFragment newInstance() {
            return new ConfigContactsListFragment();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            final String[] from =
                    new String[] { ContactNotifications.CONTACT_NAME, ContactNotifications.SUMMARY };
            final int[] to = new int[] { android.R.id.text1, android.R.id.text2 };

            // Now create an array adapter and set it to display using our row
            mAdapter = new SimpleCursorAdapter(
                    getActivity(), R.layout.simple_list_item_2, null, from, to, 0);

            setListAdapter(mAdapter);

            getLoaderManager().initLoader(0, null, this);
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

            if (Log.DEBUG) Log.v("onContextItemSelected()");

            if (info.id != -1) {
                switch (item.getItemId()) {
                case CONTEXT_MENU_EDIT_ID:
                    if (Log.DEBUG)
                        Log.v("Editing contact " + info.id);
                    startActivity(getConfigPerContactIntent(getActivity(), info.id));
                    return true;
                case CONTEXT_MENU_DELETE_ID:
                    if (Log.DEBUG)
                        Log.v("Deleting contact " + info.id);
                    getActivity().getContentResolver().delete(
                            ContactNotifications.buildContactUri(info.id), null,
                            null);
                    return true;
                default:
                    return super.onContextItemSelected(item);
                }
            }
            return false;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            final View v = inflater.inflate(R.layout.config_contacts_fragment, container, false);

            final ListView mListView = (ListView) v.findViewById(android.R.id.list);
            mListView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                    menu.add(0, CONTEXT_MENU_EDIT_ID, 0, R.string.contact_customization_edit);
                    menu.add(0, CONTEXT_MENU_DELETE_ID, 0, R.string.contact_customization_remove);
                }
            });

            ContentResolver content = getActivity().getContentResolver();
            Cursor cursor =
                    content.query(Contacts.CONTENT_URI, CONTACT_PROJECTION, null, null, null);
            final ContactListAdapter adapter = new ContactListAdapter(getActivity(), cursor);

            final AutoCompleteTextView contactsAutoComplete =
                    (AutoCompleteTextView) v.findViewById(R.id.ContactsAutoCompleteTextView);
            contactsAutoComplete.setAdapter(adapter);

            contactsAutoComplete.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final Cursor c = (Cursor) adapter.getItem(position);
                    final Uri uri = Uri.withAppendedPath(
                            ContactNotifications.CONTENT_LOOKUP_URI, c.getString(COLUMN_LOOKUP_KEY));
                    startActivity(getConfigPerContactIntent(getActivity(), uri));
                    contactsAutoComplete.setText("");
                }
            });

            return v;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            menu.add(0, CONTEXT_MENU_EDIT_ID, 0, R.string.contact_customization_edit);
            menu.add(0, CONTEXT_MENU_DELETE_ID, 0, R.string.contact_customization_remove);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(), ContactNotifications.CONTENT_URI,
                    ContactNotifications.PROJECTION_SUMMARY, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mAdapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loarder) {
            mAdapter.swapCursor(null);
        }
    }
}
