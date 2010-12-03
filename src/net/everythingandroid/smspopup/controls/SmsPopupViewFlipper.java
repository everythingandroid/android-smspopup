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

  public void addMessage(SmsMmsMessage newMessage) {
    messages.add(newMessage);
    totalMessages = messages.size();
    addView(new SmsPopupView(context, newMessage));
  }

  public void addMessages(ArrayList<SmsMmsMessage> newMessages) {
    if (newMessages != null) {
      for (int i=0; i<newMessages.size(); i++) {
        addView(new SmsPopupView(context, newMessages.get(i)));
      }
      messages.addAll(newMessages);
      totalMessages = messages.size();
    }
  }

  public boolean removeMessage() {
      return removeMessage(0);
  }

  public boolean removeMessage(int numMessage) {
    if (numMessage < totalMessages && numMessage >= 0) {
      removeViewAt(numMessage);
      messages.remove(numMessage);
      totalMessages = messages.size();
      if (totalMessages > 0) {
        return false;
      }
    }
    return true;
  }

  public boolean removeActiveMessage() {
    return removeMessage(currentMessage);
  }

  public SmsMmsMessage getActiveMessage() {
    return messages.get(currentMessage);
  }


  @Override
  public void showNext() {

    if (currentMessage < totalMessages-1) {
      currentMessage += 1;
      setInAnimation(AnimationHelper.inFromRightAnimation());
      setOutAnimation(AnimationHelper.outToLeftAnimation());
      super.showNext();

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

      if (Log.DEBUG) Log.v("showPrevious() - " + currentMessage + ", " + getActiveMessage().getContactName());
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {

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
