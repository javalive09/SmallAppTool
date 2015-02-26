package com.peter.smallapptool;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.Scroller;
import android.widget.TextView;

public class HorizontalPullView extends ViewGroup {

	private Scroller mScroller;
	private int mTouchSlop;
	private static final int STATE_IDLE = 0;
	private static final int STATE_DRAGGING = 1;
	private static final int STATE_SETTLING = 2;
	private static final int mAnimTime = 600;
	private int mTouchState = STATE_IDLE;
	private boolean mFinish;
	private int mStartY;
	private int mDeltaY;
	private boolean mCanPull;
	private boolean canFinish;
	private ScrollListener mScrollListener;
	private TextView mTextView;

	public HorizontalPullView(Context context) {
		super(context);
		init();
	}

	public HorizontalPullView(Context context, AttributeSet attrs) {
		super(context, attrs, 0);
		init();
	}

	private void init() {
		mScroller = new Scroller(getContext());
		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {
			int mScrollerX = mScroller.getCurrX();
			int mScrollerY = mScroller.getCurrY();
			scrollTo(mScrollerX, mScrollerY);
			invalidate();
		}else if(mFinish){
			if (canFinish && mScrollListener != null) {
				Log.i("peter", "end======");
				mScrollListener.scrollEnd();
				mFinish = false;
				canFinish = false;
			}
		}
	}

	public boolean onInterceptTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		final int currentY = (int) ev.getY();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mFinish = false;
			canFinish = false;
			mStartY = currentY;
			if (mScroller.isFinished()) {
				mTouchState = STATE_IDLE;
			} else {
				mTouchState = STATE_SETTLING;
			}
			break;
		case MotionEvent.ACTION_MOVE:
			final int deltaY = currentY - mStartY;
			
			if (mMoveToTop && deltaY > mTouchSlop) {
				mTouchState = STATE_DRAGGING;
			}
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			mTouchState = STATE_IDLE;
			break;
		}
		return mTouchState == STATE_DRAGGING;
	}

	@SuppressLint("ClickableViewAccessibility")
	public boolean onTouchEvent(MotionEvent event) {
		final int action = event.getAction();
		final int currentY = (int) event.getY();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if (mCanPull) {
				return true;
			} else {
				return false;
			}
		case MotionEvent.ACTION_MOVE:
			int deltaY = currentY - mStartY;
			Log.i("peter", "deltaY = " + deltaY);
			int textViewH = mTextView.getMeasuredHeight();
			mDeltaY = deltaY;
			if (deltaY > textViewH) {
				mDeltaY = textViewH;
			}
			scrollTo(0, -mDeltaY);
			if (Math.abs(mDeltaY) >= mTextView.getMeasuredHeight()) {
				mTextView.setText("现在抬手全部清理");
				canFinish = true;
			} else {
				mTextView.setText("继续下拉全部清理");
				canFinish = false;
			}
			break;
		case MotionEvent.ACTION_UP:
			deltaY = -getScrollY();
			mScroller.startScroll(getScrollX(), getScrollY(), 0, deltaY,
					mAnimTime);
			invalidate();
			mFinish = true;
			break;
		}
		return true;
	}
	
	public void setScrollListener(ScrollListener listener) {
		mScrollListener = listener;
	}

	public void setTextView(TextView tv) {
		mTextView = tv;
	}

	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		measureChildren(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		ListView lv = (ListView) getChildAt(0);
		lv.layout(l, t, r, b);
		lv.setOnScrollListener(mListener);
	}

	OnScrollListener mListener = new OnScrollListener() {

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {

		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			int top = view.getChildAt(0).getTop();
			Log.i("peter", "top = " + top);
			if (top == 0) {
				Log.i("log", "滑到顶部");
				mMoveToTop = true;
			} else {
				mMoveToTop = false;
			}
		}
	};

	private boolean mMoveToTop = true;

	public static interface ScrollListener {
		void scrollEnd();
	}

}