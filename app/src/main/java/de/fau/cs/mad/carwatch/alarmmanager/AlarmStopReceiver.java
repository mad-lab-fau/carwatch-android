package de.fau.cs.mad.carwatch.alarmmanager;

import android.app.Application;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.orhanobut.logger.Logger;

import java.util.concurrent.ExecutionException;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.util.AlarmRepository;

/**
 * BroadcastReceiver to stop alarm ringing
 */
public class AlarmStopReceiver extends BroadcastReceiver {

    private final String TAG = AlarmStopReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra(Constants.EXTRA_ID, 0);

        AlarmRepository repository = new AlarmRepository((Application) context.getApplicationContext());
        try {
            Alarm alarm = repository.getAlarmById(alarmId);
            if (alarm != null) {
                alarm.setActive(false);
                if (alarm.hasHiddenTime()) {
                    alarm.setHiddenDelta(0);
                }
                repository.updateActive(alarm);
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.stopAlarmSound();

        Logger.log(Logger.INFO, Constants.LOGGER_ACTION_STOP, String.valueOf(alarmId), null);
        Log.d(TAG, "Stopping Alarm: " + alarmId);

        // Dismiss notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }
}
