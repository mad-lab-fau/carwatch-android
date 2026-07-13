package de.fau.cs.mad.carwatch.alarmmanager;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.ShowAlarmActivity;
import de.fau.cs.mad.carwatch.userpresent.UserPresentService;
import de.fau.cs.mad.carwatch.util.AlarmRepository;

import static android.os.Build.VERSION;
import static android.os.Build.VERSION_CODES;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = AlarmReceiver.class.getSimpleName();
    private final String CHANNEL_ID = TAG + "Channel";

    @SuppressLint("WrongConstant")
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create and add notification channel
        if (VERSION.SDK_INT >= VERSION_CODES.O && notificationManager != null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, TAG, NotificationManager.IMPORTANCE_MAX);
            notificationManager.createNotificationChannel(channel);
        }

        // stop user present service if running
        if (UserPresentService.serviceRunning) {
            UserPresentService.stopService(context);
        }

        int alarmId = intent.getIntExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_INITIAL);

        AlarmRepository repository = AlarmRepository.getInstance((Application) context.getApplicationContext());
        Alarm alarm;

        try {
            alarm = repository.getAlarmById(alarmId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error while getting alarm with id " + alarmId + " from database", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        if (alarm == null) {
            Log.e(TAG, "No alarm found with id " + alarmId);
            return;
        }

        if (alarmId == Constants.EXTRA_ALARM_ID_INITIAL) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            preferences.edit()
                    .putLong(Constants.PREF_PENDING_WAKEUP_NOTIFICATION_TIME, System.currentTimeMillis())
                    .putBoolean(
                            Constants.PREF_SHOULD_FINISH_PREVIOUS_DAY_ON_WAKEUP,
                            AlarmHandler.hasUnfinishedCurrentStudyDay(context))
                    .apply();
        }

        Notification notification = buildNotification(context, alarm);

        // Play alarm ringing sound
        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.playAlarmSound(context);

        try {
            // create Json object and log information
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarmId);
            json.put(Constants.LOGGER_EXTRA_SALIVA_ID, alarm.getSalivaId());
            LoggerUtil.log(Constants.LOGGER_ACTION_ALARM_RING, json);
        } catch (JSONException e) {
            Log.e(TAG, "Error while creating JSON object for logger for alarm with ID " + alarmId, e);
        }

        if (notificationManager != null) {
            Log.d(TAG, "Displaying notification for alarm " + alarmId);
            notificationManager.notify(alarmId, notification);
        }
    }

    private Notification buildNotification(Context context, Alarm alarm) {
        PendingIntent stopIntent = createStopAlarmIntent(context, alarm);

        Intent fullScreenIntent = new Intent(context, ShowAlarmActivity.class);
        fullScreenIntent.putExtra(Constants.EXTRA_ALARM_ID, alarm.getId());

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, 0,
                fullScreenIntent, pendingFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setVibrate(Constants.VIBRATION_PATTERN)
                .setSmallIcon(R.drawable.ic_alarm_white_24dp)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setColorized(false)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(getNotificationText(context, alarm))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getNotificationText(context, alarm)))
                .setContentIntent(stopIntent)
                .setAutoCancel(false)
                .addAction(R.drawable.ic_stop_black_24dp, context.getString(R.string.stop), stopIntent);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true);
        } else {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null && notificationManager.canUseFullScreenIntent()) {
                builder.setFullScreenIntent(fullScreenPendingIntent, true);
            }
        }

        return builder.build();
    }

    private String getNotificationText(Context context, Alarm alarm) {
        if (alarm.getSalivaId() < 0) {
            return context.getString(R.string.alarm_notification_text);
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int startSampleIdx = Integer.parseInt(sp.getString(Constants.PREF_START_SAMPLE, Constants.DEFAULT_START_SAMPLE).substring(1));
        return context.getString(R.string.timer_notification_text, alarm.getSalivaId() + startSampleIdx);
    }


    /**
        * Creates a PendingIntent to stop the alarm
        *
        * @param context Context
        * @param alarm   Alarm to stop
        * @return PendingIntent to stop the alarm
    */
    private PendingIntent createStopAlarmIntent(Context context, Alarm alarm) {
        Intent stopAlarmIntent = new Intent(context, AlarmStopReceiver.class);
        stopAlarmIntent.putExtra(Constants.EXTRA_ALARM_ID, alarm.getId());
        stopAlarmIntent.putExtra(Constants.EXTRA_SALIVA_ID, alarm.getSalivaId());
        stopAlarmIntent.putExtra(Constants.EXTRA_SOURCE, AlarmSource.SOURCE_NOTIFICATION);
        stopAlarmIntent.setAction(Constants.ACTION_STOP_ALARM);

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, 0, stopAlarmIntent, pendingFlags);
    }

}
