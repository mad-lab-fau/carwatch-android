package de.fau.cs.mad.carwatch.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import de.fau.cs.mad.carwatch.BuildConfig;
import de.fau.cs.mad.carwatch.R;

public class AppInfoDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View rootView = inflater.inflate(R.layout.widget_app_info_dialog, null);

        TextView appVersionTextView = rootView.findViewById(R.id.tv_app_version);
        appVersionTextView.setText(Html.fromHtml(getString(R.string.app_version, BuildConfig.VERSION_NAME)));

        AlertDialog dialog = new CarwatchDialogBuilder(requireContext())
                .setTitle(R.string.menu_app_info)
                .setView(rootView)
                .setPositiveButton(R.string.ok, null)
                .create();

        dialog.setOnShowListener(shownDialog -> CarwatchDialogBuilder.applyCarwatchWindowBackground(
                dialog,
                requireContext()
        ));

        return dialog;
    }
}
