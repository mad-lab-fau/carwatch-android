package de.fau.cs.mad.carwatch.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.fau.cs.mad.carwatch.R;

public class CarwatchDialogBuilder extends MaterialAlertDialogBuilder {

    private final Context context;

    public CarwatchDialogBuilder(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    @NonNull
    @Override
    public AlertDialog show() {
        AlertDialog dialog = super.show();
        applyCarwatchWindowBackground(dialog, context);
        return dialog;
    }

    public static void applyCarwatchWindowBackground(@NonNull AlertDialog dialog, @NonNull Context context) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.bg_study_information_dialog));
        }
        styleDialogButton(dialog.getButton(AlertDialog.BUTTON_POSITIVE), context);
        styleDialogButton(dialog.getButton(AlertDialog.BUTTON_NEGATIVE), context);
        styleDialogButton(dialog.getButton(AlertDialog.BUTTON_NEUTRAL), context);
    }

    private static void styleDialogButton(Button button, Context context) {
        if (button == null) {
            return;
        }
        button.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_dialog_button));
        button.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onSurface));
        button.setMinHeight(dp(context, 40));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(context, 18), 0, dp(context, 18), 0);
        button.setAllCaps(false);
        button.setSingleLine(false);
        button.setMaxLines(2);
        button.setEllipsize(TextUtils.TruncateAt.END);
        ViewGroup.LayoutParams layoutParams = button.getLayoutParams();
        if (layoutParams != null) {
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            button.setLayoutParams(layoutParams);
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
