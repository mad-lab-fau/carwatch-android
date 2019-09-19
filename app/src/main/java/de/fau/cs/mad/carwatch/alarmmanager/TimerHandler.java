package de.fau.cs.mad.carwatch.alarmmanager;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.joda.time.DateTime;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.ui.ScannerActivity;

public class TimerHandler {

    private static final String TAG = TimerHandler.class.getSimpleName();

    private static final String CHANNEL_ID = "TimerHandlerChannel";


    public static void scheduleSalivaTimer(Context context, int alarmId, int salivaId) {
        int timerId = alarmId + Constants.ALARM_OFFSET_TIMER;
        if (salivaId < Constants.SALIVA_TIMES.length) {
            if (Constants.SALIVA_TIMES[salivaId] == 0) {
                scheduleSalivaCountdown(context, timerId, salivaId);
            } else {
                DateTime timeToRing = DateTime.now().plusMinutes(Constants.SALIVA_TIMES[salivaId]);
                AlarmHandler.scheduleAlarmAtTime(context, timeToRing, timerId, salivaId);
            }
        }
    }

    @SuppressLint("WrongConstant")
    public static void scheduleSalivaCountdown(Context context, int timerId, int salivaId) {

        long when = DateTime.now().plusMinutes(Constants.TIMER_DURATION).getMillis();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Create and add notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, TAG, NotificationManager.IMPORTANCE_MAX);
                notificationManager.createNotificationChannel(channel);
            }
        }

        Notification notification = buildCountdownNotification(context, timerId, salivaId, when);

        if (alarmManager != null) {
            PendingIntent pendingIntent = getTimerPendingIntent(context, timerId, salivaId);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, when, pendingIntent);
        }

        if (notificationManager != null) {
            notificationManager.notify(timerId, notification);
        }
    }


    public static void cancelTimer(Context context, int timerId) {
        // Dismiss notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Get PendingIntent to TimerReceiver Broadcast channel
        Intent intent = new Intent(context, TimerReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, timerId, intent, PendingIntent.FLAG_NO_CREATE);

        Log.d(TAG, "Cancelling timer " + timerId);

        if (notificationManager != null) {
            notificationManager.cancel(timerId);
        }

        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }

        // Play alarm ringing sound
        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.stopAlarmSound();
    }


    private static Notification buildCountdownNotification(Context context, int timerId, int salivaId, long when) {
        Intent contentIntent = new Intent(context, ScannerActivity.class);
        contentIntent.putExtra(Constants.EXTRA_ALARM_ID, timerId);
        contentIntent.putExtra(Constants.EXTRA_SALIVA_ID, salivaId);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(context, 0,
                contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(when)
                .setUsesChronometer(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setContentIntent(contentPendingIntent)
                .setSmallIcon(R.drawable.ic_alarm_white_24dp)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.timer_notification_text, salivaId));

        return builder.build();
    }

    private static PendingIntent getTimerPendingIntent(Context context, int timerId, int salivaId) {
        // Get PendingIntent to TimerReceiver Broadcast
        Intent intent = new Intent(context, TimerReceiver.class);
        intent.putExtra(Constants.EXTRA_ALARM_ID, timerId);
        intent.putExtra(Constants.EXTRA_SALIVA_ID, salivaId);

        return PendingIntent.getBroadcast(context, timerId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    // kills all timers for a alarm
    public static void killAllTimersForAlarm(Context context, int alarmId) {
        int timerId = alarmId;
        for (int ignored : Constants.SALIVA_TIMES) {
            TimerHandler.cancelTimer(context, timerId);
            timerId += Constants.ALARM_OFFSET_TIMER;
        }
    }

}
