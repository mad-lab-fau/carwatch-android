package de.fau.cs.mad.carwatch.alarmmanager;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;

/**
 * Broadcast Receiver to snooze alarm
 */
public class AlarmSnoozeReceiver extends BroadcastReceiver {

    private static final String TAG = AlarmSnoozeReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_DEFAULT);
        AlarmSource alarmSource = (AlarmSource) intent.getSerializableExtra(Constants.EXTRA_SOURCE);
        if (alarmSource == null) {
            // this should never happen!
            alarmSource = AlarmSource.SOURCE_UNKNOWN;
        }

        Log.d(TAG, "Stopping alarm " + alarmId + " to activate snooze...");
        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.stopAlarmSound();

        // Dismiss notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int snoozeDuration = Integer.parseInt(sp.getString(Constants.PREF_SNOOZE_DURATION, "5"));

        DateTime snoozeTime = DateTime.now();
        // set seconds to 0
        snoozeTime = snoozeTime.minusSeconds(snoozeTime.getSecondOfMinute());
        // add snooze time
        snoozeTime = snoozeTime.plusMinutes(snoozeDuration);

        try {
            // create Json object and log information
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarmId);
            json.put(Constants.LOGGER_EXTRA_ALARM_SNOOZE_DURATION, snoozeDuration);
            json.put(Constants.LOGGER_EXTRA_ALARM_SOURCE, alarmSource.ordinal());
            LoggerUtil.log(Constants.LOGGER_ACTION_ALARM_SNOOZE, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Schedule next ring
        Log.d(TAG, "Alarm source: " + alarmSource);
        Log.d(TAG, "Snoozing alarm " + alarmId + " for " + snoozeDuration + " minutes");

        AlarmHandler.scheduleAlarmAtTime(context, snoozeTime, alarmId);
    }
}
