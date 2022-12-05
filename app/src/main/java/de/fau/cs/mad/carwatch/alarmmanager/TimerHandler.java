package de.fau.cs.mad.carwatch.alarmmanager;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.json.JSONException;
import org.json.JSONObject;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.BarcodeActivity;

public class TimerHandler {

    private static final String TAG = TimerHandler.class.getSimpleName();

    private static final String CHANNEL_ID = TAG + "Channel";

    public static void finishTimer(Context context) {
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            int dayId = sp.getInt(Constants.PREF_DAY_COUNTER, 0);

            // create Json object and log information
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_DAY_COUNTER, dayId);
            LoggerUtil.log(Constants.LOGGER_ACTION_DAY_FINISHED, json);

            // one day was completed
            // save the day the saliva sample was taken in order to prevent abuse
            sp.edit()
                    .putInt(Constants.PREF_DAY_COUNTER, ++dayId)
                    .putLong(Constants.PREF_MORNING_TAKEN, LocalTime.MIDNIGHT.toDateTimeToday().getMillis())
                    .putInt(Constants.PREF_MORNING_ONGOING, Constants.EXTRA_ALARM_ID_DEFAULT)
                    .apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static long scheduleSalivaTimer(Context context, int alarmId, int salivaId) {
        if (salivaId < Constants.SALIVA_TIMES.length) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putInt(Constants.PREF_MORNING_ONGOING, alarmId).apply();

            if (Constants.SALIVA_TIMES[salivaId] == 0) {
                // first saliva sample => directly schedule barcode scan timer
                scheduleSalivaCountdown(context, alarmId, salivaId);
                return 0;
            } else {
                DateTime timeToRing = DateTime.now().plusMinutes(Constants.SALIVA_TIMES[salivaId]);
                AlarmHandler.scheduleAlarmAtTime(context, timeToRing, alarmId, salivaId, null);
                return timeToRing.getMillis();
            }
        } else if (salivaId == Constants.EXTRA_SALIVA_ID_EVENING) {
            // evening saliva sample => directly schedule barcode scan timer
            scheduleSalivaCountdown(context, alarmId, salivaId);
            return 0;
        }
        return 0;
    }

    @SuppressLint("WrongConstant")
    public static void scheduleSalivaCountdown(Context context, int alarmId, int salivaId) {
        long when = DateTime.now().plusMinutes(Constants.TIMER_DURATION).getMillis();
        //long when = DateTime.now().plusSeconds(Constants.TIMER_DURATION).getMillis();TODO change back

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Create and add notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, TAG, NotificationManager.IMPORTANCE_MAX);
                notificationManager.createNotificationChannel(channel);
            }
        }

        Notification notification = buildCountdownNotification(context, alarmId, salivaId, when);

        if (alarmManager != null) {
            PendingIntent pendingIntent = getTimerPendingIntent(context, alarmId, salivaId);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, when, pendingIntent);
        }

        if (notificationManager != null) {
            notificationManager.notify(alarmId, notification);
        }
    }


    public static void cancelTimer(Context context, int alarmId) {
        // Dismiss notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Get PendingIntent to TimerReceiver Broadcast channel
        Intent intent = new Intent(context, TimerReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarmId, intent, PendingIntent.FLAG_NO_CREATE);

        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Cancelling timer " + alarmId + " for alarm " + alarmId);
            Log.d(TAG, "Cancelling timer " + pendingIntent);
        }

        if (notificationManager != null) {
            notificationManager.cancel(alarmId);
        }

        // Play alarm ringing sound
        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.stopAlarmSound();
    }


    private static Notification buildCountdownNotification(Context context, int alarmId, int salivaId, long when) {
        Intent contentIntent = new Intent(context, BarcodeActivity.class);
        contentIntent.putExtra(Constants.EXTRA_ALARM_ID, alarmId);
        contentIntent.putExtra(Constants.EXTRA_TIMER_ID, alarmId);
        contentIntent.putExtra(Constants.EXTRA_SALIVA_ID, salivaId);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(context, 0,
                contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String contentText =
                salivaId == Constants.EXTRA_SALIVA_ID_EVENING ?
                        context.getString(R.string.timer_notification_text_evening) :
                        context.getString(R.string.timer_notification_text, salivaId);

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
                .setContentText(contentText);

        return builder.build();
    }

    public static Notification buildAlarmNotification(Context context, int alarmId, int salivaId) {
        // Full screen Intent
        Intent fullScreenIntent = new Intent(context, BarcodeActivity.class);
        fullScreenIntent.putExtra(Constants.EXTRA_ALARM_ID, alarmId);
        fullScreenIntent.putExtra(Constants.EXTRA_SALIVA_ID, salivaId);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String contentText =
                salivaId == Constants.EXTRA_SALIVA_ID_EVENING ?
                        context.getString(R.string.timer_over_notification_text_evening) :
                        context.getString(R.string.timer_over_notification_text, salivaId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setVibrate(Constants.VIBRATION_PATTERN)
                .setSmallIcon(R.drawable.ic_alarm_white_24dp)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(contentText)
                .setFullScreenIntent(fullScreenPendingIntent, true);

        return builder.build();
    }

    private static PendingIntent getTimerPendingIntent(Context context, int timerId, int salivaId) {
        // Get PendingIntent to TimerReceiver Broadcast
        Intent intent = new Intent(context, TimerReceiver.class);
        intent.putExtra(Constants.EXTRA_TIMER_ID, timerId);
        intent.putExtra(Constants.EXTRA_SALIVA_ID, salivaId);

        return PendingIntent.getBroadcast(context, timerId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
