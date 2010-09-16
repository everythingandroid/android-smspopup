package net.everythingandroid.smspopup.preferences;

import net.everythingandroid.smspopup.ManageNotification;
import net.everythingandroid.smspopup.R;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;

/**
 * A {@link Preference} that displays a list of entries as
 * a dialog.
 * <p>
 * This preference will store a string into the SharedPreferences. This string will be the value
 * from the {@link #setEntryValues(CharSequence[])} array.
 *
 * @attr ref android.R.styleable#ListPreference_entries
 * @attr ref android.R.styleable#ListPreference_entryValues
 */
public class NotificationIconListPreference extends DialogPreference {
  private CharSequence[] mEntries;
  private CharSequence[] mEntryValues;
  private String mValue;
  private String mSummary;
  private int mClickedDialogEntryIndex;
  private Context context;
  private MyObj[] listViewItems;

  public NotificationIconListPreference(Context _context, AttributeSet attrs) {
      super(_context, attrs);

      context = _context;

      mEntries = context.getResources().getStringArray(R.array.pref_notif_icon_entries);
      mEntryValues = context.getResources().getStringArray(R.array.pref_notif_icon_values);
      mSummary = context.getResources().getString(R.string.pref_notif_icon_summary);

      listViewItems = new MyObj[mEntries.length];
      for (int i=0; i<mEntries.length; i++) {
        listViewItems[i] = new MyObj(mEntries[i], ManageNotification.NOTIF_ICON_RES[i][0]);
      }

      //android.R.layout.simple_list_ite//single_choice
//  simple_list_item_single_choice      TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ListPreference, 0, 0);
//      a.gett

//        TypedArray a = context.obtainStyledAttributes(attrs,
//                com.android.internal.R.styleable.ListPreference, 0, 0);
//        mEntries = a.getTextArray(com.android.internal.R.styleable.ListPreference_entries);
//        mEntryValues = a.getTextArray(com.android.internal.R.styleable.ListPreference_entryValues);
//        a.recycle();
//
//        /* Retrieve the Preference summary attribute since it's private
//         * in the Preference class.
//         */
//        a = context.obtainStyledAttributes(attrs,
//                com.android.internal.R.styleable.Preference, 0, 0);
//        mSummary = a.getString(com.android.internal.R.styleable.Preference_summary);
//        a.recycle();
  }

  public NotificationIconListPreference(Context context) {
      this(context, null);
  }

  /**
   * Sets the human-readable entries to be shown in the list. This will be
   * shown in subsequent dialogs.
   * <p>
   * Each entry must have a corresponding index in
   * {@link #setEntryValues(CharSequence[])}.
   *
   * @param entries The entries.
   * @see #setEntryValues(CharSequence[])
   */
  public void setEntries(CharSequence[] entries) {
      mEntries = entries;
  }

  /**
   * @see #setEntries(CharSequence[])
   * @param entriesResId The entries array as a resource.
   */
  public void setEntries(int entriesResId) {
      setEntries(getContext().getResources().getTextArray(entriesResId));
  }

  /**
   * The list of entries to be shown in the list in subsequent dialogs.
   *
   * @return The list as an array.
   */
  public CharSequence[] getEntries() {
      return mEntries;
  }

  /**
   * The array to find the value to save for a preference when an entry from
   * entries is selected. If a user clicks on the second item in entries, the
   * second item in this array will be saved to the preference.
   *
   * @param entryValues The array to be used as values to save for the preference.
   */
  public void setEntryValues(CharSequence[] entryValues) {
      mEntryValues = entryValues;
  }

  /**
   * @see #setEntryValues(CharSequence[])
   * @param entryValuesResId The entry values array as a resource.
   */
  public void setEntryValues(int entryValuesResId) {
      setEntryValues(getContext().getResources().getTextArray(entryValuesResId));
  }

  /**
   * Returns the array of values to be saved for the preference.
   *
   * @return The array of values.
   */
  public CharSequence[] getEntryValues() {
      return mEntryValues;
  }

  /**
   * Sets the value of the key. This should be one of the entries in
   * {@link #getEntryValues()}.
   *
   * @param value The value to set for the key.
   */
  public void setValue(String value) {
      mValue = value;

      persistString(value);
  }

  /**
   * Returns the summary of this ListPreference. If the summary
   * has a {@linkplain java.lang.String#format String formatting}
   * marker in it (i.e. "%s" or "%1$s"), then the current entry
   * value will be substituted in its place.
   *
   * @return the summary with appropriate string substitution
   */
  @Override
  public CharSequence getSummary() {
      final CharSequence entry = getEntry();
      if (mSummary == null || entry == null) {
          return super.getSummary();
      } else {
          return String.format(mSummary, entry);
      }
  }

  /**
   * Sets the summary for this Preference with a CharSequence.
   * If the summary has a
   * {@linkplain java.lang.String#format String formatting}
   * marker in it (i.e. "%s" or "%1$s"), then the current entry
   * value will be substituted in its place when it's retrieved.
   *
   * @param summary The summary for the preference.
   */
  @Override
  public void setSummary(CharSequence summary) {
      super.setSummary(summary);
      if (summary == null && mSummary != null) {
          mSummary = null;
      } else if (summary != null && !summary.equals(mSummary)) {
          mSummary = summary.toString();
      }
  }

  /**
   * Sets the value to the given index from the entry values.
   *
   * @param index The index of the value to set.
   */
  public void setValueIndex(int index) {
      if (mEntryValues != null) {
          setValue(mEntryValues[index].toString());
      }
  }

  /**
   * Returns the value of the key. This should be one of the entries in
   * {@link #getEntryValues()}.
   *
   * @return The value of the key.
   */
  public String getValue() {
      return mValue;
  }

  /**
   * Returns the entry corresponding to the current value.
   *
   * @return The entry corresponding to the current value, or null.
   */
  public CharSequence getEntry() {
      int index = getValueIndex();
      return index >= 0 && mEntries != null ? mEntries[index] : null;
  }

  /**
   * Returns the index of the given value (in the entry values array).
   *
   * @param value The value whose index should be returned.
   * @return The index of the value, or -1 if not found.
   */
  public int findIndexOfValue(String value) {
      if (value != null && mEntryValues != null) {
          for (int i = mEntryValues.length - 1; i >= 0; i--) {
              if (mEntryValues[i].equals(value)) {
                  return i;
              }
          }
      }
      return -1;
  }

  private int getValueIndex() {
      return findIndexOfValue(mValue);
  }

  @Override
  protected void onPrepareDialogBuilder(Builder builder) {
      super.onPrepareDialogBuilder(builder);

      if (mEntries == null || mEntryValues == null) {
          throw new IllegalStateException(
                  "ListPreference requires an entries array and an entryValues array.");
      }

      mClickedDialogEntryIndex = getValueIndex();

//        builder.setSingleChoiceItems(mEntries, mClickedDialogEntryIndex,
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        mClickedDialogEntryIndex = which;
//
//                        /*
//                         * Clicking on an item simulates the positive button
//                         * click, and dismisses the dialog.
//                         */
//                        NotificationIconListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
//                        dialog.dismiss();
//                    }
//        });

      builder.setSingleChoiceItems(
          //new MyCustomAdapter(context, R.layout.notification_icon_listview_row, android.R.id.text1, (String[]) mEntries),
          //new MyCustomAdapter(context, android.R.layout.simple_list_item_1, android.R.id.text1, (String[]) mEntries),
          //new ArrayAdapter<CharSequence>(context, android.R.layout.simple_list_item_single_choice, android.R.id.text1, mEntries),
          new MyCustomAdapter(context, R.layout.notification_icon_listview_row, listViewItems, mClickedDialogEntryIndex),
          mClickedDialogEntryIndex,
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              mClickedDialogEntryIndex = which;

              /*
               * Clicking on an item simulates the positive button
               * click, and dismisses the dialog.
               */
              NotificationIconListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
              dialog.dismiss();
            }
      });

      /*
       * The typical interaction for list-based dialogs is to have
       * click-on-an-item dismiss the dialog instead of the user having to
       * press 'Ok'.
       */
      builder.setPositiveButton(null, null);
  }

  @Override
  protected void onDialogClosed(boolean positiveResult) {
      super.onDialogClosed(positiveResult);

      if (positiveResult && mClickedDialogEntryIndex >= 0 && mEntryValues != null) {
          String value = mEntryValues[mClickedDialogEntryIndex].toString();
          if (callChangeListener(value)) {
              setValue(value);
          }
      }
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
      return a.getString(index);
  }

  @Override
  protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
      setValue(restoreValue ? getPersistedString(mValue) : (String) defaultValue);
  }

  @Override
  protected Parcelable onSaveInstanceState() {
      final Parcelable superState = super.onSaveInstanceState();
      if (isPersistent()) {
          // No need to save instance state since it's persistent
          return superState;
      }

      final SavedState myState = new SavedState(superState);
      myState.value = getValue();
      return myState;
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
      if (state == null || !state.getClass().equals(SavedState.class)) {
          // Didn't save state for us in onSaveInstanceState
          super.onRestoreInstanceState(state);
          return;
      }

      SavedState myState = (SavedState) state;
      super.onRestoreInstanceState(myState.getSuperState());
      setValue(myState.value);
  }

  private static class SavedState extends BaseSavedState {
      String value;

      public SavedState(Parcel source) {
          super(source);
          value = source.readString();
      }

      @Override
      public void writeToParcel(Parcel dest, int flags) {
          super.writeToParcel(dest, flags);
          dest.writeString(value);
      }

      public SavedState(Parcelable superState) {
          super(superState);
      }

      public static final Parcelable.Creator<SavedState> CREATOR =
              new Parcelable.Creator<SavedState>() {
          public SavedState createFromParcel(Parcel in) {
              return new SavedState(in);
          }

          public SavedState[] newArray(int size) {
              return new SavedState[size];
          }
      };
  }

  private class MyObj {
    public CharSequence text;
    public int iconResId;

    public MyObj(CharSequence t, int i) {
     text = t;
     iconResId = i;
    }
  }

  private static class ViewHolder {
    CheckedTextView text;
    ImageView icon;
  }

  public class MyCustomAdapter extends ArrayAdapter<MyObj> {
    private Context context;
    private int selected;

    //public MyCustomAdapter(Context _context, int resource, int textViewResourceId, MyObj[] objects) {
    public MyCustomAdapter(Context _context, int textViewResourceId, MyObj[] objects, int s) {
      //super( _context,  resource,  textViewResourceId, objects);
      super(_context, textViewResourceId, objects);
      context = _context;
      selected = s;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      ViewHolder holder;

      if (convertView == null) {
        LayoutInflater inflater =
          (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.notification_icon_listview_row, null);

        holder = new ViewHolder();
        holder.text = (CheckedTextView) convertView.findViewById(android.R.id.text1);
        holder.icon = (ImageView) convertView.findViewById(android.R.id.icon);

        convertView.setTag(holder);
      } else {
        holder = (ViewHolder) convertView.getTag();
      }

      holder.text.setText(getItem(position).text);
      holder.icon.setImageResource(getItem(position).iconResId);

      if (position == selected) {
        holder.text.setChecked(true);
      }

      return convertView;
    }
  }

}