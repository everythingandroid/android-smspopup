package net.everythingandroid.smspopup.controls;

import java.util.ArrayList;

import net.everythingandroid.smspopup.Log;
import net.everythingandroid.smspopup.SmsMmsMessage;
import uk.co.jasonfry.android.tools.ui.SwipeView;
import android.content.Context;
import android.util.AttributeSet;

public class SmsPopupSwipeView extends SwipeView {

  private Context context;
  private ArrayList<SmsMmsMessage> messages;
  private MessageCountChanged messageCountChanged;
  private static boolean lockMode;

  public static final int PRIVACY_MODE_OFF = 0;
  public static final int PRIVACY_MODE_HIDE_MESSAGE = 1;
  public static final int PRIVACY_MODE_HIDE_ALL = 2;

  private static int privacyMode = PRIVACY_MODE_OFF;

  float oldTouchValue;

  public SmsPopupSwipeView(Context context) {
    super(context);
    init(context);
  }

  public SmsPopupSwipeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  private void init(Context c) {
    context = c;
    messages = new ArrayList<SmsMmsMessage>(5);
    setOnPageChangedListener(new OnPageChangedListener() {
      @Override
      public void onPageChanged(int oldPage, int newPage) {
        UpdateMessageCount();
      }
    });
  }

  /**
   * Add a message and its view to the end of the list of messages
   *
   * @param newMessage
   */
  public void addMessage(SmsMmsMessage newMessage) {
    messages.add(newMessage);
    addView(new SmsPopupView(context, newMessage));
    UpdateMessageCount();
  }

  public void addMessages(ArrayList<SmsMmsMessage> newMessages) {
    if (newMessages != null) {
      for (int i=0; i<newMessages.size(); i++) {
        addView(new SmsPopupView(context, newMessages.get(i)));
      }
      messages.addAll(newMessages);
      UpdateMessageCount();
    }
  }

  /**
   * Remove the message and its view and the location numMessage
   *
   * @param numMessage
   * @return true if there were no more messages to remove, false otherwise
   */
  public boolean removeMessage(int numMessage) {
    final int totalMessages = getPageCount();
    final int currentMessage = getCurrentPage();

    if (numMessage < totalMessages && numMessage >= 0 && totalMessages > 1) {
      if (currentMessage == numMessage) {
        // If removing last page, go to previous
        if (currentMessage == (totalMessages-1)) {
          showPrevious();
        } else {
          //showNext();
        }        
      }

      // Remove the view
      removeView(numMessage);

      // Remove message from arraylist
      messages.remove(numMessage);

      // Run any other updates (as set by interface)
      UpdateMessageCount();

      // If more messages, return false
      if (totalMessages > 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Remove the currently active message, if there is only one message left then it will not
   * be removed
   *
   * @return true if there were no more messages to remove, false otherwise
   */
  public boolean removeActiveMessage() {
    return removeMessage(getCurrentPage());
  }
  
  /**
   * Removes a view from the child layout
   * @param index view to remove
   */
  public void removeView(final int index) {
    getChildContainer().removeViewAt(index);
  }

  /**
   * Return the currently active message
   * @return
   */
  public SmsMmsMessage getActiveMessage() {
    return messages.get(getCurrentPage());
  }

  public void setOnMessageCountChanged(MessageCountChanged m) {
    messageCountChanged = m;
  }

  public static interface MessageCountChanged {
    abstract void onChange(int current, int total);
  }

  private void UpdateMessageCount() {
    if (messageCountChanged != null) {
      messageCountChanged.onChange(getCurrentPage(), getPageCount());
    }
  }

  public void showNext() {
    if (Log.DEBUG) Log.v("showNext() - " + getCurrentPage() + ", " + getActiveMessage().getContactName());
    smoothScrollToPage(getCurrentPage()+1);
  }

  public void showPrevious() {
    if (Log.DEBUG) Log.v("showPrevious() - " + getCurrentPage() + ", " + getActiveMessage().getContactName());
    smoothScrollToPage(getCurrentPage()-1);
  }

  public void refreshPrivacy() {
    for (int i=0; i<getChildCount(); i++) {
      ((SmsPopupView) getChildContainer().getChildAt(i)).refreshPrivacy(privacyMode);
    }
  }

  // Set privacy using mode
  public static void setPrivacy(int mode) {
    privacyMode = mode;
  }

  // Set privacy from preference boolean values
  public static void setPrivacy(boolean privacyMode, boolean privacySender) {
    setPrivacy(SmsPopupView.PRIVACY_MODE_OFF);
    if (privacyMode) {
      if (privacySender) {
        setPrivacy(SmsPopupView.PRIVACY_MODE_HIDE_ALL);
      } else {
        setPrivacy(SmsPopupView.PRIVACY_MODE_HIDE_MESSAGE);
      }
    }
  }

  public static void setLockMode(boolean mode) {
    lockMode = mode;
  }
  
  public static boolean getLockMode(boolean mode) {
    return lockMode;
  }    
}