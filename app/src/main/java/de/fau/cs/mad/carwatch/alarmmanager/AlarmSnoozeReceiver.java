package de.fau.cs.mad.carwatch.alarmmanager;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.joda.time.DateTime;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;

/**
 * Broadcast Receiver to snooze alarm
 */
public class AlarmSnoozeReceiver extends BroadcastReceiver {

    private static final String TAG = AlarmSnoozeReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra(Constants.EXTRA_ID, 0);
        int alarmSource = intent.getIntExtra(Constants.EXTRA_SOURCE, -1);

        Log.d(TAG, "Stopping alarm " + alarmId + " to activate snooze...");
        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.stopAlarmSound();

        // Dismiss notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int snoozeDuration = sp.getInt(Constants.PREF_SNOOZE_DURATION, 5);

        DateTime snoozeTime = DateTime.now();
        // set seconds to 0
        snoozeTime = snoozeTime.minusSeconds(snoozeTime.getSecondOfMinute());
        // add snooze time
        snoozeTime = snoozeTime.plusMinutes(snoozeDuration);

        // Schedule next ring
        LoggerUtil.log(Constants.LOGGER_ACTION_SNOOZE, String.valueOf(alarmId));
        LoggerUtil.log(Constants.LOGGER_EXTRA_SNOOZE_DURATION, String.valueOf(snoozeDuration));
        LoggerUtil.log(Constants.LOGGER_EXTRA_SNOOZE_SOURCE, String.valueOf(alarmSource));
        Log.d(TAG, "Alarm source: " + alarmSource);
        Log.d(TAG, "Snoozing alarm " + alarmId + " for " + snoozeDuration + " minutes");

        AlarmHandler alarmHandler = new AlarmHandler(context, null);
        alarmHandler.scheduleAlarmAtTime(snoozeTime, alarmId);
    }
}
