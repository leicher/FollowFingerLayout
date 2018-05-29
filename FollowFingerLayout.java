package leicher;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import java.lang.reflect.Field;

import leicher.file.ui.UiUtil;

/**
 * @author leicher
 * 用于悬浮窗 随手 滑动
 */
@SuppressWarnings("unused")
public class FollowFingerLayout extends FrameLayout{

    public static final int STATUS_CAN_REMOVE = 1;
    public static final int STATUS_CAN_NOT_REMOVE = 2;

    private WindowManager mManager;
    private View mContent;
    private Context mContext;
    private WindowManager.LayoutParams mParams;
    private boolean mFollowing = false;
    private MoveOutListener mMoveOutListener;
    private LocationChangedListener mLocationChangedListener;
    private ValueAnimator mAnimator;

    private int mWindowWidth;
    private int mWindowHeight;
    private int mStatusBarHeight;
    private int mTouchSlop;
    private int mOutStatus = STATUS_CAN_NOT_REMOVE;

    private Rect mPadding;
    private float mDownX;
    private float mDownY;
    private float mDeltaX;
    private float mDeltaY;

    FollowFingerLayout(@NonNull Context context) {
        super(context);
    }

    public FollowFingerLayout(View content) {
        this(content.getContext());
        this.mContext = getContext();
        this.mContent = content;
        init();
    }

    private void init(){
        mManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
        mStatusBarHeight = getStatusBarHeight(mContext);
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        mWindowWidth = metrics.widthPixels;
        mWindowHeight = metrics.heightPixels;
        mPadding = new Rect();
        addViewInLayout(mContent, 0, generateDefaultLayoutParams());
    }

    public void attachToWindow(WindowManager.LayoutParams params){
        if (params == null){
            params = new WindowManager.LayoutParams();
        }
        int margin = UiUtil.dp2px(mContext, 3f);
        FrameLayout.LayoutParams childParams = (LayoutParams) mContent.getLayoutParams();

        childParams.width = params.width;
        childParams.height = params.height;
        childParams.setMargins(margin, margin, margin, margin);
        mContent.setLayoutParams(childParams);
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mParams = params;
        mManager.addView(this, params);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAnimator != null){
            mAnimator.cancel();
        }
    }

    protected void performOnMoveOut(){
        onRemove();
        if (mMoveOutListener != null){
            mMoveOutListener.onReMove(this, mManager);
        }
    }

    protected void performOnStatusChange(int status){
        if (mMoveOutListener != null){
            mMoveOutListener.onStatusChange(this, status);
        }
    }

    protected void performLocationChanged(int x, int y){
        if (mLocationChangedListener != null){
            mLocationChangedListener.onLocationChanged(x, y);
        }
    }

    protected void performFingerRelease(){
        if (mLocationChangedListener != null){
            int targetUpper = mWindowWidth - getMeasuredWidth();
            int targetLower = 0;
            int target = mParams.x > targetUpper ? targetUpper : mParams.x < targetLower ? targetLower : mParams.x;
            mLocationChangedListener.onFingerRelease(target, mParams.y);
        }
    }


    public MoveOutListener getMoveOutListener() {
        return mMoveOutListener;
    }

    public void setMoveOutListener(MoveOutListener mMoveOutListener) {
        this.mMoveOutListener = mMoveOutListener;
    }

    public LocationChangedListener getLocationChangedListener() {
        return mLocationChangedListener;
    }

    public void setLocationChangedListener(LocationChangedListener mLocationChangedListener) {
        this.mLocationChangedListener = mLocationChangedListener;
    }

    public void detachToWindow(){
        mManager.removeView(this);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = false;
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                mFollowing = false;
                mDownX = ev.getRawX();
                mDownY = ev.getRawY();
                mDeltaX = computeDeltaX(mDownX);
                mDeltaY = computeDeltaY(mDownY);
                break;
            case MotionEvent.ACTION_MOVE:
                float x = ev.getRawX();
                float y = ev.getRawY();
                if (Math.abs(mDownY - y) >= mTouchSlop || Math.abs(mDownX - x) >= mTouchSlop) { // 满足才认为有滑动
                    intercept = true;
                }
                break;
            default:break;
        }
        return intercept || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_MOVE:
                float x = event.getRawX();
                float y = event.getRawY();
                if (Math.abs(mDownY - y) >= mTouchSlop || Math.abs(mDownX - x) >= mTouchSlop){ // 满足才认为有滑动
                    mDownX = x;
                    mDownY = y;
                    mParams.x = computeX(x);
                    mParams.y = computeY(y);
                    mManager.updateViewLayout(this, mParams);
                    invalidate();
                    performLocationChanged(mParams.x, mParams.y);
                    mFollowing = true;
                    return false;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mFollowing = false;
                if(mOutStatus == STATUS_CAN_REMOVE){
                    performOnMoveOut();
                }else {
                    reversalAnimator();
                }
                invalidate();
                break;
                default: break;
        }

        return super.onTouchEvent(event);
    }

    protected void onRemove(){

    }


    protected void reversalAnimator(){
        int targetUpper = mWindowWidth - getMeasuredWidth();
        int targetLower = 0;
        int target = mParams.x > targetUpper ? targetUpper : mParams.x < targetLower ? targetLower : mParams.x;
        if (target != mParams.x){
            mAnimator = ValueAnimator.ofInt(mParams.x, target);
            mAnimator.setDuration(200);
            mAnimator.setInterpolator(new LinearInterpolator());
            mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mParams.x = (int) animation.getAnimatedValue();
                    mManager.updateViewLayout(FollowFingerLayout.this, mParams);
                }
            });
            mAnimator.start();
        }
    }


    @SuppressLint("RtlHardcoded")
    protected float computeDeltaX(float x){
        if (horizontalReversal()){
            return (mWindowWidth - x) - mParams.x;
        }
        return mDownX - mParams.x;
    }

    @SuppressLint("RtlHardcoded")
    protected float computeDeltaY(float y){
        if (verticalReversal()){
            return (mWindowHeight - y) - mParams.y;
        }
        return y - mParams.y;
    }

    /**
     * 计算 window 的 x 位置
     * @param x
     * @return
     */
    @SuppressLint("RtlHardcoded")
    protected int computeX(float x){
        boolean b = horizontalReversal();
        int res = b ? (int) ((mWindowWidth - x) - mDeltaX + 0.5f) : (int) (x - mDeltaX + 0.5f);
        int upper = mWindowWidth - getMeasuredWidth() / 2;
        int lower = -getMeasuredWidth() / 2;
        if (b){
            upper -= mPadding.left;
            lower += mPadding.right;
        }else {
            upper -= mPadding.right;
            lower += mPadding.left;
        }

        //res = limit(res, upper, lower);
        if (res >= upper || res <= lower){
            if (mOutStatus == STATUS_CAN_NOT_REMOVE)
            performOnStatusChange(mOutStatus = STATUS_CAN_REMOVE);
        }else {
            if (mOutStatus == STATUS_CAN_REMOVE)
                performOnStatusChange(mOutStatus = STATUS_CAN_NOT_REMOVE);
        }
        return res;
    }

    /**
     * 计算 window 的 y 位置
     * @param y
     * @return
     */
    protected int computeY(float y){
        boolean b = verticalReversal();
        int res = b ? (int) ((mWindowHeight - y) - mDeltaY + 0.5f) : (int) (y - mDeltaY + 0.5f);
        int upper = mWindowHeight - getMeasuredHeight();
        int lower = 0;
        if (b) {
            upper = upper - mStatusBarHeight;
            upper -= mPadding.top;
            lower += mPadding.bottom;
        }else {
            // 不同的WindowManager.LayoutParams 的type可能 0 位置不一样,可酌情处理
            //lower = mStatusBarHeight;
            upper -= mStatusBarHeight;
            upper -= mPadding.bottom;
            lower += mPadding.top;
        }
        res = limit(res, upper, lower);
        return res;
    }

    protected boolean horizontalReversal(){
        return (mParams.gravity & Gravity.RIGHT) == Gravity.RIGHT;
    }

    protected boolean verticalReversal(){
        return (mParams.gravity & Gravity.BOTTOM) == Gravity.BOTTOM;
    }

    private static int limit(int target, int upper, int lower){
        return target > upper ? upper : target < lower ? lower : target;
    }

    /**
     * 用于滑出屏幕移除的监听
     */
    public interface MoveOutListener{

        void onStatusChange(FollowFingerLayout layout, int status);

        void onReMove(FollowFingerLayout layout, WindowManager manager);

    }

    /**
     * 用于监听位置改变等
     */
    public interface LocationChangedListener{

        void onLocationChanged(int x, int y);

        void onFingerRelease(int x, int y);
    }


    /**
     * 获取状态栏/通知栏的高度
     *
     * @return
     */
    @SuppressLint("PrivateApi")
    public static int getStatusBarHeight(Context context) {
        int x,bar = 0;
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Field field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(null).toString());
            bar = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bar;
    }
}
