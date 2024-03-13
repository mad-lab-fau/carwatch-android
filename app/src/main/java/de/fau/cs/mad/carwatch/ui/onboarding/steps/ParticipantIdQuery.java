package de.fau.cs.mad.carwatch.ui.onboarding.steps;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.preference.PreferenceManager;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.logger.MetadataLogger;

public class ParticipantIdQuery extends BaseWelcomeSlide {

    private EditText participantIdEditText;

    public ParticipantIdQuery() {
        super();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        participantIdEditText = rootView != null ? rootView.findViewById(R.id.et_participant_id) : null;
        if (participantIdEditText != null) {
            participantIdEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    canShowNextSlide.set(charSequence.length() > 0);
                    canShowNextSlide.notifyChange();
                }

                @Override
                public void afterTextChanged(Editable editable) { }
            });
        }
        return rootView;
    }

    @Override
    public void onSlideFinished() {
        String participantId = participantIdEditText.getText().toString();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        sharedPreferences.edit()
                .putString(Constants.PREF_PARTICIPANT_ID, participantId)
                .putBoolean(Constants.PREF_PARTICIPANT_ID_WAS_SET, false)
                .apply();
        MetadataLogger.logDeviceProperties();
        MetadataLogger.logAppMetadata();
        MetadataLogger.logStudyData(requireContext());
        MetadataLogger.logParticipantId(requireContext());
    }

    @Override
    protected int getResourceId() {
        return R.layout.fragment_participant_id_query;
    }
}