package de.fau.cs.mad.carwatch.ui;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;

import de.fau.cs.mad.carwatch.R;

public final class CarwatchSnackbar {

    public static final int LENGTH_SHORT = Snackbar.LENGTH_SHORT;
    public static final int LENGTH_LONG = Snackbar.LENGTH_LONG;

    private static final int BOTTOM_NAV_GAP_DP = 12;
    private static final int DEFAULT_BOTTOM_NAV_HEIGHT_DP = 80;

    private CarwatchSnackbar() {
    }

    public static Snackbar show(@NonNull View anchor, @StringRes int messageId, int duration) {
        return show(anchor, anchor.getContext().getText(messageId), duration);
    }

    public static Snackbar show(@NonNull View anchor, @NonNull CharSequence message, int duration) {
        Snackbar snackbar = Snackbar.make(anchor, message, duration);
        positionAboveBottomNavigation(snackbar, anchor);
        snackbar.show();
        return snackbar;
    }

    private static void positionAboveBottomNavigation(@NonNull Snackbar snackbar, @NonNull View anchor) {
        View snackbarView = snackbar.getView();
        View rootView = anchor.getRootView();
        View bottomNavigation = rootView != null ? rootView.findViewById(R.id.nav_view) : null;

        int bottomMargin = getViewHeight(bottomNavigation) + dp(anchor, BOTTOM_NAV_GAP_DP);
        ViewGroup.LayoutParams layoutParams = snackbarView.getLayoutParams();
        if (layoutParams instanceof CoordinatorLayout.LayoutParams coordinatorParams) {
            coordinatorParams.gravity = Gravity.BOTTOM;
            coordinatorParams.bottomMargin = Math.max(coordinatorParams.bottomMargin, bottomMargin);
            snackbarView.setLayoutParams(coordinatorParams);
        } else if (layoutParams instanceof ViewGroup.MarginLayoutParams marginParams) {
            marginParams.bottomMargin = Math.max(marginParams.bottomMargin, bottomMargin);
            snackbarView.setLayoutParams(marginParams);
        }
    }

    private static int getViewHeight(View view) {
        if (view == null || view.getVisibility() == View.GONE) {
            return 0;
        }
        if (view.getHeight() > 0) {
            return view.getHeight();
        }
        if (view.getMeasuredHeight() > 0) {
            return view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams != null && layoutParams.height > 0) {
            return layoutParams.height;
        }
        return dp(view, DEFAULT_BOTTOM_NAV_HEIGHT_DP);
    }

    private static int dp(@NonNull View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }
}
