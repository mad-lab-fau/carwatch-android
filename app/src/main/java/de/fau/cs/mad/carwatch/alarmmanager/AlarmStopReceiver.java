package de.fau.cs.mad.carwatch.alarmmanager;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.BarcodeActivity;
import de.fau.cs.mad.carwatch.util.AlarmRepository;

/**
 * BroadcastReceiver to stop alarm ringing
 */
public class AlarmStopReceiver extends BroadcastReceiver {

    private final String TAG = AlarmStopReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_INITIAL);

        AlarmRepository repository = AlarmRepository.getInstance((Application) context.getApplicationContext());
        Alarm alarm;

        try {
            alarm = repository.getAlarmById(alarmId);
            alarm.setActive(false);
            repository.updateActive(alarm);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error while getting alarm with id " + alarmId + " from database");
            e.printStackTrace();
            return;
        }

        AlarmSource alarmSource = (AlarmSource) intent.getSerializableExtra(Constants.EXTRA_SOURCE);
        if (alarmSource == null) {
            // this should never happen!
            alarmSource = AlarmSource.SOURCE_UNKNOWN;
        }


        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.stopAlarmSound();

        try {
            // create Json object and log information
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarmId);
            json.put(Constants.LOGGER_EXTRA_ALARM_SOURCE, alarmSource.ordinal());
            json.put(Constants.LOGGER_EXTRA_SALIVA_ID, alarm.getSalivaId());
            LoggerUtil.log(Constants.LOGGER_ACTION_ALARM_STOP, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Stopping Alarm: " + alarmId);

        // Dismiss notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        DateTime timeTaken = new DateTime(sp.getLong(Constants.PREF_MORNING_TAKEN, 0));

        int currentAlarmId = sp.getInt(Constants.PREF_MORNING_ONGOING, Constants.EXTRA_ALARM_ID_INITIAL);
        if (currentAlarmId != Constants.EXTRA_ALARM_ID_INITIAL && currentAlarmId % Constants.ALARM_OFFSET != alarmId % Constants.ALARM_OFFSET) {
            // There's already a saliva procedure running at the moment
            Log.d(TAG, "Saliva procedure with alarm id " + currentAlarmId + " already running at the moment!");
            setResultCode(Activity.RESULT_CANCELED);
            return;
        }

        final DateTime midnight = LocalTime.MIDNIGHT.toDateTimeToday();

        if (timeTaken.equals(midnight) && alarmSource == AlarmSource.SOURCE_ACTIVITY) {
            // morning already finished => return (with result code)
            setResultCode(Activity.RESULT_CANCELED);
            return;
        }

        if (!timeTaken.equals(midnight)) {
            int eveningSalivaId = sp.getInt(Constants.PREF_EVENING_SALIVA_ID, 1);
            TimerHandler.scheduleSalivaCountdown(context, alarmId, alarm.getSalivaId(), eveningSalivaId);
        }

        if (sp.getBoolean(Constants.PREF_FIRST_RUN_ALARM, false)) {
            AlarmHandler.scheduleSalivaAlarms(context);
            sp.edit().putBoolean(Constants.PREF_FIRST_RUN_ALARM, false).apply();
        }

        if (alarmSource != AlarmSource.SOURCE_NOTIFICATION) {
            // barcode activity is automatically started if alarm is stopped by AlarmStopActivity
            return;
        }

        Intent scannerIntent = new Intent(context, BarcodeActivity.class);
        scannerIntent.putExtra(Constants.EXTRA_ALARM_ID, alarmId);
        scannerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (timeTaken.equals(midnight)) {
            scannerIntent.putExtra(Constants.EXTRA_DAY_FINISHED, Activity.RESULT_CANCELED);
        }

        context.startActivity(scannerIntent);
    }
}
