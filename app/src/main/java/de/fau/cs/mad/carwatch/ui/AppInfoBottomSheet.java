package de.fau.cs.mad.carwatch.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.text.HtmlCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import de.fau.cs.mad.carwatch.BuildConfig;
import de.fau.cs.mad.carwatch.R;

public final class AppInfoBottomSheet {

    private AppInfoBottomSheet() {
    }

    public static void show(Activity activity) {
        ViewGroup parent = activity.findViewById(android.R.id.content);
        View contentView = activity.getLayoutInflater().inflate(R.layout.widget_app_info_dialog, parent, false);

        TextView appVersionTextView = contentView.findViewById(R.id.tv_app_version);
        appVersionTextView.setText(HtmlCompat.fromHtml(
                activity.getString(R.string.app_version, BuildConfig.VERSION_NAME),
                HtmlCompat.FROM_HTML_MODE_LEGACY
        ));
        TextView developerInfoTextView = contentView.findViewById(R.id.tv_developer_info);
        developerInfoTextView.setText(HtmlCompat.fromHtml(
                activity.getString(R.string.app_info_developer_details),
                HtmlCompat.FROM_HTML_MODE_LEGACY
        ));

        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        dialog.setContentView(contentView);

        View closeButton = contentView.findViewById(R.id.btn_sheet_close);
        if (closeButton != null) {
            closeButton.setOnClickListener(view -> dialog.dismiss());
        }

        View privacyButton = contentView.findViewById(R.id.btn_privacy_policy);
        if (privacyButton != null) {
            privacyButton.setOnClickListener(view -> openPrivacyPolicy(activity));
        }

        dialog.show();
    }

    private static void openPrivacyPolicy(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(activity.getString(R.string.app_info_privacy_policy_url)));
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            // No browser is available. The bottom sheet still shows the developer contact information.
        }
    }
}
