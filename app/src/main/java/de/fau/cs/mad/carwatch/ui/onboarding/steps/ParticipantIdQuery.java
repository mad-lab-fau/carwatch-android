package de.fau.cs.mad.carwatch.ui.onboarding.steps;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashSet;

import androidx.preference.PreferenceManager;
import de.fau.cs.mad.carwatch.BuildConfig;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.util.Utils;

public class ParticipantIdQuery extends BaseWelcomeSlide {

    private EditText participantIdEditText;

    public ParticipantIdQuery() {
        super();
        isNextButtonEnabled.set(false);
        isSkipButtonVisible.set(false);
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
                    isNextButtonEnabled.set(charSequence.length() > 0);
                    isNextButtonEnabled.notifyChange();
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
        sharedPreferences.edit().putString(Constants.PREF_SUBJECT_ID, participantId).apply();
        logInitialData(sharedPreferences);
    }

    @Override
    protected int getResourceId() {
        return R.layout.fragment_participant_id_query;
    }

    private void logInitialData(SharedPreferences sharedPreferences) {
        logParticipantId(sharedPreferences);
        logAppPhoneMetadata();
        logStudyData(sharedPreferences);
    }

    private void logParticipantId(SharedPreferences sharedPreferences) {
        String participantId = sharedPreferences.getString(Constants.PREF_SUBJECT_ID, "");

        try {
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_SUBJECT_ID, participantId);
            LoggerUtil.log(Constants.LOGGER_ACTION_SUBJECT_ID_SET, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void logAppPhoneMetadata() {
        try {
            // App Metadata – Version Code and Version Name
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_APP_VERSION_CODE, BuildConfig.VERSION_CODE);
            json.put(Constants.LOGGER_EXTRA_APP_VERSION_NAME, BuildConfig.VERSION_NAME);
            LoggerUtil.log(Constants.LOGGER_ACTION_APP_METADATA, json);

            // Phone Metadata – Brand, Manufacturer, Model, Android SDK level, Security Patch (if applicable), Build Release
            json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_PHONE_BRAND, Build.BRAND);
            json.put(Constants.LOGGER_EXTRA_PHONE_MANUFACTURER, Build.MANUFACTURER);
            json.put(Constants.LOGGER_EXTRA_PHONE_MODEL, Build.MODEL);
            json.put(Constants.LOGGER_EXTRA_PHONE_VERSION_SDK_LEVEL, Build.VERSION.SDK_INT);
            json.put(Constants.LOGGER_EXTRA_PHONE_VERSION_SECURITY_PATCH, Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Build.VERSION.SECURITY_PATCH : ""); // this
            json.put(Constants.LOGGER_EXTRA_PHONE_VERSION_RELEASE, Build.VERSION.RELEASE); // this

            LoggerUtil.log(Constants.LOGGER_ACTION_PHONE_METADATA, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void logStudyData(SharedPreferences sharedPreferences) {
        try {
            // construct human-readable sample ids
            boolean hasEveningSalivette = sharedPreferences.getBoolean(Constants.PREF_HAS_EVENING, false);
            String salivaDistancesString = sharedPreferences.getString(Constants.PREF_SALIVA_DISTANCES, "");
            String salivaTimesString = sharedPreferences.getString(Constants.PREF_SALIVA_TIMES, "");
            int[] salivaDistances = Utils.decodeArrayFromString(salivaDistancesString);
            String[] salivaTimes = salivaTimesString.split(Constants.QR_PARSER_LIST_SEPARATOR);
            String startSample = sharedPreferences.getString(Constants.PREF_START_SAMPLE, "");
            String samplePrefix = startSample.substring(0, 1);
            int startSampleIdx = Integer.parseInt(startSample.substring(1));
            LinkedHashSet<String> salivaIds = new LinkedHashSet<>();
            for (int i = startSampleIdx; i < salivaDistances.length + startSampleIdx + salivaTimes.length; i++) {
                String sampleId = samplePrefix + i;
                salivaIds.add(sampleId);
            }
            if (hasEveningSalivette) {
                salivaIds.add(samplePrefix + Constants.EXTRA_SALIVA_ID_EVENING);
            }
            // log all relevant study data
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_STUDY_NAME, sharedPreferences.getString(Constants.PREF_STUDY_NAME, ""));
            json.put(Constants.LOGGER_EXTRA_NUM_SUBJECTS, sharedPreferences.getInt(Constants.PREF_NUM_SUBJECTS, 0));
            json.put(Constants.LOGGER_EXTRA_SALIVA_DISTANCES, salivaDistancesString);
            json.put(Constants.LOGGER_EXTRA_SALIVA_TIMES, salivaTimesString);
            json.put(Constants.LOGGER_EXTRA_STUDY_DAYS, sharedPreferences.getInt(Constants.PREF_NUM_DAYS, 0));
            json.put(Constants.LOGGER_EXTRA_SALIVA_IDS, salivaIds);
            json.put(Constants.LOGGER_EXTRA_HAS_EVENING_SALIVETTE, hasEveningSalivette);
            json.put(Constants.LOGGER_EXTRA_SHARE_EMAIL_ADDRESS, sharedPreferences.getString(Constants.PREF_SHARE_EMAIL_ADDRESS, ""));
            json.put(Constants.LOGGER_EXTRA_CHECK_DUPLICATES, sharedPreferences.getBoolean(Constants.PREF_CHECK_DUPLICATES, false));
            json.put(Constants.LOGGER_EXTRA_MANUAL_SCAN, sharedPreferences.getBoolean(Constants.PREF_MANUAL_SCAN, false));
            LoggerUtil.log(Constants.LOGGER_ACTION_STUDY_DATA, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}