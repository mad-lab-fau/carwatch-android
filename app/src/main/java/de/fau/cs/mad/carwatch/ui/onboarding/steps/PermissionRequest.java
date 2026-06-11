package de.fau.cs.mad.carwatch.ui.onboarding.steps;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import de.fau.cs.mad.carwatch.R;
/**
 * create an instance of this fragment.
 */
public class PermissionRequest extends BaseWelcomeSlide {

    public PermissionRequest() {
        super();
        canShowNextSlide.set(true);
        canShowPreviousSlide.set(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        if (root == null)
            return null;

        LinearLayout notificationText = root.findViewById(R.id.ll_notification_permission_description);
        // notification permission is only required for Android 13 and above
        // so we hide the notification permission description for older versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            notificationText.setVisibility(View.GONE);
        }

        return root;
    }

    @Override
    protected int getResourceId() {
        return R.layout.fragment_permission_request;
    }
}
