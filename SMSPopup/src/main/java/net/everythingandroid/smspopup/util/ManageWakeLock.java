package net.everythingandroid.smspopup.util;

import net.everythingandroid.smspopup.BuildConfig;
import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.receiver.ClearAllReceiver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;

public class ManageWakeLock {
    private static volatile PowerManager.WakeLock mWakeLock = null;
    private static volatile  PowerManager.WakeLock mPartialWakeLock = null;
    private static final boolean PREFS_SCREENON_DEFAULT = true;
    private static final boolean PREFS_DIMSCREEN_DEFAULT = false;
    private static final String PREFS_TIMEOUT_DEFAULT = "30";

    public static synchronized void acquireFull(Context mContext) {
        if (mWakeLock != null) {
            if (BuildConfig.DEBUG)
                Log.v("**Wakelock already held");
            return;
        }

        PowerManager mPm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        int flags;

        // Check dim screen preference
        if (mPrefs.getBoolean(
                mContext.getString(R.string.pref_dimscreen_key), PREFS_DIMSCREEN_DEFAULT)) {
            flags = PowerManager.SCREEN_DIM_WAKE_LOCK;
        } else {
            flags = PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
        }

        // Check if screen should turn on, if so, set flags and unlock keyguard
        if (mPrefs.getBoolean(mContext.getString(R.string.pref_screen_on_key),
                PREFS_SCREENON_DEFAULT)) {
            flags |= PowerManager.ACQUIRE_CAUSES_WAKEUP;
            ManageKeyguard.disableKeyguard(mContext);
        }

        mWakeLock = mPm.newWakeLock(flags, Log.LOGTAG + ".full");
        mWakeLock.setReferenceCounted(false);
        mWakeLock.acquire();
        if (BuildConfig.DEBUG)
            Log.v("**Wakelock acquired");

        // Fetch wakelock/screen timeout from preferences
        int timeout = Integer.valueOf(mPrefs.getString(
                mContext.getString(R.string.pref_timeout_key), PREFS_TIMEOUT_DEFAULT));

        // Set a receiver to remove all locks in "timeout" seconds
        ClearAllReceiver.setCancel(mContext, timeout);
    }

    public static synchronized void acquirePartial(Context mContext) {
        // Check if partial lock already exists
        if (mPartialWakeLock != null)
            return;

        PowerManager mPm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        mPartialWakeLock =
                mPm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Log.LOGTAG + ".partial");
        if (BuildConfig.DEBUG)
            Log.v("**Wakelock (partial) acquired");
        mPartialWakeLock.setReferenceCounted(false);

        // Set timeout of 5mins in case for some reason the wakelock is not released by the
        // activity. Note that this only works because setReferenceCounted(false), otherwise a
        // system bug causes a crash (http://code.google.com/p/android/issues/detail?id=14184).
        mPartialWakeLock.acquire(300000);
    }

    public static synchronized void releaseFull() {
        if (mWakeLock != null) {
            if (BuildConfig.DEBUG)
                Log.v("**Wakelock released");
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    public static synchronized void releasePartial() {
        if (mPartialWakeLock != null && mPartialWakeLock.isHeld()) {
            if (BuildConfig.DEBUG)
                Log.v("**Wakelock (partial) released");
            mPartialWakeLock.release();
            mPartialWakeLock = null;
        }
    }

    public static synchronized void releaseAll() {
        releaseFull();
        releasePartial();
    }

}