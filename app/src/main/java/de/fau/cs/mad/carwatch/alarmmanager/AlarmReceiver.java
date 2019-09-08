package de.fau.cs.mad.carwatch.alarmmanager;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.ui.ShowAlarmActivity;

import static android.os.Build.VERSION;
import static android.os.Build.VERSION_CODES;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = AlarmReceiver.class.getSimpleName();
    private final String CHANNEL_ID = "AlarmReceiverChannel";

    @SuppressLint("WrongConstant")
    @Override
    public void onReceive(Context context, Intent intent) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create and add notification channel
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            if (notificationManager != null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, TAG, NotificationManager.IMPORTANCE_MAX);
                notificationManager.createNotificationChannel(channel);
            }
        }

        int alarmId = intent.getIntExtra(Constants.EXTRA_ID, 0);
        Notification notification = buildNotification(context, alarmId);

        // Play alarm ringing sound
        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.playAlarmSound(context);

        Log.d(TAG, "Displaying notification for alarm " + alarmId);
        if (notificationManager != null) {
            notificationManager.notify(alarmId, notification);
        }
    }

    private Notification buildNotification(Context context, int alarmId) {
        // Full screen Intent
        PendingIntent snoozeIntent = createSnoozeAlarmIntent(context, alarmId);
        PendingIntent stopIntent = createStopAlarmIntent(context, alarmId);


        Intent fullScreenIntent = new Intent(context, ShowAlarmActivity.class);
        fullScreenIntent.putExtra(Constants.EXTRA_ID, alarmId);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                // TODO only for development!
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_alarm_white_24dp)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.alarm_notification_text))
                .addAction(R.drawable.ic_snooze_white_24dp, context.getString(R.string.snooze), snoozeIntent)
                .addAction(R.drawable.ic_stop_white_24dp, context.getString(R.string.stop), stopIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true);

        return builder.build();
    }


    /**
     * Get PendingIntent to Snooze Alarm
     *
     * @param context current App context
     * @param alarmId ID of alarm to handle
     * @return PendingIntent to AlarmSnooze(Broadcast)Receiver
     */
    private PendingIntent createSnoozeAlarmIntent(Context context, int alarmId) {
        Intent snoozeAlarmIntent = new Intent(context, AlarmSnoozeReceiver.class);
        snoozeAlarmIntent.putExtra(Constants.EXTRA_ID, alarmId);
        snoozeAlarmIntent.setAction("Snooze Alarm");
        return PendingIntent.getBroadcast(context, 0, snoozeAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Get PendingIntent to Stop Alarm
     *
     * @param context current App context
     * @param alarmId ID of alarm to handle
     * @return PendingIntent to AlarmStop(Broadcast)Receiver
     */
    private PendingIntent createStopAlarmIntent(Context context, int alarmId) {
        Intent stopAlarmIntent = new Intent(context, AlarmStopReceiver.class);
        stopAlarmIntent.putExtra(Constants.EXTRA_ID, alarmId);
        stopAlarmIntent.setAction("Stop Alarm");
        return PendingIntent.getBroadcast(context, 0, stopAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
