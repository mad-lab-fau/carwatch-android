package de.fau.cs.mad.carwatch.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import de.fau.cs.mad.carwatch.Constants;

public final class HeaderUiHelper {

    private HeaderUiHelper() {
    }

    public static void configureTransparentStatusBar(Activity activity, SharedPreferences sharedPreferences) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), true);
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        boolean isNightModeEnabled = sharedPreferences.getBoolean(Constants.PREF_NIGHT_MODE_ENABLED, false);
        new WindowInsetsControllerCompat(activity.getWindow(), activity.getWindow().getDecorView())
                .setAppearanceLightStatusBars(!isNightModeEnabled);
    }
}
