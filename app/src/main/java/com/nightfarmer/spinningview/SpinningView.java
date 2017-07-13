package com.nightfarmer.spinningview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PointF;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangfan on 17-7-11.
 */

public class SpinningView extends ViewGroup {

    SpinningAdapter adapter;
    List<View> childList = new ArrayList<>();
    private int itemCount = 0;

    int size = 300;//组件尺寸
    int dist = 400;//镜头深度
    private double angdegX = Math.toRadians(0);
    private double angdegZ = Math.toRadians(0);
    private int mMaxFlingVelocity;
    private VelocityTracker vTracker;

    private int measureWidth;
    private int measureHeight;
    private ValueAnimator valueAnimator;

    private OnScrollListener onScrollListener;

    public SpinningView(Context context) {
        this(context, null);
    }

    public SpinningView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpinningView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        final ViewConfiguration vc = ViewConfiguration.get(context);
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    public void setSize(int size) {
        this.size = size;
        requestLayout();
    }

    public void setDist(int dist) {
        this.dist = dist;
        requestLayout();
    }

    public void setAngdegX(double angdegX) {
        this.angdegX = Math.toRadians(angdegX);
        requestLayout();
    }

    public void setAngdegZ(double angdegZ) {
        this.angdegZ = Math.toRadians(angdegZ);
        requestLayout();
    }

    public void setAdapter(SpinningAdapter adapter) {
        this.adapter = adapter;
        itemCount = adapter.getItemCount();
        childList.clear();
        for (int i = 0; i < itemCount; i++) {
            View view = adapter.onCreateItemView(this, i);
            childList.add(view);
//            view.setOnTouchListener(onItemTouchListener);
        }
        resetChildren();
    }

    private void resetChildren() {
        removeAllViews();
        for (View view : childList) {
            addView(view);
        }
    }

    float preX = 0;
//    float preY = 0;


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            PointF pointF = rotateByZValue(ev.getX() - measureWidth / 2, ev.getY() - measureWidth / 2, -angdegZ);
            preX = pointF.x;
            if (vTracker != null) {
                vTracker.clear();
            }
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        PointF pointF = rotateByZValue(event.getX() - measureWidth / 2, event.getY() - measureWidth / 2, -angdegZ);
        int action = event.getAction();
        if (vTracker == null) {
            vTracker = VelocityTracker.obtain();
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (vTracker != null) {
                    vTracker.clear();
                }
                vTracker.addMovement(event);
                break;
            case MotionEvent.ACTION_MOVE:
                cancelAnim();
                float L = pointF.x - preX;
//                angleOffset += L;
//                L=nπR/180
//                L代表弧长,R代表半径,n代表圆心角的度数
                angleOffset += (L * 180 / Math.PI / size);
//                Log.i("angleOffset", "" + angleOffset);

                vTracker.addMovement(event);
//                        vTracker.computeCurrentVelocity(1000);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:


                vTracker.addMovement(event);
                vTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                float yVelocity = vTracker.getYVelocity();
                float xVelocity = vTracker.getXVelocity();
//                Log.i("xxx", "zzz " + yVelocity + " x " + mMaxFlingVelocity);

                PointF velocityF = rotateByZValue(xVelocity, yVelocity, -angdegZ);
                float velocity = velocityF.x;
                int sign = velocity > 0 ? 1 : -1;
//                L=nπR/180 //算出来两个item之间的弧长
                float pAngle = 360f / itemCount;//间隔角度
                double pl = pAngle * Math.PI * size / 180;//间隔弧长
                double n = sign * Math.ceil((velocity * sign * 0.1 / pl) - 0.25);

                float startValue = angleOffset;
                float endValue = (float) (startValue + (n * pAngle));
                float round = Math.round(endValue / pAngle);
                endValue = (int) (round * pAngle);

                float diffValue = endValue - startValue;
                if (diffValue == 0 && velocity == 0) {
//                    Log.i("nnn", "" + n + " = " + event.getAction() + " = " + round + " = " + diffValue + " = " + angleOffset + " = " + pointF.x);
                    endValue += (pointF.x > 0 ? -1 : 1) * pAngle;
                }
                cancelAnim();
                valueAnimator = ValueAnimator.ofFloat(startValue, endValue);
                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        angleOffset = (Float) animation.getAnimatedValue();
                        requestLayout();
                    }
                });
                valueAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (valueAnimator != null) {
                            valueAnimator = null;
                            if (onScrollListener != null) {
                                float currentAngleOffset = (Float) ((ValueAnimator) animation).getAnimatedValue();
                                if (currentAngleOffset < 0) {
                                    currentAngleOffset += (Math.ceil(Math.abs(currentAngleOffset) / 360) * 360);
                                }
                                currentAngleOffset %= 360;
                                float pAngle = 360f / itemCount;//间隔角度
                                int position = (int) (currentAngleOffset / pAngle);
                                onScrollListener.onItemChecked(position);
                            }
                        }
                    }
                });
                valueAnimator.setInterpolator(new LinearOutSlowInInterpolator());
                valueAnimator.setDuration(500);
                valueAnimator.start();
                vTracker.clear();
                break;
        }
        preX = pointF.x;

        requestLayout();

        return true;
    }

    private void cancelAnim() {
        if (valueAnimator != null) {
            ValueAnimator anim = valueAnimator;
            valueAnimator = null;
            anim.cancel();
        }
    }


    private float angleOffset = 0f;

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int centerX = (right - left) / 2;
        int centerY = (bottom - top) / 2;
        for (int i = 0; i < childList.size(); i++) {
            View view = childList.get(i);

            double angleView = -90 - (360.0 / itemCount) * i;
            angleView += angleOffset;
            if (angleView < 0) {
                angleView += (Math.ceil(Math.abs(angleView) / 360) * 360);
            }
            angleView = angleView % 360;

            float x = (float) Math.cos(Math.toRadians(angleView)) * size;
            float y = (float) Math.sin(Math.toRadians(angleView)) * size;

            int measuredHeight = view.getMeasuredHeight();
            int measuredWidth = view.getMeasuredWidth();
            int v_left = centerX - measuredWidth / 2;
            int v_top = centerY - measuredHeight / 2;
            int v_right = centerX + measuredWidth / 2;
            int v_bottom = centerY + measuredHeight / 2;
            view.layout(v_left, v_top, v_right, v_bottom);

            view.setTranslationX(x);
            view.setTranslationY((float) (Math.cos(angdegX) * y));

            double z = Math.sin(angdegX) * y;
            float scale = (float) ((dist - z) / dist);

            view.setScaleX(scale);//对view进行缩放
            view.setScaleY(scale);//对view进行缩放
            ViewCompat.setZ(view, scale);

            float translationX = view.getTranslationX();
            float translationY = view.getTranslationY();
//
            PointF newTrans = rotateByZ(translationX, translationY);
            view.setTranslationX(newTrans.x);
            view.setTranslationY(newTrans.y);

        }
    }

//    private OnTouchListener onItemTouchListener = new OnTouchListener() {
//        @Override
//        public boolean onTouch(View view, MotionEvent motionEvent) {
//            return false;
//        }
//    };

    private PointF rotateByZ(float valueX, float valueY) {
        return rotateByZValue(valueX, valueY, angdegZ);
    }

    private PointF rotateByZValue(float valueX, float valueY, double angdegZ) {
//        x1=x0cosa-y0sina
//        y1=x0sina+y0cosa
        float x = (float) (valueX * Math.cos(angdegZ) - valueY * Math.sin(angdegZ));
        float y = (float) (valueX * Math.sin(angdegZ) + valueY * Math.cos(angdegZ));
        return new PointF(x, y);
    }

    /**
     * 计算控件的大小
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        measureWidth = measureWidth(widthMeasureSpec);
        measureHeight = measureHeight(heightMeasureSpec);

        // 计算自定义的ViewGroup中所有子控件的大小
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        // 设置自定义的控件MyViewGroup的大小
        setMeasuredDimension(measureWidth, measureHeight);
    }

    private int measureWidth(int pWidthMeasureSpec) {
        int result = getSuggestedMinimumWidth();
        int widthMode = MeasureSpec.getMode(pWidthMeasureSpec);// 得到模式
        int widthSize = MeasureSpec.getSize(pWidthMeasureSpec);// 得到尺寸

        switch (widthMode) {
            /**
             * mode共有三种情况，取值分别为MeasureSpec.UNSPECIFIED, MeasureSpec.EXACTLY,
             * MeasureSpec.AT_MOST。
             *
             *
             * MeasureSpec.EXACTLY是精确尺寸，
             * 当我们将控件的layout_width或layout_height指定为具体数值时如andorid
             * :layout_width="50dip"，或者为FILL_PARENT是，都是控件大小已经确定的情况，都是精确尺寸。
             *
             *
             * MeasureSpec.AT_MOST是最大尺寸，
             * 当控件的layout_width或layout_height指定为WRAP_CONTENT时
             * ，控件大小一般随着控件的子空间或内容进行变化，此时控件尺寸只要不超过父控件允许的最大尺寸即可
             * 。因此，此时的mode是AT_MOST，size给出了父控件允许的最大尺寸。
             *
             *
             * MeasureSpec.UNSPECIFIED是未指定尺寸，这种情况不多，一般都是父控件是AdapterView，
             * 通过measure方法传入的模式。
             */
            case MeasureSpec.UNSPECIFIED:
                result = result;
                break;
            case MeasureSpec.AT_MOST:
                result = size;
                break;
            case MeasureSpec.EXACTLY:
                result = widthSize;
                break;
        }
        return result;
    }

    private int measureHeight(int pHeightMeasureSpec) {
        int result = getSuggestedMinimumHeight();

        int heightMode = MeasureSpec.getMode(pHeightMeasureSpec);
        int heightSize = MeasureSpec.getSize(pHeightMeasureSpec);

        switch (heightMode) {
            case MeasureSpec.UNSPECIFIED:
//                Log.i("measureHeight", "UNSPECIFIED");
                result = result;
                break;
            case MeasureSpec.AT_MOST:
//                Log.i("measureHeight", "AT_MOST");
                result = size;
                break;
            case MeasureSpec.EXACTLY:
                result = heightSize;
//                Log.i("measureHeight", "EXACTLY" + heightSize);
                break;
        }
        return result;
    }


    public interface OnScrollListener {
        void onItemChecked(int position);
    }
}
