package net.everythingandroid.smspopup;

public class Log {
  public final static String LOGTAG = "SMSPopup";

  private static final boolean DEBUG = true;

  // private static final boolean DEBUG = false;
  // static final boolean LOGV = DEBUG ? Config.LOGD : Config.LOGV;

  public static void v(String msg) {
    if (DEBUG) {
      // if (LOGV) {
      android.util.Log.v(LOGTAG, msg);
    }
  }

  public static void e(String msg) {
    android.util.Log.e(LOGTAG, msg);
  }
}
