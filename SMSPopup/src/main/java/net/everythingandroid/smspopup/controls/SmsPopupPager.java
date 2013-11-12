package net.everythingandroid.smspopup.controls;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;

import com.viewpagerindicator.CirclePageIndicator;

import net.everythingandroid.smspopup.BuildConfig;
import net.everythingandroid.smspopup.R;
import net.everythingandroid.smspopup.provider.SmsMmsMessage;
import net.everythingandroid.smspopup.util.Log;

import java.util.ArrayList;

public class SmsPopupPager extends ViewPager implements OnPageChangeListener {

    private ArrayList<SmsMmsMessage> messages;
    private int currentPage;
    private MessageCountChanged messageCountChanged;
    private Context mContext;
    private CirclePageIndicator mPagerIndicator;
    private volatile boolean removingMessage = false;
    private GestureDetector mGestureDetector;

    public static int STATUS_MESSAGES_REMAINING = 0;
    public static int STATUS_NO_MESSAGES_REMAINING = 1;
    public static int STATUS_REMOVING_MESSAGE = 2;

    public SmsPopupPager(Context context) {
        super(context);
        init(context);
    }

    public SmsPopupPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        messages = new ArrayList<SmsMmsMessage>(5);
        currentPage = 0;
        setOffscreenPageLimit(1);
        setPageMargin((int) context.getResources().getDimension(R.dimen.smspopup_pager_margin));
        setLongClickable(true);
    }

    public int getPageCount() {
        return messages.size();
    }

    public void setGestureListener(SimpleOnGestureListener listener) {
        mGestureDetector = new GestureDetector(mContext, listener);
    }

    /**
     * Add a message and its view to the end of the list of messages.
     *
     * @param newMessage
     *            The message to add.
     */
    public synchronized void addMessage(SmsMmsMessage newMessage) {
        messages.add(newMessage);
        UpdateMessageCount();
    }

    /**
     * Add a list of new messages to the end of the current message list.
     *
     * @param newMessages
     *            The list of new messages to add.
     */
    public synchronized void addMessages(ArrayList<SmsMmsMessage> newMessages) {
        if (newMessages != null) {
            messages.addAll(0, newMessages);
            UpdateMessageCount();
        }
    }

    /**
     * Remove a specific message from the list, if there is only one message left then it will not
     * be removed.
     *
     * @param numMessage
     * @return One of STATUS_MESSAGES_REMAINING, STATUS_NO_MESSAGES_REMAINING or
     * STATUS_REMOVING_MESSAGE
     */
    public synchronized int removeMessage(final int numMessage) {
        if (removingMessage) {
            return STATUS_REMOVING_MESSAGE;
        }

        final int totalMessages = getPageCount();

        if (totalMessages <= 1) {
            return STATUS_NO_MESSAGES_REMAINING;
        }

        if (numMessage >= totalMessages || numMessage < 0) {
            return STATUS_NO_MESSAGES_REMAINING;
        }

        Animation mAnimation = AnimationUtils.loadAnimation(mContext, R.anim.shrink_fade_out_center);
        mAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                removingMessage = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (numMessage < currentPage && currentPage != (totalMessages - 1)) {
                    currentPage--;
                }

                messages.remove(numMessage);
                getAdapter().notifyDataSetChanged();
                UpdateMessageCount();
                removingMessage = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}

        });
        startAnimation(mAnimation);

        return STATUS_MESSAGES_REMAINING;
    }

    /**
     * Remove the currently active message, if there is only one message left then it will not be
     * removed.
     *
     * @return One of STATUS_MESSAGES_REMAINING, STATUS_NO_MESSAGES_REMAINING or
     * STATUS_REMOVING_MESSAGE
     */
    public int removeActiveMessage() {
        return removeMessage(currentPage);
    }

    /**
     * Return the currently active message.
     *
     * @return The currently visible message.
     */
    public synchronized SmsMmsMessage getActiveMessage() {
        return messages.get(currentPage);
    }

    public synchronized int getActiveMessageNum() {
        return currentPage;
    }

    public void setOnMessageCountChanged(MessageCountChanged m) {
        messageCountChanged = m;
    }

    public static interface MessageCountChanged {
        abstract void onChange(int current, int total);
    }

    private void UpdateMessageCount() {
        if (mPagerIndicator != null) {
            mPagerIndicator.invalidate();
        }
        if (messageCountChanged != null) {
            messageCountChanged.onChange(currentPage, getPageCount());
        }
    }

    public void showNext() {
        if (currentPage < (getPageCount() - 1)) {
            setCurrentItem(currentPage + 1);
        }
        if (BuildConfig.DEBUG)
            Log.v("showNext() - " + currentPage + ", " + getActiveMessage().getContactName());
    }

    public void showPrevious() {
        if (currentPage > 0) {
            setCurrentItem(currentPage - 1);
        }
        if (BuildConfig.DEBUG)
            Log.v("showPrevious() - " + currentPage + ", " + getActiveMessage().getContactName());
    }

    @Override
    public void setCurrentItem(int num) {
        super.setCurrentItem(num);
        currentPage = num;
    }

    public void showLast() {
        setCurrentItem(getPageCount() - 1);
    }

    @Override
    public void onPageScrollStateChanged(int state) {}

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        super.onPageScrolled(position, positionOffset, positionOffsetPixels);
    }

    @Override
    public void onPageSelected(int position) {
        currentPage = position;
    }

    public void setIndicator(CirclePageIndicator pagerIndicator) {
        if (pagerIndicator != null) {
            mPagerIndicator = pagerIndicator;
            mPagerIndicator.setOnPageChangeListener(new SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    currentPage = position;
                }
            });
        }
    }

    /**
     * Check if the set of messages associated with this pager should send a notification.
     *
     * @return The message number that requires a notification or -1 if no notificaiton is needed.
     */
    public SmsMmsMessage shouldNotify() {
        SmsMmsMessage message;
        for (int i = 0; i < messages.size(); i++) {
            message = messages.get(i);
            if (message.shouldNotify()) {
                return message;
            }
        }

        return null;
    }

    public ArrayList<SmsMmsMessage> getMessages() {
        return messages;
    }

    public SmsMmsMessage getMessage(int i) {
        return messages.get(i);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
}
