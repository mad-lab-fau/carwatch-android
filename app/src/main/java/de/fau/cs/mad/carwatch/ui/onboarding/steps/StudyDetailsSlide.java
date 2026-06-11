package de.fau.cs.mad.carwatch.ui.onboarding.steps;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;

public class StudyDetailsSlide extends BaseWelcomeSlide {

    public StudyDetailsSlide() {
        super();
        canShowNextSlide.set(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        if (root == null) {
            return null;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String studyName = sharedPreferences.getString(Constants.PREF_STUDY_NAME, "");
        String participantId = sharedPreferences.getString(Constants.PREF_PARTICIPANT_ID, "");
        int studyDays = sharedPreferences.getInt(Constants.PREF_NUM_DAYS, 0);
        int samplesPerDay = sharedPreferences.getInt(Constants.PREF_TOTAL_NUM_SAMPLES, 0);
        boolean hasEveningSample = sharedPreferences.getBoolean(Constants.PREF_HAS_EVENING, false);

        setDetailRow(root, R.id.row_study_name, R.string.label_study_name, studyName);
        setDetailRow(root, R.id.row_participant_id, R.string.label_participant_id, participantId);
        setDetailRow(root, R.id.row_study_days, R.string.label_study_days, String.valueOf(studyDays));
        setDetailRow(root, R.id.row_samples_per_day, R.string.label_samples_per_day, String.valueOf(samplesPerDay));
        setDetailRow(root, R.id.row_evening_sample, R.string.label_evening_sample, getString(hasEveningSample ? R.string.yes : R.string.no));

        return root;
    }

    private void setDetailRow(View root, int rowId, int labelId, String value) {
        View row = root.findViewById(rowId);
        TextView labelView = row.findViewById(R.id.tv_detail_label);
        TextView valueView = row.findViewById(R.id.tv_detail_value);
        labelView.setText(labelId);
        valueView.setText(value);
    }

    @Override
    protected int getResourceId() {
        return R.layout.fragment_study_details;
    }
}
