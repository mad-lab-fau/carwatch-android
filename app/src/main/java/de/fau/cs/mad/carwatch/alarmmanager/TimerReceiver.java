package de.fau.cs.mad.carwatch.alarmmanager;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.ui.ScannerActivity;

import static android.os.Build.VERSION;
import static android.os.Build.VERSION_CODES;

public class TimerReceiver extends BroadcastReceiver {

    private static final String TAG = TimerReceiver.class.getSimpleName();

    private final String CHANNEL_ID = "TimerReceiverChannel";

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

        int alarmId = intent.getIntExtra(Constants.EXTRA_ID, -1);

        Notification notification = buildNotification(context, alarmId);

        // Play alarm ringing sound
        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.playAlarmSound(context);

        if (notificationManager != null) {
            notificationManager.notify(alarmId, notification);
        }
    }


    private Notification buildNotification(Context context, int alarmId) {
        // Full screen Intent
        Intent fullScreenIntent = new Intent(context, ScannerActivity.class);
        fullScreenIntent.putExtra(Constants.EXTRA_ID, alarmId);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_alarm_white_24dp)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.alarm_notification_text))
                .setFullScreenIntent(fullScreenPendingIntent, true);

        return builder.build();
    }
}
