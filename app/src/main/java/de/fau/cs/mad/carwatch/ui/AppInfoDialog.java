package de.fau.cs.mad.carwatch.ui;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import de.fau.cs.mad.carwatch.BuildConfig;
import de.fau.cs.mad.carwatch.R;

public class AppInfoDialog extends DialogFragment implements View.OnClickListener {

    private static final String TAG = AppInfoDialog.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.widget_app_info_dialog, container);

        Button okButton = rootView.findViewById(R.id.button_ok);
        okButton.setOnClickListener(this);

        TextView appVersionTextView = rootView.findViewById(R.id.tv_app_version);
        appVersionTextView.setText(Html.fromHtml(getResources().getString(R.string.app_version, BuildConfig.VERSION_NAME)));

        return rootView;
    }


    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_ok) {
            dismiss();
        }
    }
}
