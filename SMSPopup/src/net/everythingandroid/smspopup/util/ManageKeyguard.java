package net.everythingandroid.smspopup.util;

import net.everythingandroid.smspopup.BuildConfig;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.KeyguardManager.OnKeyguardExitResult;
import android.content.Context;

public class ManageKeyguard {
    private static KeyguardManager myKM = null;
    private static KeyguardLock myKL = null; // TODO: change to FLAG_DISMISS_KEYGUARD/FLAG_SHOW_WHEN_LOCKED

    public static synchronized void initialize(Context context) {
        if (myKM == null) {
            myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        }
    }

    public static synchronized void disableKeyguard(Context context) {
        initialize(context);

        if (myKM.inKeyguardRestrictedInputMode()) {
            myKL = myKM.newKeyguardLock(Log.LOGTAG);
            myKL.disableKeyguard();
            if (BuildConfig.DEBUG)
                Log.v("--Keyguard disabled");
        } else {
            myKL = null;
        }
    }

    public static synchronized boolean inKeyguardRestrictedInputMode() {
        if (myKM != null) {
            if (BuildConfig.DEBUG)
                Log.v("--inKeyguardRestrictedInputMode = " + myKM.inKeyguardRestrictedInputMode());
            return myKM.inKeyguardRestrictedInputMode();
        }
        return false;
    }

    public static synchronized void reenableKeyguard() {
        if (myKM != null) {
            if (myKL != null) {
                myKL.reenableKeyguard();
                myKL = null;
                if (BuildConfig.DEBUG)
                    Log.v("--Keyguard reenabled");
            }
        }
    }

    public static synchronized void exitKeyguardSecurely(final LaunchOnKeyguardExit callback) {
        if (inKeyguardRestrictedInputMode()) {
            if (BuildConfig.DEBUG)
                Log.v("--Trying to exit keyguard securely");
            myKM.exitKeyguardSecurely(new OnKeyguardExitResult() {
                @Override
                public void onKeyguardExitResult(boolean success) {
                    reenableKeyguard();
                    if (success) {
                        if (BuildConfig.DEBUG)
                            Log.v("--Keyguard exited securely");
                        callback.LaunchOnKeyguardExitSuccess();
                    } else {
                        if (BuildConfig.DEBUG)
                            Log.v("--Keyguard exit failed");
                    }
                }
            });
        } else {
            callback.LaunchOnKeyguardExitSuccess();
        }
    }

    public interface LaunchOnKeyguardExit {
        public void LaunchOnKeyguardExitSuccess();
    }
}
