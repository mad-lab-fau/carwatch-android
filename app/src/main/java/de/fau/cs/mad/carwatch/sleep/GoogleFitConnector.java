package de.fau.cs.mad.carwatch.sleep;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.SessionReadResponse;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.logger.SleepDataLogger;

public class GoogleFitConnector {

    public static final String TAG = GoogleFitConnector.class.getSimpleName();
    private static final TimeUnit SESSION_TIME_UNIT = TimeUnit.MILLISECONDS;
    private static final String LOGGER_DATE_PATTERN = "dd.MM.yyyy";
    private static final String STAGE_TIME_PATTERN = "HH:mm";
    private static final String[] SLEEP_STAGE_NAMES = new String[]{
            "Unused",
            "Awake (during sleep)",
            "Sleep",
            "Out-of-bed",
            "Light sleep",
            "Deep sleep",
            "REM sleep"
    };

    private static final List<String> SLEEP_TYPES = Arrays.asList("Sleep", "Light sleep", "Deep sleep", "REM sleep");

    private final @NonNull Context context;

    public GoogleFitConnector(@NonNull Context context) {
        this.context = context;
    }

    public void requestPermissions(@NonNull Activity activity) {
        FitnessOptions fitnessOptions = buildSleepFitnessOptions();
        GoogleSignIn.requestPermissions(
                activity,
                Constants.GOOGLE_FIT_REQUEST_CODE,
                GoogleSignIn.getAccountForExtension(activity, fitnessOptions),
                fitnessOptions);
    }

    public boolean canAccessSleepData() {
        FitnessOptions fitnessOptions = buildSleepFitnessOptions();
        GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(context, fitnessOptions);
        return GoogleSignIn.hasPermissions(account, fitnessOptions);
    }

    public void logLastSleepData() {
        DateTime endTime = DateTime.now();
        DateTime startTime = endTime.minusDays(1);

        SessionReadRequest request = new SessionReadRequest.Builder()
                .readSessionsFromAllApps()
                .includeSleepSessions()
                .read(DataType.TYPE_SLEEP_SEGMENT)
                .setTimeInterval(startTime.getMillis(), endTime.getMillis(), SESSION_TIME_UNIT)
                .build();

        Fitness.getSessionsClient(context, getGoogleSignInAccount())
                .readSession(request)
                .addOnSuccessListener(this::logSleepData)
                .addOnFailureListener(this::handleSessionReadError);
    }

    public boolean wasSleepLoggedToday() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        DateTime lastSleepLoggedTime = new DateTime(sharedPreferences.getLong(Constants.PREF_LAST_SLEEP_LOGGED_TIME, 0));
        return lastSleepLoggedTime.withTime(LocalTime.MIDNIGHT).equals(LocalTime.MIDNIGHT.toDateTimeToday());
    }

    private GoogleSignInAccount getGoogleSignInAccount() {
        FitnessOptions fitnessOptions = buildSleepFitnessOptions();
        return GoogleSignIn.getAccountForExtension(context, fitnessOptions);
    }

    private static FitnessOptions buildSleepFitnessOptions() {
        return FitnessOptions.builder()
                .accessSleepSessions(FitnessOptions.ACCESS_READ)
                .build();
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
        return resultCode == ConnectionResult.SUCCESS;
    }

    private void logSleepData(SessionReadResponse response) {
        List<Session> sessions = response.getSessions();

        if (sessions.size() > 1)
            LoggerUtil.log(TAG, Constants.LOGGER_RECORDED_SLEEP_DATA + ";Multiple sleep sessions found.");

        List<Long> sessionWakeUpTimes = new ArrayList<>();
        List<SleepPhase> sleepPhases = new ArrayList<>();

        for (Session session : sessions) {
            long sessionWakeUpTime = 0;

            for (DataSet dataSet : response.getDataSet(session)) {
                for (DataPoint dataPoint : dataSet.getDataPoints()) {
                    DateTime start = new DateTime(dataPoint.getStartTime(SESSION_TIME_UNIT));
                    DateTime end = new DateTime(dataPoint.getEndTime(SESSION_TIME_UNIT));
                    int sleepSegmentType = dataPoint.getValue(Field.FIELD_SLEEP_SEGMENT_TYPE).asInt();
                    String stage = SLEEP_STAGE_NAMES[sleepSegmentType];

                    SleepPhase sleepPhase = new SleepPhase(stage, start, end);
                    sleepPhases.add(sleepPhase);

                    if (SLEEP_TYPES.contains(stage)) {
                        sessionWakeUpTime = Math.max(end.getMillis(), sessionWakeUpTime);
                    }
                }
            }

            if (sessionWakeUpTime == 0) {
                sessionWakeUpTime = session.getEndTime(TimeUnit.MILLISECONDS);
            }
            sessionWakeUpTimes.add(sessionWakeUpTime);
        }

        if (sessionWakeUpTimes.isEmpty()) {
            Log.i(TAG, Constants.LOGGER_RECORDED_SLEEP_DATA + ": No sleep data found.");
            return;
        }

        SleepDataLogger.log(context, sleepPhases);
        DateTime wakeUpTime = new DateTime(Collections.max(sessionWakeUpTimes));
        JSONObject wakeUpTimeLog = new JSONObject();
        try {
            wakeUpTimeLog.put(Constants.LOGGER_EXTRA_ALARM_TIMESTAMP, wakeUpTime.getMillis());
            wakeUpTimeLog.put(Constants.LOGGER_TRANSLATED_TIMESTAMP,
                    wakeUpTime.toString(LOGGER_DATE_PATTERN + " " + STAGE_TIME_PATTERN));
            LoggerUtil.log(Constants.LOGGER_RECORDED_WAKE_UP_TIME, wakeUpTimeLog);
        } catch (JSONException e) {
            Log.e(TAG, "Error while creating JSON object", e);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putLong(Constants.PREF_LAST_SLEEP_LOGGED_TIME, wakeUpTime.getMillis()).apply();
    }

    private void handleSessionReadError(Exception e) {
        Log.w(TAG, "There was a problem reading the sleep sessions.", e);
        LoggerUtil.log(TAG, "There was a problem reading the sleep sessions." + e.getMessage());
    }
}
