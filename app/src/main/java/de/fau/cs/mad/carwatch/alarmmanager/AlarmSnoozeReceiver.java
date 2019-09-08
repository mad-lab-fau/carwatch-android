package de.fau.cs.mad.carwatch.alarmmanager;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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

        // Get snooze length from shared preferences
        DateTime snoozeTime = DateTime.now();
        snoozeTime = snoozeTime.minusSeconds(snoozeTime.getSecondOfMinute());
        snoozeTime = snoozeTime.plusMinutes(5);

        // Schedule next ring
        Log.d(TAG, "Snoozing alarm " + alarmId + " for " + snoozeTime.getMinuteOfHour() + " minutes");
        AlarmHandler alarmHandler = new AlarmHandler(context, null);
        alarmHandler.scheduleAlarmAtTime(snoozeTime, alarmId);
    }
}
