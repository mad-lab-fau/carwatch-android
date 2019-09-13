package de.fau.cs.mad.carwatch.alarmmanager;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.orhanobut.logger.Logger;

import org.joda.time.DateTime;

import de.fau.cs.mad.carwatch.Constants;

/**
 * Broadcast Receiver to snooze alarm
 */
public class AlarmSnoozeReceiver extends BroadcastReceiver {

    private static final String TAG = AlarmSnoozeReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra(Constants.EXTRA_ID, 0);

        Log.d(TAG, "Stopping alarm " + alarmId + " to activate snooze...");
        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.stopAlarmSound();

        // Dismiss notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        // TODO Get snooze duration from shared preferences
        int snoozeDuration = 5;

        DateTime snoozeTime = DateTime.now();
        // set seconds to 0
        snoozeTime = snoozeTime.minusSeconds(snoozeTime.getSecondOfMinute());
        // add snooze time
        snoozeTime = snoozeTime.plusMinutes(snoozeDuration);

        // Schedule next ring
        Logger.log(Logger.INFO, Constants.LOGGER_ACTION_SNOOZE, String.valueOf(alarmId), null);
        Logger.log(Logger.INFO, Constants.LOGGER_EXTRA_SNOOZE_DURATION, String.valueOf(snoozeDuration), null);
        Log.d(TAG, "Snoozing alarm " + alarmId + " for " + snoozeDuration + " minutes");

        AlarmHandler alarmHandler = new AlarmHandler(context, null);
        alarmHandler.scheduleAlarmAtTime(snoozeTime, alarmId);
    }
}
