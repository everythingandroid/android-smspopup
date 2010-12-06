package net.everythingandroid.smspopup.controls;

import java.util.ArrayList;

import net.everythingandroid.smspopup.Log;
import net.everythingandroid.smspopup.SmsMmsMessage;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ViewFlipper;

public class SmsPopupViewFlipper extends ViewFlipper {

  private Context context;

  private ArrayList<SmsMmsMessage> messages;
  private int currentMessage;
  private int totalMessages;
  private MessageCountChanged messageCountChanged;

  public static final int PRIVACY_MODE_OFF = 0;
  public static final int PRIVACY_MODE_HIDE_MESSAGE = 1;
  public static final int PRIVACY_MODE_HIDE_ALL = 2;

  private static int privacyMode = PRIVACY_MODE_OFF;
  private static boolean lockMode;

  float oldTouchValue;

  public SmsPopupViewFlipper(Context context) {
    super(context);
    init(context);
  }

  public SmsPopupViewFlipper(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  private void init(Context c) {
    context = c;
    messages = new ArrayList<SmsMmsMessage>(5);
    totalMessages = 0;
    currentMessage = 0;
  }

  /**
   * Add a message and its view to the end of the list of messages
   *
   * @param newMessage
   */
  public void addMessage(SmsMmsMessage newMessage) {
    messages.add(newMessage);
    totalMessages = messages.size();
    addView(new SmsPopupView(context, newMessage));
    UpdateMessageCount();
  }

  public void addMessages(ArrayList<SmsMmsMessage> newMessages) {
    if (newMessages != null) {
      for (int i=0; i<newMessages.size(); i++) {
        addView(new SmsPopupView(context, newMessages.get(i)));
      }
      messages.addAll(newMessages);
      totalMessages = messages.size();
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

    if (numMessage < totalMessages && numMessage >= 0 && totalMessages > 1) {

      // Fadeout current message
      setOutAnimation(context, android.R.anim.fade_out);

      // If last message, slide in from left
      if (numMessage == (totalMessages-1)) {
        setInAnimation(AnimationHelper.inFromLeftAnimation());
      } else{ // Else slide in from right
        setInAnimation(AnimationHelper.inFromRightAnimation());
      }

      // Remove the view
      removeViewAt(numMessage);

      // Remove message from arraylist
      messages.remove(numMessage);

      // Update total messages count
      totalMessages = messages.size();

      // If we removed the last message then set current message to last
      if (currentMessage >= totalMessages) {
        currentMessage = totalMessages-1;
      }

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
    return removeMessage(currentMessage);
  }

  /**
   * Return the currently active message
   * @return
   */
  public SmsMmsMessage getActiveMessage() {
    return messages.get(currentMessage);
  }

  public void setOnMessageCountChanged(MessageCountChanged m) {
    messageCountChanged = m;
  }

  public static interface MessageCountChanged {
    abstract void onChange(int current, int total);
  }

  private void UpdateMessageCount() {
    if (messageCountChanged != null) {
      messageCountChanged.onChange(currentMessage, totalMessages);
    }
  }

  @Override
  public void showNext() {
    if (currentMessage < totalMessages-1) {
      currentMessage += 1;
      setInAnimation(AnimationHelper.inFromRightAnimation());
      setOutAnimation(AnimationHelper.outToLeftAnimation());
      super.showNext();
      UpdateMessageCount();
      if (Log.DEBUG) Log.v("showNext() - " + currentMessage + ", " + getActiveMessage().getContactName());
    }
  }

  @Override
  public void showPrevious() {
    if (currentMessage > 0) {
      currentMessage -= 1;
      setInAnimation(AnimationHelper.inFromLeftAnimation());
      setOutAnimation(AnimationHelper.outToRightAnimation());
      super.showPrevious();
      UpdateMessageCount();
      if (Log.DEBUG) Log.v("showPrevious() - " + currentMessage + ", " + getActiveMessage().getContactName());
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (lockMode) return true;

    switch (event.getAction()) {
      case MotionEvent.ACTION_MOVE:
        Log.v("ACTION_MOVE");
        final View currentView = getCurrentView();
        currentView.layout((int) (event.getX() - oldTouchValue), currentView.getTop(),
            currentView.getRight(), currentView.getBottom());
        oldTouchValue = event.getX();
        break;
    }

    return super.onTouchEvent(event);
  }

  public void refreshPrivacy() {
    for (int i=0; i<getChildCount(); i++) {
      ((SmsPopupView) getChildAt(i)).refreshPrivacy(privacyMode);
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

  private static class AnimationHelper {
    public static Animation inFromRightAnimation() {
      Animation inFromRight = new TranslateAnimation(
      Animation.RELATIVE_TO_PARENT, +1.0f,
      Animation.RELATIVE_TO_PARENT, 0.0f,
      Animation.RELATIVE_TO_PARENT, 0.0f,
      Animation.RELATIVE_TO_PARENT, 0.0f);
      inFromRight.setDuration(350);
      inFromRight.setInterpolator(new AccelerateInterpolator());
      return inFromRight;
    }

    public static Animation outToLeftAnimation() {
      Animation outtoLeft = new TranslateAnimation(
      Animation.RELATIVE_TO_PARENT, 0.0f,
      Animation.RELATIVE_TO_PARENT, -1.0f,
      Animation.RELATIVE_TO_PARENT, 0.0f,
      Animation.RELATIVE_TO_PARENT, 0.0f);
      outtoLeft.setDuration(350);
      outtoLeft.setInterpolator(new AccelerateInterpolator());
      return outtoLeft;
    }

    public static Animation inFromLeftAnimation() {
      Animation inFromLeft = new TranslateAnimation(
      Animation.RELATIVE_TO_PARENT, -1.0f,
      Animation.RELATIVE_TO_PARENT, 0.0f,
      Animation.RELATIVE_TO_PARENT, 0.0f,
      Animation.RELATIVE_TO_PARENT, 0.0f);
      inFromLeft.setDuration(350);
      inFromLeft.setInterpolator(new AccelerateInterpolator());
      return inFromLeft;
    }

    public static Animation outToRightAnimation() {
      Animation outtoRight = new TranslateAnimation(
      Animation.RELATIVE_TO_PARENT, 0.0f,
      Animation.RELATIVE_TO_PARENT, +1.0f,
      Animation.RELATIVE_TO_PARENT, 0.0f,
      Animation.RELATIVE_TO_PARENT, 0.0f);
      outtoRight.setDuration(350);
      outtoRight.setInterpolator(new AccelerateInterpolator());
      return outtoRight;
    }
  }

}
