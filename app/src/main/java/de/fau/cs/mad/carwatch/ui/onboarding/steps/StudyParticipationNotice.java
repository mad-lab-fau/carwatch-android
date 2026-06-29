package de.fau.cs.mad.carwatch.ui.onboarding.steps;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import de.fau.cs.mad.carwatch.R;

public class StudyParticipationNotice extends BaseWelcomeSlide {

    public StudyParticipationNotice() {
        super();
        canShowPreviousSlide.set(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialCheckBox confirmationCheckbox = view.findViewById(R.id.cb_study_participation_confirmation);
        confirmationCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> canShowNextSlide.set(isChecked));

        MaterialButton privacyPolicyButton = view.findViewById(R.id.btn_study_notice_privacy_policy);
        privacyPolicyButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.app_info_privacy_policy_url)));
            startActivity(intent);
        });
    }

    @Override
    protected int getResourceId() {
        return R.layout.fragment_study_participation_notice;
    }
}
