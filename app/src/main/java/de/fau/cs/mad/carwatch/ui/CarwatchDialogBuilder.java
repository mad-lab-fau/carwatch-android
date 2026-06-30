package de.fau.cs.mad.carwatch.ui;

import android.content.Context;
import android.graphics.text.LineBreaker;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CarwatchDialogBuilder extends MaterialAlertDialogBuilder {

    public CarwatchDialogBuilder(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public AlertDialog create() {
        AlertDialog dialog = super.create();
        dialog.setOnShowListener(currentDialog -> justifyMessageText(dialog));
        return dialog;
    }

    @NonNull
    @Override
    public AlertDialog show() {
        AlertDialog dialog = super.show();
        justifyMessageText(dialog);
        return dialog;
    }

    private static void justifyMessageText(@NonNull AlertDialog dialog) {
        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView == null) {
            return;
        }

        messageView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            messageView.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD);
        }
    }
}
