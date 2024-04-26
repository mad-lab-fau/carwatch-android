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
        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.stopAlarmSound();

        // Dismiss notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        DateTime lastWakeUpAlarmRingTime = new DateTime(sharedPreferences.getLong(Constants.PREF_LAST_WAKE_UP_ALARM_RING_TIME, 0));
        DateTime dayCurrentSalivaAlarmsWereScheduled = lastWakeUpAlarmRingTime.withTime(LocalTime.MIDNIGHT);
        int alarmId = intent.getIntExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_INITIAL);
        boolean firstAlarmProcessAlreadyFinished = false;
        int dayCounter = sharedPreferences.getInt(Constants.PREF_DAY_COUNTER, 0) + 1;
        int numDays = sharedPreferences.getInt(Constants.PREF_NUM_DAYS, Integer.MAX_VALUE);
        boolean studyIsFinished = dayCounter > numDays;
        boolean resetWasSampleTaken = false;

        if (dayCurrentSalivaAlarmsWereScheduled.isBefore(LocalTime.MIDNIGHT.toDateTimeToday()) && alarmId == Constants.EXTRA_ALARM_ID_INITIAL && !studyIsFinished) {
            resetWasSampleTaken = true;
            AlarmHandler.rescheduleSalivaAlarms(context);
            sharedPreferences.edit()
                    .putLong(Constants.PREF_LAST_WAKE_UP_ALARM_RING_TIME, DateTime.now().getMillis())
                    .putInt(Constants.PREF_DAY_COUNTER, dayCounter)
                    .putInt(Constants.PREF_ID_ONGOING_ALARM, Constants.EXTRA_ALARM_ID_INITIAL)
                    .apply();

        } else {
            firstAlarmProcessAlreadyFinished = true;
        }

        AlarmRepository repository = AlarmRepository.getInstance((Application) context.getApplicationContext());
        Alarm alarm;

        try {
            alarm = repository.getAlarmById(alarmId);
            alarm.setActive(false);
            if (resetWasSampleTaken)
                alarm.setWasSampleTaken(false);
            repository.update(alarm);
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

        if (alarm.getSalivaId() == -1) {
            // no saliva procedure requested
            Log.d(TAG, "No saliva procedure requested for alarm with id " + alarmId);
            if (alarmSource == AlarmSource.SOURCE_ACTIVITY)
                setResultCode(Activity.RESULT_CANCELED);
            return;
        }

        if (alarmId == Constants.EXTRA_ALARM_ID_INITIAL && firstAlarmProcessAlreadyFinished) {
            Log.d(TAG, "First alarm process already finished for alarm with id " + alarmId);
            if (alarmSource == AlarmSource.SOURCE_ACTIVITY)
                setResultCode(Activity.RESULT_CANCELED);
            return;
        }

        int currentAlarmId = sharedPreferences.getInt(Constants.PREF_ID_ONGOING_ALARM, Constants.EXTRA_ALARM_ID_INITIAL);
        if (currentAlarmId != Constants.EXTRA_ALARM_ID_INITIAL && currentAlarmId % Constants.ALARM_OFFSET != alarmId % Constants.ALARM_OFFSET) {
            // There's already a saliva procedure running at the moment
            Log.d(TAG, "Saliva procedure with alarm id " + currentAlarmId + " already running at the moment!");
            setResultCode(Activity.RESULT_CANCELED);
            return;
        }

        TimerHandler.scheduleSalivaCountdown(context, alarmId, alarm.getSalivaId());

        if (alarmSource != AlarmSource.SOURCE_NOTIFICATION) {
            // barcode activity is automatically started if alarm is stopped by AlarmStopActivity
            return;
        }

        Intent scannerIntent = new Intent(context, BarcodeActivity.class);
        scannerIntent.putExtra(Constants.EXTRA_ALARM_ID, alarmId);
        scannerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(scannerIntent);
    }
}
