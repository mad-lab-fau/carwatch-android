package de.fau.cs.mad.carwatch.alarmmanager;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.fau.cs.mad.carwatch.Constants;

public class TimerStopReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_INITIAL);
        int notificationId = alarmId + Constants.ALARM_OFFSET_TIMER;

        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.stopAlarmSound();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null)
            notificationManager.cancel(notificationId);
    }
}
