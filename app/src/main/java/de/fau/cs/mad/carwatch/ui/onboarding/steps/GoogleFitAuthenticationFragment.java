package de.fau.cs.mad.carwatch.ui.onboarding.steps;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.sleep.GoogleFitConnector;
import de.fau.cs.mad.carwatch.ui.onboarding.SlideShowActivity;
import de.fau.cs.mad.carwatch.util.Utils;

/**
 * A simple {@link Fragment} subclass that can be used in a {@link SlideShowActivity} to request
 * Google Fit permissions.
 * Use the {@link GoogleFitAuthenticationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GoogleFitAuthenticationFragment extends BaseWelcomeSlide {

    public static final String TAG = GoogleFitAuthenticationFragment.class.getSimpleName();

    public GoogleFitAuthenticationFragment() { }

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment GoogleFitAuthenticationFragment.
     */
    public static GoogleFitAuthenticationFragment newInstance() {
        return new GoogleFitAuthenticationFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        if (root == null) {
            return null;
        }

        Button enableGoogleFitButton = root.findViewById(R.id.btn_yes_google_fit_authentication);
        enableGoogleFitButton.setOnClickListener(v -> requestGoogleFitPermissions());

        Button skipGoogleFitButton = root.findViewById(R.id.btn_no_google_fit_authentication);
        skipGoogleFitButton.setOnClickListener(v -> canShowNextSlide.set(true));

        return root;
    }

    private void requestGoogleFitPermissions() {
        if (!Utils.isInternetAvailable(requireContext())) {
            showErrorDialog(getString(R.string.error_no_internet_connection));
            return;
        }

        GoogleFitConnector gfc = new GoogleFitConnector(requireContext());

        if (!gfc.canAccessSleepData()) {
            gfc.requestPermissions(requireActivity());
        } else {
            Log.d(TAG, "User has already authorized the Google Fit API access.");
            canShowNextSlide.set(true);
        }
    }

    @Override
    protected int getResourceId() {
        return R.layout.fragment_google_fit_authentication;
    }
}