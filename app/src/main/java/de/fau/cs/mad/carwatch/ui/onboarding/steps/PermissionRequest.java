package de.fau.cs.mad.carwatch.ui.onboarding.steps;

import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.util.Utils;

/**
 * create an instance of this fragment.
 */
public class PermissionRequest extends BaseWelcomeSlide {

    public PermissionRequest() {
        super();
        isSkipButtonVisible.set(false);
    }

    @Override
    public void onSlideFinished() {
        Utils.requestRuntimePermissions(getActivity());
    }

    @Override
    protected int getResourceId() {
        return R.layout.fragment_permission_request;
    }
}