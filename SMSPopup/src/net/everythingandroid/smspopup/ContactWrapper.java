package net.everythingandroid.smspopup;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.PeopleColumns;
import android.provider.Contacts.Photos;

public class ContactWrapper {

  // Reflection variables
  private static Class<?> contactsClass;
  private static Method contactsOpenPhotoStreamMethod;
  private static boolean preparedEclairSDK = false;

  private static boolean PRE_ECLAIR =
    SmsPopupUtils.getSDKVersionNumber() < SmsPopupUtils.SDK_VERSION_ECLAIR ? true : false;

  // Projection columns
  private static final String CONTACT_ID = Contacts.People._ID;
  private static final String CONTACT_DISPLAY_NAME = PeopleColumns.DISPLAY_NAME;
  private static final String CONTACT_PERSON_ID = Contacts.Phones.PERSON_ID;
  private static final String CONTACT_ID_ECLAIR = "_id";
  private static final String CONTACT_DISPLAY_NAME_ECLAIR = "display_name";
  private static final String CONTACT_ID_EMAIL_ECLAIR = "contact_id";

  // Projection column ids
  public static final int COL_CONTACT_ID = 0;
  public static final int COL_DISPLAY_NAME = 1;
  public static final int COL_CONTACT_ID_EMAIL = 2;
  public static final int COL_CONTACT_PERSON_ID = 3;

  // Projections
  private static final String[] PEOPLE_PROJECTION =
    new String[] { CONTACT_ID, CONTACT_DISPLAY_NAME };

  private static final String[] PEOPLE_PROJECTION_ECLAIR =
    new String[] { CONTACT_ID_ECLAIR, CONTACT_DISPLAY_NAME_ECLAIR };

  // Don't allow this class to be instantiated
  private ContactWrapper() {}

  /**
   * Fetch regular contact content URI
   */
  public static Uri getContentUri() {
    if (PRE_ECLAIR) return Contacts.People.CONTENT_URI;

    // ContactsContract.Contacts.CONTENT_URI
    return Uri.parse("content://com.android.contacts/contacts");
  }

  /**
   * Fetch phone lookup content filter URI
   */
  public static Uri getPhoneLookupContentFilterUri() {
    if (PRE_ECLAIR) return Contacts.Phones.CONTENT_FILTER_URL;

    // ContactsContract.PhoneLookup.CONTENT_FILTER_URI
    return Uri.parse("content://com.android.contacts/phone_lookup");
  }

  /**
   * Fetch email lookup content filter URI
   */
  public static Uri getEmailLookupContentFilterUri() {
    if (PRE_ECLAIR) return Uri.parse("content://contacts/people/with_email_or_im_filter");

    return Uri.parse("content://com.android.contacts/data/emails/lookup");
  }

  /**
   * Fetch default sort order for contacts
   */
  public static String getDefaultSortOrder() {
    if (PRE_ECLAIR) return Contacts.People.DEFAULT_SORT_ORDER;

    // TODO: couldn't find what replaces this in 2.0 SDK
    return null;
  }

  /**
   * Fetch base contact projection (id and display name)
   */
  public static String[] getBasePeopleProjection() {
    if (PRE_ECLAIR) return PEOPLE_PROJECTION;

    return PEOPLE_PROJECTION_ECLAIR;
  }

  /**
   * Fetch contact table column
   */
  public static String getColumn(int col) {
    if (PRE_ECLAIR) {
      switch(col) {
        case COL_CONTACT_ID:
          return CONTACT_ID;

        case COL_DISPLAY_NAME:
          return CONTACT_DISPLAY_NAME;

        case COL_CONTACT_ID_EMAIL:
          return CONTACT_ID;

        case COL_CONTACT_PERSON_ID:
          return CONTACT_PERSON_ID;
      }

      return null;
    }

    switch(col) {
      case COL_CONTACT_ID:
        return CONTACT_ID_ECLAIR;

      case COL_DISPLAY_NAME:
        return CONTACT_DISPLAY_NAME_ECLAIR;

      case COL_CONTACT_ID_EMAIL:
        return CONTACT_ID_EMAIL_ECLAIR;

      case COL_CONTACT_PERSON_ID:
        return CONTACT_ID_ECLAIR;
    }

    return null;
  }

  /**
   * Returns an InputStream for the person's photo
   * @param id the id of the person
   */
  public static InputStream openContactPhotoInputStream(ContentResolver cr, String id) {
    if (id == null) return null;
    if ("0".equals(id)) return null;

    Cursor cursor;
    Uri photoUri;

    // If we're pre-Eclair then lookup the contact using the standard URIs
    if (PRE_ECLAIR) {

      /*
       * Contacts.People.CONTENT_URI is "content://contacts/people"
       * Contacts.Photos.CONTENT_DIRECTORY is "photo";
       * Uri will end up being "content://contacts/people/#contactId/photo"
       */
      if (Log.DEBUG) Log.v("openContactPhotoInputStream(): looking in Contacts.People");
      photoUri = Uri.withAppendedPath(
          Uri.withAppendedPath(Contacts.People.CONTENT_URI, id),
          Contacts.Photos.CONTENT_DIRECTORY);

      cursor = cr.query(photoUri, new String[] {Photos.DATA}, null, null, null);

      if (cursor != null) {
        try {
          if (cursor.moveToFirst()) {
            byte[] data = cursor.getBlob(0);
            if (data != null) {
              if (Log.DEBUG) Log.v("openContactPhotoInputStream(): contact photo found");
              return new ByteArrayInputStream(data);
            }
          }
        } finally {
          cursor.close();
        }
      }

      if (Log.DEBUG) Log.v("openContactPhotoInputStream(): looking in Contacts.Photos");
      /*
       * Contacts.Photos.CONTENT_URI is "content://contacts/photos"
       * Uri will end up being "content://contacts/photos/#contactId"
       */
      photoUri = Uri.withAppendedPath(Contacts.Photos.CONTENT_URI, id);

      cursor = cr.query(photoUri, new String[] {Photos.DATA}, null, null, null);

      if (cursor != null) {
        try {
          if (cursor.moveToFirst()) {
            byte[] data = cursor.getBlob(0);
            if (data != null) {
              if (Log.DEBUG) Log.v("openContactPhotoInputStream(): contact photo found");
              return new ByteArrayInputStream(data);
            }
          }
        } finally {
          cursor.close();
        }
      }

      return null;
    }

    /*
     * User is using Eclair or beyond, use reflection to pull the photo
     */
    if (!preparedEclairSDK) {
      prepareEclairSDK();
    }

    if (preparedEclairSDK) {
      try {
        return (InputStream) contactsOpenPhotoStreamMethod.invoke(
            contactsClass, cr,
            Uri.withAppendedPath(getContentUri(), id));
      } catch (IllegalArgumentException e) {
        Log.e("Unable to fetch contact photo using Anroid 2.0+ SDK: " + e.toString());
      } catch (IllegalAccessException e) {
        Log.e("Unable to fetch contact photo using Anroid 2.0+ SDK: " + e.toString());
      } catch (InvocationTargetException e) {
        Log.e("Unable to fetch contact photo using Anroid 2.0+ SDK: " + e.toString());
      }
    }

    return null;
  }

  /*
   * Prepare Eclair (and beyond) SDK call using reflection
   */
  private static void prepareEclairSDK() {
    try {
      contactsClass = Class.forName("android.provider.ContactsContract$Contacts");
      contactsOpenPhotoStreamMethod = contactsClass.getMethod("openContactPhotoInputStream",
          new Class[] { ContentResolver.class, Uri.class });
      preparedEclairSDK = true;
    } catch (ClassNotFoundException e) {
      Log.e("Unable to prepare Anroid 2.0+ SDK functions: " + e.toString());
    } catch (SecurityException e) {
      Log.e("Unable to prepare Anroid 2.0+ SDK functions: " + e.toString());
    } catch (NoSuchMethodException e) {
      Log.e("Unable to prepare Anroid 2.0+ SDK functions: " + e.toString());
    }
  }
}