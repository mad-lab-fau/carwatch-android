package de.fau.cs.mad.carwatch.alarmmanager;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.fau.cs.mad.carwatch.Constants;

/**
 * BroadcastReceiver to stop alarm ringing
 */
public class AlarmStopReceiver extends BroadcastReceiver {

    private final String TAG = AlarmStopReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra(Constants.EXTRA_ID, 0);

        Log.d(TAG, "Stopping alarm " + alarmId);

        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.stopAlarmSound();

        // Dismiss notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }
}
