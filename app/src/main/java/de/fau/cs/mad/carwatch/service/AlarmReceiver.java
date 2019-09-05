package de.fau.cs.mad.carwatch.service;

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

import de.fau.cs.mad.carwatch.AlarmActivity;
import de.fau.cs.mad.carwatch.R;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = AlarmReceiver.class.getSimpleName();

    private final String CHANNEL_ID = "AlarmReceiverChannel";

    @SuppressLint("WrongConstant")
    @Override
    public void onReceive(Context context, Intent intent) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create and add notification channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, TAG, NotificationManager.IMPORTANCE_MAX);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        Notification notification = buildNotification(context);

        Log.i(TAG, "displaying notification for alarm");
        if (notificationManager != null) {
            notificationManager.notify(0x01, notification);
        }
    }

    private Notification buildNotification(Context context) {
        // Full screen Intent
        Intent fullScreenIntent = new Intent(context, AlarmActivity.class);
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
                .setSmallIcon(R.drawable.ic_alarm_black_24dp)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("ALARM ALARM!")
                .setFullScreenIntent(fullScreenPendingIntent, true);

        return builder.build();
    }
}
