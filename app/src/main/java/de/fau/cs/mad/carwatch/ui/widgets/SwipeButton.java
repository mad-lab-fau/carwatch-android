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
        void onSwipe();
    }

    private final TextView stopTextView;
    private final ImageView slidingButton;

    private final Drawable stopDrawable;
    private final Drawable ringDrawable;

    private float initialX = 0;

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

        stopTextView = findViewById(R.id.tv_stop);
        slidingButton = findViewById(R.id.iv_slide);

        String stopText = attributes.getString(R.styleable.SwipeButton_text);
        if (stopText == null) {
            stopText = context.getString(R.string.stop);
        }

        stopTextView.setText(stopText);

        stopDrawable = ContextCompat.getDrawable(context, attributes.getResourceId(R.styleable.SwipeButton_iconLeft, R.drawable.ic_stop_black_24dp));
        ringDrawable = ContextCompat.getDrawable(context, attributes.getResourceId(R.styleable.SwipeButton_iconRight, R.drawable.ic_ring_black_24dp));

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
        if (motionEvent.getX() > initialX + slidingButton.getWidth() / 2.0f && motionEvent.getX() + slidingButton.getWidth() / 2.0f + initialX < getWidth()) {
            slidingButton.setX(motionEvent.getX() - slidingButton.getWidth() / 2.0f);
            float alpha = 1.0f / (2 * initialX - getWidth() + slidingButton.getWidth()) * (slidingButton.getX() + initialX - getWidth() + slidingButton.getWidth());
            stopTextView.setAlpha(alpha);
        }

        // move out of right bounds
        if (motionEvent.getX() + slidingButton.getWidth() / 2.0f + initialX >= getWidth()) {
            slidingButton.setX(getWidth() - slidingButton.getWidth() - initialX);
            stopTextView.setAlpha(0.0f);
        }

        // move out of left bounds
        if (motionEvent.getX() < initialX) {
            slidingButton.setPivotX(0);
            ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(slidingButton, "scaleX", 0.95f, 1.0f);
            scaleXAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleXAnimator.start();
            slidingButton.setX(initialX);
        }
    }

    private void releaseButton() {
        if (slidingButton.getX() > (getWidth() - slidingButton.getWidth() - initialX) * 0.75f) {
            expandButton();
        } else {
            moveButtonBack();
        }
    }

    private void moveButtonBack() {
        final ValueAnimator positionAnimator = ValueAnimator.ofFloat(slidingButton.getX(), initialX);
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

        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(stopTextView, "alpha", 1.0f);
        positionAnimator.setDuration(200);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(positionAnimator, alphaAnimator);
        animatorSet.start();
    }

    private void expandButton() {
        // move sliding button to end of bar
        slidingButton.setX(getWidth() - slidingButton.getWidth() - initialX);
        stopTextView.setAlpha(0.0f);

        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(slidingButton, "scaleX", 2.0f, 1.0f);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(slidingButton, "scaleY", 2.0f, 1.0f);
        scaleXAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleYAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationEnd(animation);
                slidingButton.setImageDrawable(stopDrawable);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (swipeListener != null) {
                    swipeListener.onSwipe();
                }
            }
        });

        animatorSet.playTogether(scaleXAnimator, scaleYAnimator);
        animatorSet.start();
    }
}
