package net.everythingandroid.smspopup.controls;

import java.util.ArrayList;

import net.everythingandroid.smspopup.controls.SmsPopupView.OnReactToMessage;
import net.everythingandroid.smspopup.provider.SmsMmsMessage;
import net.everythingandroid.smspopup.util.Log;
import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.View;

import com.viewpagerindicator.CirclePageIndicator;

public class SmsPopupPager extends ViewPager implements OnPageChangeListener {

    private ArrayList<SmsMmsMessage> messages;
    private int currentPage;
    private MessageCountChanged messageCountChanged;
    private Context mContext;
    private SmsPopupPagerAdapter mAdapter;
    private CirclePageIndicator mPagerIndicator;
    private int privacyMode;
    private OnReactToMessage mOnReactToMessage;

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
        mAdapter = new SmsPopupPagerAdapter();
        setAdapter(mAdapter);
        currentPage = 0;
    }

    public void setOnReactToMessage(OnReactToMessage r) {
        mOnReactToMessage = r;
    }

    public int getPageCount() {
        return messages.size();
    }

    /**
     * Add a message and its view to the end of the list of messages.
     * 
     * @param newMessage
     *            The message to add.
     */
    public void addMessage(SmsMmsMessage newMessage) {
        messages.add(newMessage);
        UpdateMessageCount();
    }

    /**
     * Add a list of new messages to the end of the current message list.
     * 
     * @param newMessages
     *            The list of new messages to add.
     */
    public void addMessages(ArrayList<SmsMmsMessage> newMessages) {
        if (newMessages != null) {
            messages.addAll(newMessages);
            UpdateMessageCount();
        }
    }

    /**
     * Remove a specific message from the list, if there is only one message left then it will not
     * be removed.
     * 
     * @param numMessage
     * @return true if a message was removed, false otherwise.
     */
    public synchronized boolean removeMessage(int numMessage) {
        final int totalMessages = getPageCount();

        if (totalMessages <= 1)
            return false;
        if (numMessage >= totalMessages || numMessage < 0)
            return false;

        if (numMessage < currentPage && currentPage != (totalMessages - 1)) {
            currentPage--;
        }

        messages.remove(numMessage);
        mAdapter.notifyDataSetChanged();
        UpdateMessageCount();

        return true;
    }

    /**
     * Remove the currently active message, if there is only one message left then it will not be
     * removed.
     * 
     * @return true if a message was removed, false otherwise.
     */
    public boolean removeActiveMessage() {
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
        if (Log.DEBUG)
            Log.v("showNext() - " + currentPage + ", " + getActiveMessage().getContactName());
    }

    public void showPrevious() {
        if (currentPage > 0) {
            setCurrentItem(currentPage - 1);
        }
        if (Log.DEBUG)
            Log.v("showPrevious() - " + currentPage + ", " + getActiveMessage().getContactName());
    }

    @Override
    public void setCurrentItem(int num) {
        super.setCurrentItem(num);
        currentPage = num;
        UpdateMessageCount();
    }

    @Override
    public void onPageScrollStateChanged(int state) {}

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // super.onPageScrolled(position, positionOffset, positionOffsetPixels);
    }

    @Override
    public void onPageSelected(int position) {
        currentPage = position;
        UpdateMessageCount();
    }

    private class SmsPopupPagerAdapter extends PagerAdapter {

        @Override
        public void finishUpdate(View container) {}

        @Override
        public int getCount() {
            return getPageCount();
        }

        @Override
        public Object instantiateItem(View container, int position) {
            SmsPopupView mView = new SmsPopupView(mContext, messages.get(position), privacyMode);
            mView.setOnReactToMessage(mOnReactToMessage);
            ((ViewPager) container).addView(mView);
            return mView;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            ((ViewPager) container).removeView((SmsPopupView) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((SmsPopupView) object);
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {}

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void startUpdate(View container) {}

        @Override
        public int getItemPosition(Object object) {
            int idx = messages.indexOf(object);
            if (idx == -1) {
                return PagerAdapter.POSITION_NONE;
            }
            return idx;
        }

    }

    public void setIndicator(CirclePageIndicator pagerIndicator) {
        if (pagerIndicator != null) {
            mPagerIndicator = pagerIndicator;
            mPagerIndicator.setOnPageChangeListener(this);
        }
    }

    public void setPrivacy(int mode) {
        privacyMode = mode;
        for (int i = 0; i < getChildCount(); i++) {
            ((SmsPopupView) getChildAt(i)).setPrivacy(mode);
        }
    }
}
