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

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;

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

        SessionReadRequest request2 = new SessionReadRequest.Builder()
                .readSessionsFromAllApps()
                .includeSleepSessions()
                .read(DataType.TYPE_SLEEP_SEGMENT)
                .setTimeInterval(startTime.getMillis(), endTime.getMillis(), SESSION_TIME_UNIT)
                .build();


        Fitness.getSessionsClient(context, getGoogleSignInAccount())
                .readSession(request)
                .addOnFailureListener(e -> Log.w(TAG, "There was a problem reading the sleep sessions.", e))
                .addOnSuccessListener(response -> {
                    List<Session> sessions = response.getSessions();

                    if (sessions.isEmpty())
                        LoggerUtil.log(TAG, Constants.LOGGER_RECORDED_SLEEP_DATA + ";No sleep sessions found.");
                    else if (sessions.size() > 1)
                        LoggerUtil.log(TAG, Constants.LOGGER_RECORDED_SLEEP_DATA + ";Multiple sleep sessions found.");

                    for (Session session : sessions) {
                        int datasetsLogged = 0;

                        for (DataSet dataSet : response.getDataSet(session)) {
                            List<DataPoint> dataPoints = dataSet.getDataPoints();
                            if (dataPoints.isEmpty())
                                continue;

                            if (dataPoints.size() > 3)
                                dataPoints = dataPoints.subList(dataPoints.size() - 3, dataPoints.size());

                            JSONArray stageLogs = new JSONArray();
                            for (DataPoint dataPoint : dataPoints) {
                                DateTime start = new DateTime(dataPoint.getStartTime(SESSION_TIME_UNIT));
                                DateTime end = new DateTime(dataPoint.getEndTime(SESSION_TIME_UNIT));
                                String stage = SLEEP_STAGE_NAMES[dataPoint.getValue(Field.FIELD_SLEEP_SEGMENT_TYPE).asInt()];

                                JSONObject stageLog = new JSONObject();
                                try {
                                    stageLog.put(Constants.LOGGER_STAGE_DATE, start.toString(LOGGER_DATE_PATTERN));
                                    stageLog.put(Constants.LOGGER_STAGE, stage);
                                    stageLog.put(Constants.LOGGER_STAGE_START, start.toString(STAGE_TIME_PATTERN));
                                    stageLog.put(Constants.LOGGER_STAGE_END, end.toString(STAGE_TIME_PATTERN));
                                    stageLogs.put(stageLog);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error while creating JSON object", e);
                                }
                            }
                            JSONObject sessionLog = new JSONObject();
                            try {
                                sessionLog.put(Constants.LOGGER_LAST_SLEEP_PHASES, stageLogs);
                                LoggerUtil.log(Constants.LOGGER_RECORDED_SLEEP_DATA, sessionLog);
                                datasetsLogged++;
                            } catch (JSONException e) {
                                Log.e(TAG, "Error while creating JSON object", e);
                            }
                        }

                        if (datasetsLogged > 0) {
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                            sharedPreferences.edit().putLong(Constants.PREF_LAST_SLEEP_LOGGED_TIME, endTime.getMillis()).apply();
                        }
                    }
                });
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
}
