package net.everythingandroid.smspopup;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;

public class SMSPopupUtilsService extends Service {
	public static final String ACTION_MARK_THREAD_READ =
			"net.everythingandroid.smspopup.ACTION_MARK_THREAD_READ";
	public static final String ACTION_DELETE_MESSAGE =
			"net.everythingandroid.smspopup.ACTION_DELETE_MESSAGE";	
	public static final String ACTION_OTHER = "net.everythingandroid.smspopup.ACTION_OTHER";

	private Context context;
   private ServiceHandler mServiceHandler;
	private Looper mServiceLooper;
	
	static final Object mStartingServiceSync = new Object();
	static PowerManager.WakeLock mStartingService;
	
	@Override
	public void onCreate() {
		HandlerThread thread = new HandlerThread(Log.LOGTAG, Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		context = getApplicationContext();
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);	
	}
	
   @Override
	public void onStart(Intent intent, int startId) {
		//mResultCode = intent.getIntExtra("result", 0);
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		msg.obj = intent;
		mServiceHandler.sendMessage(msg);
	}

	@Override
	public void onDestroy() {
		mServiceLooper.quit();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
   private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			Log.v("SMSPopupUtilsService: handleMessage()");
			int serviceId = msg.arg1;
			Intent intent = (Intent) msg.obj;

			String action = intent.getAction();
			
			if (ACTION_MARK_THREAD_READ.equals(action)) {
				Log.v("SMSPopupUtilsService: marking thread read");
				SmsMmsMessage message = new SmsMmsMessage(context, intent.getExtras());
				message.setThreadRead();
			} else if (ACTION_DELETE_MESSAGE.equals(action)) {
				Log.v("SMSPopupUtilsService: deleting message");
				SmsMmsMessage message = new SmsMmsMessage(context, intent.getExtras());
				message.delete();
			} else if (ACTION_OTHER.equals(action)) {
				
			}
			
			// NOTE: We MUST not call stopSelf() directly, since we need to
			// make sure the wake lock acquired by AlertReceiver is released.
			finishStartingService(SMSPopupUtilsService.this, serviceId);
		}
	}

   /**
	 * Start the service to process the current event notifications, acquiring
	 * the wake lock before returning to ensure that the service will run.
	 */
	public static void beginStartingService(Context context, Intent intent) {
		synchronized (mStartingServiceSync) {
			Log.v("SMSPopupUtilsService: beginStartingService()");
			if (mStartingService == null) {
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				mStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				      "StartingPopupUtilsService");
				mStartingService.setReferenceCounted(false);
			}
			mStartingService.acquire();
			context.startService(intent);
		}
	}

	/**
	 * Called back by the service when it has finished processing notifications,
	 * releasing the wake lock if the service is now stopping.
	 */
	public static void finishStartingService(Service service, int startId) {
		synchronized (mStartingServiceSync) {
			Log.v("SMSPopupUtilsService: finishStartingService()");
			if (mStartingService != null) {
				if (service.stopSelfResult(startId)) {
					mStartingService.release();
				}
			}
		}
	}
}