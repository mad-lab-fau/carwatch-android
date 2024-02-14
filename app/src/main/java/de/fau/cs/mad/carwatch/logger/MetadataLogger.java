package de.fau.cs.mad.carwatch.logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashSet;

import androidx.preference.PreferenceManager;
import de.fau.cs.mad.carwatch.BuildConfig;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.util.Utils;

public class MetadataLogger {
    public static void logDeviceProperties() {
        JSONObject json = new JSONObject();
        try {
            json.put(Constants.LOGGER_EXTRA_PHONE_BRAND, Build.BRAND);
            json.put(Constants.LOGGER_EXTRA_PHONE_MANUFACTURER, Build.MANUFACTURER);
            json.put(Constants.LOGGER_EXTRA_PHONE_MODEL, Build.MODEL);
            json.put(Constants.LOGGER_EXTRA_PHONE_VERSION_SDK_LEVEL, Build.VERSION.SDK_INT);
            json.put(Constants.LOGGER_EXTRA_PHONE_VERSION_SECURITY_PATCH, Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? Build.VERSION.SECURITY_PATCH : "");
            json.put(Constants.LOGGER_EXTRA_PHONE_VERSION_RELEASE, Build.VERSION.RELEASE);
            LoggerUtil.log(Constants.LOGGER_ACTION_PHONE_METADATA, json);
        } catch (JSONException e) {
            LoggerUtil.log(Constants.LOGGER_ACTION_PHONE_METADATA, "Failed to log device properties: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void logAppMetadata() {
        JSONObject json = new JSONObject();
        try {
            json.put(Constants.LOGGER_EXTRA_APP_VERSION_CODE, BuildConfig.VERSION_CODE);
            json.put(Constants.LOGGER_EXTRA_APP_VERSION_NAME, BuildConfig.VERSION_NAME);
            LoggerUtil.log(Constants.LOGGER_ACTION_APP_METADATA, json);
        } catch (Exception e) {
            LoggerUtil.log(Constants.LOGGER_ACTION_APP_METADATA, "Failed to log app metadata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void logStudyData(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean hasEveningSample = sp.getBoolean(Constants.PREF_HAS_EVENING, false);
        String salivaDistancesString = sp.getString(Constants.PREF_SALIVA_DISTANCES, "");
        String salivaTimesString = sp.getString(Constants.PREF_SALIVA_TIMES, "");
        int[] salivaDistances = Utils.decodeArrayFromString(salivaDistancesString);
        String[] salivaTimes = salivaTimesString.split(Constants.QR_PARSER_LIST_SEPARATOR);
        String startSample = sp.getString(Constants.PREF_START_SAMPLE, "");
        String samplePrefix = startSample.substring(0, 1);
        int startSampleIdx = Integer.parseInt(startSample.substring(1));
        LinkedHashSet<String> salivaIds = new LinkedHashSet<>();
        for (int i = startSampleIdx; i < salivaDistances.length + startSampleIdx + salivaTimes.length; i++) {
            String sampleId = samplePrefix + i;
            salivaIds.add(sampleId);
        }
        if (hasEveningSample) {
            salivaIds.add(samplePrefix + Constants.EXTRA_SALIVA_ID_EVENING);
        }
        // log all relevant study data
        JSONObject json = new JSONObject();
        try {
            json.put(Constants.LOGGER_EXTRA_STUDY_NAME, sp.getString(Constants.PREF_STUDY_NAME, ""));
            json.put(Constants.LOGGER_EXTRA_NUM_PARTICIPANTS, sp.getInt(Constants.PREF_NUM_PARTICIPANTS, 0));
            json.put(Constants.LOGGER_EXTRA_SALIVA_DISTANCES, salivaDistancesString);
            json.put(Constants.LOGGER_EXTRA_SALIVA_TIMES, salivaTimesString);
            json.put(Constants.LOGGER_EXTRA_STUDY_DAYS, sp.getInt(Constants.PREF_NUM_DAYS, 0));
            json.put(Constants.LOGGER_EXTRA_SALIVA_IDS, salivaIds);
            json.put(Constants.LOGGER_EXTRA_HAS_EVENING_SALIVETTE, hasEveningSample);
            json.put(Constants.LOGGER_EXTRA_SHARE_EMAIL_ADDRESS, sp.getString(Constants.PREF_SHARE_EMAIL_ADDRESS, ""));
            json.put(Constants.LOGGER_EXTRA_CHECK_DUPLICATES, sp.getBoolean(Constants.PREF_CHECK_DUPLICATES, false));
            json.put(Constants.LOGGER_EXTRA_MANUAL_SCAN, sp.getBoolean(Constants.PREF_MANUAL_SCAN, false));
            LoggerUtil.log(Constants.LOGGER_ACTION_STUDY_DATA, json);
        } catch (JSONException e) {
            LoggerUtil.log(Constants.LOGGER_ACTION_STUDY_DATA, "Failed to log study data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void logParticipantId(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String participantId = sp.getString(Constants.PREF_PARTICIPANT_ID, "");
        JSONObject json = new JSONObject();
        try {
            json.put(Constants.LOGGER_EXTRA_PARTICIPANT_ID, participantId);
            LoggerUtil.log(Constants.LOGGER_ACTION_PARTICIPANT_ID_SET, json);
        } catch (JSONException e) {
            LoggerUtil.log(Constants.LOGGER_ACTION_PARTICIPANT_ID_SET, "Failed to log participant id: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
