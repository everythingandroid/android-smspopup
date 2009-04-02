package net.everythingandroid.smspopup;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.preference.PreferenceManager;
import android.telephony.gsm.SmsMessage;

public class SMSReceiverService extends Service {
	private static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	private static final String ACTION_MMS_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED";
	private static final String MMS_DATA_TYPE = "application/vnd.wap.mms-message";
   //private static final String ACTION_MESSAGE_SENT = "com.android.mms.transaction.MESSAGE_SENT";

	/* 
	 * This is the number of retries and pause between retries that we will keep
	 * checking the system message database for the latest incoming message
	 */
	private static final int MESSAGE_RETRY = 5;
	private static final int MESSAGE_RETRY_PAUSE = 500;

	private Context context;
   private ServiceHandler mServiceHandler;
	private Looper mServiceLooper;
//	private int mResultCode;
	
	private static final Object mStartingServiceSync = new Object();
	private static PowerManager.WakeLock mStartingService;
	
	@Override
	public void onCreate() {
		Log.v("SMSReceiverService: onCreate()");
		HandlerThread thread = new HandlerThread(Log.LOGTAG, Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		context = getApplicationContext();
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);	
	}
	
   @Override
	public void onStart(Intent intent, int startId) {
   	Log.v("SMSReceiverService: onStart()");
		
   	//mResultCode = intent.getIntExtra("result", 0);
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		msg.obj = intent;
		mServiceHandler.sendMessage(msg);
	}

	@Override
	public void onDestroy() {
		Log.v("SMSReceiverService: onDestroy()");
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
			Log.v("SMSReceiverService: handleMessage()");
			
			int serviceId = msg.arg1;
			Intent intent = (Intent) msg.obj;
			String action = intent.getAction();
			String dataType = intent.getType();

			if (ACTION_SMS_RECEIVED.equals(action)) {
				handleSmsReceived(intent);
			} else if (ACTION_MMS_RECEIVED.equals(action) && MMS_DATA_TYPE.equals(dataType)) {
				handleMmsReceived(intent);
//			} else if (ACTION_MESSAGE_SENT.equals(action)) {
//				handleMessageSent(intent);
			}

			// NOTE: We MUST not call stopSelf() directly, since we need to
			// make sure the wake lock acquired by AlertReceiver is released.
			finishStartingService(SMSReceiverService.this, serviceId);
		}
	}

	private void handleSmsReceived(Intent intent) {
		Log.v("SMSReceiver: Intercept SMS");
		int count = 0;
				
		Bundle bundle = intent.getExtras();
		if (bundle != null) {

			SharedPreferences myPrefs = 
				PreferenceManager.getDefaultSharedPreferences(context);
			boolean onlyShowOnKeyguard = myPrefs.getBoolean(
					context.getString(R.string.pref_onlyShowOnKeyguard_key),
					Boolean.valueOf(
							context.getString(R.string.pref_onlyShowOnKeyguard_default)));

			SmsMessage[] messages = SMSPopupUtils.getMessagesFromIntent(intent);
			if (messages != null) {
				SmsMessage sms = messages[0];

				// Make sure SMS is not Class 0 or a replacement SMS
				if (sms.getMessageClass() != SmsMessage.MessageClass.CLASS_0 && !sms.isReplace()) {
					
					String body = "";
					if (messages.length == 1) {
						body = messages[0].getDisplayMessageBody();
					} else {
						StringBuilder bodyText = new StringBuilder();
						for (int i = 0; i < messages.length; i++) {
							bodyText.append(messages[i].getMessageBody());
						}
						body = bodyText.toString();
					}

					// Switched to getDisplayOriginatingAddress()
					//String address = messages[0].getOriginatingAddress();
					String address = messages[0].getDisplayOriginatingAddress();
					
					// Changed to use system time, same as the system app (see Mms.git)
					// The only issue with this is we can no longer do a direct match
					// with the database to find this message based on timestamp as there 
					// will be a slight difference in the times.

					long timestamp_provider = messages[0].getTimestampMillis();
					long timestamp = System.currentTimeMillis();

					//String message = body.toString();

//					SmsMmsMessage smsMessageFromIntent = new SmsMmsMessage(context, 
//							address, message, timestamp, SmsMmsMessage.MESSAGE_TYPE_SMS);
					
					SmsMmsMessage smsMessage = null;
					boolean equalToIntent = false;
					
					//while (smsMessage == null && count < SMS_RETRY && !equalToIntent) {
					while (count < MESSAGE_RETRY && !equalToIntent) {
						count++;
						smsMessage = SMSPopupUtils.getSmsDetails(context);
						if (smsMessage != null) {
							// The local database matches the data received from the intent
//							if (smsMessageFromIntent.equals(smsMessage)) {
							if (smsMessage.equals(address, timestamp, timestamp_provider, body)
									|| count == MESSAGE_RETRY) {
								equalToIntent = true;
								
								Log.v("SMS in DB matches Intent");
								ManageKeyguard.initialize(context);

								if (ManageKeyguard.inKeyguardRestrictedInputMode() || !onlyShowOnKeyguard) {
									Log.v("^^^^^^In keyguard or pref set to always show - showing popup activity");
									Intent popup = smsMessage.getPopupIntent();						
									ManageWakeLock.acquirePartial(context);
									context.startActivity(popup);
								} else {
									Log.v("^^^^^^Not in keyguard, only using notification");
									ManageNotification.show(context, smsMessage);
									ReminderReceiver.scheduleReminder(context, smsMessage);
								}								
							}
						}
						if (!equalToIntent) {		
							Log.v("SMS not found, sleeping (count is " + count + ")");
							try {
								Thread.sleep(MESSAGE_RETRY_PAUSE);
							} catch (InterruptedException e) {
								//e.printStackTrace();
							}
						}
					}
					
					// After while loop
					if (!equalToIntent) {
						// Aw oh, we tried hard, but couldn't find an exact match for the 
						// incoming message in the system database.
						// TODO: not sure what we should do here, play the notification anyway?
					}
				}
			}
		}
   }

	private void handleMmsReceived(Intent intent) {
		Log.v("MMS received!");
		SmsMmsMessage mmsMessage = null;
		int count = 0;
		
		// Ok this is super hacky, but fixes the case where this code
		// runs before the system MMS transaction service (that stores
		// the MMS details in the database).  This should really be
		// a content listener that waits for a while then gives up...
		while (mmsMessage == null && count < MESSAGE_RETRY) {
			mmsMessage = SMSPopupUtils.getMmsDetails(context);
			if (mmsMessage != null) {
				Log.v("MMS found in content provider");
				SharedPreferences myPrefs =
					PreferenceManager.getDefaultSharedPreferences(context);
				boolean onlyShowOnKeyguard = myPrefs.getBoolean(
						context.getString(R.string.pref_onlyShowOnKeyguard_key),
						Boolean.valueOf(
								context.getString(R.string.pref_onlyShowOnKeyguard_default)));
	
				if (ManageKeyguard.inKeyguardRestrictedInputMode() || !onlyShowOnKeyguard) {
					Log.v("^^^^^^In keyguard or pref set to always show - showing popup activity");
					Intent popup = mmsMessage.getPopupIntent();
					ManageWakeLock.acquirePartial(context);
					context.startActivity(popup);
				} else {
					Log.v("^^^^^^Not in keyguard, only using notification");
					ManageNotification.show(context, mmsMessage);
					ReminderReceiver.scheduleReminder(context, mmsMessage);
				}
			} else {				
				Log.v("MMS not found, sleeping (count is " + count + ")");
				count++;
				try {
					Thread.sleep(MESSAGE_RETRY_PAUSE);
				} catch (InterruptedException e) {
					//e.printStackTrace();
				}
			}
		}
	}

//	Unfortunately the system Mms app does not broadcast messages being sent to
// all receivers.
//	private void handleMessageSent(Intent intent) {
//		if (mResultCode != Activity.RESULT_OK && mResultCode != SmsManager.RESULT_ERROR_RADIO_OFF) {
//		}
//	}

   /**
	 * Start the service to process the current event notifications, acquiring
	 * the wake lock before returning to ensure that the service will run.
	 */
	public static void beginStartingService(Context context, Intent intent) {
		synchronized (mStartingServiceSync) {
			Log.v("SMSReceiverService: beginStartingService()");
			if (mStartingService == null) {
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				mStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				      Log.LOGTAG);
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
			Log.v("SMSReceiverService: finishStartingService()");
			if (mStartingService != null) {
				if (service.stopSelfResult(startId)) {
					mStartingService.release();
				}
			}
		}
	}
}