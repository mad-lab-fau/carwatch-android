package de.fau.cs.mad.carwatch.alarmmanager;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.content.IntentCompat;
import androidx.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.BarcodeActivity;
import de.fau.cs.mad.carwatch.ui.MainActivity;
import de.fau.cs.mad.carwatch.util.AlarmRepository;

public class AlarmStopReceiver extends BroadcastReceiver {

    private static final String TAG = AlarmStopReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmSoundControl.getInstance().stopAlarmSound();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int alarmId = intent.getIntExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_INITIAL);

        AlarmRepository repository = AlarmRepository.getInstance((Application) context.getApplicationContext());
        Alarm alarm;

        try {
            alarm = repository.getAlarmById(alarmId);
            if (alarm == null) {
                Log.e(TAG, "No alarm found with id " + alarmId);
                return;
            }
            alarm.setActive(false);
            repository.update(alarm);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error while getting alarm with id " + alarmId + " from database", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        AlarmSource alarmSource = IntentCompat.getSerializableExtra(
                intent,
                Constants.EXTRA_SOURCE,
                AlarmSource.class);
        if (alarmSource == null) {
            alarmSource = AlarmSource.SOURCE_UNKNOWN;
        }

        if (alarmId == Constants.EXTRA_ALARM_ID_INITIAL) {
            if (alarmSource == AlarmSource.SOURCE_ACTIVITY) {
                setResultCode(Activity.RESULT_CANCELED);
            } else {
                Intent wakeupIntent = new Intent(context, MainActivity.class);
                wakeupIntent.putExtra(Constants.EXTRA_TARGET_NAV_ELEMENT, R.id.navigation_wakeup);
                wakeupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(wakeupIntent);
            }
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarmId);
            json.put(Constants.LOGGER_EXTRA_ALARM_SOURCE, alarmSource.ordinal());
            json.put(Constants.LOGGER_EXTRA_SALIVA_ID, alarm.getSalivaId());
            LoggerUtil.log(Constants.LOGGER_ACTION_ALARM_STOP, json);
        } catch (JSONException e) {
            Log.e(TAG, "Could not log alarm stop", e);
        }

        Log.d(TAG, "Stopping Alarm: " + alarmId);

        if (alarm.getSalivaId() == -1) {
            Log.d(TAG, "No saliva procedure requested for alarm with id " + alarmId);
            if (alarmSource == AlarmSource.SOURCE_ACTIVITY) {
                setResultCode(Activity.RESULT_CANCELED);
            }
            return;
        }

        int currentAlarmId = sharedPreferences.getInt(Constants.PREF_ID_ONGOING_ALARM, Constants.EXTRA_ALARM_ID_INITIAL);
        if (currentAlarmId != Constants.EXTRA_ALARM_ID_INITIAL && currentAlarmId % Constants.ALARM_OFFSET != alarmId % Constants.ALARM_OFFSET) {
            Log.d(TAG, "Saliva procedure with alarm id " + currentAlarmId + " already running at the moment!");
            setResultCode(Activity.RESULT_CANCELED);
            return;
        }

        TimerHandler.scheduleSalivaCountdown(context, alarmId, alarm.getSalivaId());

        if (alarmSource != AlarmSource.SOURCE_NOTIFICATION) {
            return;
        }

        Intent scannerIntent = new Intent(context, BarcodeActivity.class);
        scannerIntent.putExtra(Constants.EXTRA_ALARM_ID, alarmId);
        scannerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(scannerIntent);
    }
}
