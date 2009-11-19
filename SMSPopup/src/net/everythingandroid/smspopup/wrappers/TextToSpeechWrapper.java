package net.everythingandroid.smspopup.wrappers;

import java.util.HashMap;
import java.util.Locale;

import android.content.Context;
import android.speech.tts.TextToSpeech;

/*
 * A wrapper class for the newer Android text-to-speech library that is only found in
 * Android OS 1.6 and above (Donut and above).  This is useful so that the app can
 * be loaded on pre-Donut devices without breaking the app.
 */
public class TextToSpeechWrapper {

  private TextToSpeech mTextToSpeech;

  // class initialization fails when this throws an exception
  static {
    try {
      Class.forName("android.speech.tts.TextToSpeech");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // Some static vars from the text-to-speech class
  public static int SUCCESS = TextToSpeech.SUCCESS;
  public static int QUEUE_FLUSH = TextToSpeech.QUEUE_FLUSH;

  // calling here forces class initialization
  public static void checkAvailable() {}

  private OnInitListener onInitListener = null;

  public interface OnInitListener {
    public abstract void onInit(int status);
  }

  /**
   * Constructor just takes a context and the OnInitListener
   */
  public TextToSpeechWrapper(Context context, TextToSpeechWrapper.OnInitListener listener) {
    onInitListener = listener;
    mTextToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
      public void onInit(int status) {
        onInitListener.onInit(status);
      }
    });
  }

  public int setSpeechRate(float speechRate) {
    return mTextToSpeech.setSpeechRate(speechRate);
  }

  public void shutdown() {
    mTextToSpeech.shutdown();
  }

  public int speak(String text, int queueMode, HashMap<String, String> params) {
    return mTextToSpeech.speak(text, queueMode, params);
  }

  public int setLanguage(Locale loc) {
    return mTextToSpeech.setLanguage(loc);
  }

  public int isLanguageAvailable(Locale loc) {
    return mTextToSpeech.isLanguageAvailable(loc);
  }
}

