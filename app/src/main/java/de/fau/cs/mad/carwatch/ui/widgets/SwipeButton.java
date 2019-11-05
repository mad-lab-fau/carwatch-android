package de.fau.cs.mad.carwatch.ui.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import de.fau.cs.mad.carwatch.R;

public class SwipeButton extends RelativeLayout implements View.OnTouchListener {

    private static final String TAG = SwipeButton.class.getSimpleName();


    public interface OnSwipeListener {
        void onSwipeLeft();

        void onSwipeRight();
    }

    private TextView leftTextView;
    private TextView rightTextView;
    private ImageView slidingButton;

    private Drawable leftDrawable;
    private Drawable rightDrawable;
    private Drawable ringDrawable;

    private float initialX = 0;
    private boolean active;

    private OnSwipeListener swipeListener;

    public SwipeButton(Context context) {
        this(context, null);
    }

    public SwipeButton(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public SwipeButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, -1);
    }

    public SwipeButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        inflate(context, R.layout.widget_swipe_button, this);

        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.SwipeButton);

        leftTextView = findViewById(R.id.tv_left);
        rightTextView = findViewById(R.id.tv_right);
        slidingButton = findViewById(R.id.iv_slide);

        String leftText = attributes.getString(R.styleable.SwipeButton_textLeft);
        if (leftText == null) {
            leftText = context.getString(R.string.snooze);
        }
        String rightText = attributes.getString(R.styleable.SwipeButton_textRight);
        if (rightText == null) {
            rightText = context.getString(R.string.stop);
        }

        leftTextView.setText(leftText);
        rightTextView.setText(rightText);

        leftDrawable = ContextCompat.getDrawable(context, attributes.getResourceId(R.styleable.SwipeButton_iconLeft, R.drawable.ic_snooze_black_24dp));
        rightDrawable = ContextCompat.getDrawable(context, attributes.getResourceId(R.styleable.SwipeButton_iconRight, R.drawable.ic_stop_black_24dp));
        ringDrawable = ContextCompat.getDrawable(context, attributes.getResourceId(R.styleable.SwipeButton_iconCenter, R.drawable.ic_ring_black_24dp));

        attributes.recycle();

        setOnTouchListener(this);
    }

    public void setOnSwipeListener(OnSwipeListener swipeListener) {
        this.swipeListener = swipeListener;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                moveButton(motionEvent);
                return true;
            case MotionEvent.ACTION_UP:
                releaseButton();
                return true;
        }
        return false;
    }

    private void moveButton(MotionEvent motionEvent) {
        if (initialX == 0.0f) {
            initialX = slidingButton.getX();
        }

        // move to right
        if (motionEvent.getX() > initialX + slidingButton.getWidth() / 2.0f && motionEvent.getX() + slidingButton.getWidth() / 2.0f < getWidth()) {
            slidingButton.setX(motionEvent.getX() - slidingButton.getWidth() / 2.0f);
            float alpha = 2.0f - 2.0f * (slidingButton.getX() + slidingButton.getWidth() / 2.0f) / getWidth();
            rightTextView.setAlpha(alpha);
        }

        // move to left
        if (motionEvent.getX() < initialX + slidingButton.getWidth() / 2.0f && motionEvent.getX() - slidingButton.getWidth() / 2.0f > 0) {
            slidingButton.setX(motionEvent.getX() - slidingButton.getWidth() / 2.0f);
            float alpha = 2.0f * (slidingButton.getX() + slidingButton.getWidth() / 2.0f) / getWidth();
            leftTextView.setAlpha(alpha);
        }

        // move out of right bounds
        if (motionEvent.getX() + slidingButton.getWidth() / 2.0f > getWidth() && slidingButton.getX() + slidingButton.getWidth() / 2.0f < getWidth()) {
            slidingButton.setX(getWidth() - slidingButton.getWidth());
            rightTextView.setAlpha(0.0f);
        }

        // move out of left bounds
        if (motionEvent.getX() < slidingButton.getWidth() / 2.0f && slidingButton.getX() > 0.0f) {
            slidingButton.setX(0.0f);
            leftTextView.setAlpha(0.0f);
        }
    }

    private void releaseButton() {
        boolean isRight = slidingButton.getX() > getWidth() / 2.0f;
        if (active) {
            moveButtonBack(isRight);
        } else {
            // handle right
            if (slidingButton.getX() + slidingButton.getWidth() / 2.0f > getWidth() * 0.85f) {
                expandButton(true);
            } else if (slidingButton.getX() + slidingButton.getWidth() / 2.0f < getWidth() * 0.15f) {
                expandButton(false);
            } else {
                moveButtonBack(isRight);
            }
        }

    }

    private void moveButtonBack(boolean isRight) {
        final ValueAnimator positionAnimator = ValueAnimator.ofFloat(slidingButton.getX(), (getWidth() - slidingButton.getWidth()) / 2.0f);
        positionAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        positionAnimator.addUpdateListener(valueAnimator -> {
            float x = (Float) positionAnimator.getAnimatedValue();
            slidingButton.setX(x);
        });

        positionAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                slidingButton.setImageDrawable(ringDrawable);
            }
        });

        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(isRight ? rightTextView : leftTextView, "alpha", 1.0f);
        positionAnimator.setDuration(200);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(positionAnimator, alphaAnimator);
        animatorSet.start();

        active = false;
    }

    private void expandButton(final boolean isRight) {
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(slidingButton, "scaleX", 2.0f, 1.0f);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(slidingButton, "scaleY", 2.0f, 1.0f);
        scaleXAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleYAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationEnd(animation);
                slidingButton.setImageDrawable(isRight ? rightDrawable : leftDrawable);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                active = true;
                if (swipeListener != null) {
                    if (isRight) {
                        swipeListener.onSwipeRight();
                    } else {
                        swipeListener.onSwipeLeft();
                    }
                }
            }
        });

        animatorSet.playTogether(scaleXAnimator, scaleYAnimator);
        animatorSet.start();
    }
}
