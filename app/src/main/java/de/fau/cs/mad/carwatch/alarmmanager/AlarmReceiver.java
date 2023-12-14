package de.fau.cs.mad.carwatch.alarmmanager;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.ShowAlarmActivity;
import de.fau.cs.mad.carwatch.userpresent.UserPresentService;

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
        int salivaId = intent.getIntExtra(Constants.EXTRA_SALIVA_ID, Constants.EXTRA_SALIVA_ID_INITIAL);

        Notification notification = buildNotification(context, alarmId, salivaId);

        // Play alarm ringing sound
        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.playAlarmSound(context);

        Log.d(TAG, "Displaying notification for alarm " + alarmId);
        try {
            // create Json object and log information
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarmId);
            json.put(Constants.LOGGER_EXTRA_SALIVA_ID, salivaId);
            LoggerUtil.log(Constants.LOGGER_ACTION_ALARM_RING, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (notificationManager != null) {
            notificationManager.notify(alarmId, notification);
        }
    }

    private Notification buildNotification(Context context, int alarmId, int salivaId) {
        PendingIntent stopIntent = createStopAlarmIntent(context, alarmId, salivaId);

        // Full screen Intent
        Intent fullScreenIntent = new Intent(context, ShowAlarmActivity.class);
        fullScreenIntent.putExtra(Constants.EXTRA_ALARM_ID, alarmId);
        fullScreenIntent.putExtra(Constants.EXTRA_SALIVA_ID, salivaId);

        int pendingFlags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        }
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
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.alarm_notification_text))
                .addAction(R.drawable.ic_stop_white_24dp, context.getString(R.string.stop), stopIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true);

        return builder.build();
    }

    /**
     * Get PendingIntent to Stop Alarm
     *
     * @param context current App context
     * @param alarmId ID of alarm to handle
     * @return PendingIntent to AlarmStop(Broadcast)Receiver
     */
    private PendingIntent createStopAlarmIntent(Context context, int alarmId, int salivaId) {
        Intent stopAlarmIntent = new Intent(context, AlarmStopReceiver.class);
        stopAlarmIntent.putExtra(Constants.EXTRA_ALARM_ID, alarmId);
        stopAlarmIntent.putExtra(Constants.EXTRA_SALIVA_ID, salivaId);
        stopAlarmIntent.putExtra(Constants.EXTRA_SOURCE, AlarmSource.SOURCE_NOTIFICATION);
        stopAlarmIntent.setAction(Constants.ACTION_STOP_ALARM);

        int pendingFlags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        }
        return PendingIntent.getBroadcast(context, 0, stopAlarmIntent, pendingFlags);
    }

}
