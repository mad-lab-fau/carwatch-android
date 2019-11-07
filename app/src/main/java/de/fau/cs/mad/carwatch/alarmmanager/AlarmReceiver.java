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
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            if (notificationManager != null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, TAG, NotificationManager.IMPORTANCE_MAX);
                notificationManager.createNotificationChannel(channel);
            }
        }

        // stop user present service if running
        if (UserPresentService.serviceRunning) {
            UserPresentService.stopService(context);
        }

        boolean isHidden = false;

        int alarmId = intent.getIntExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_DEFAULT);
        int salivaId = intent.getIntExtra(Constants.EXTRA_SALIVA_ID, Constants.EXTRA_SALIVA_ID_DEFAULT);

        // convert id from hidden alarm to regular alarm id (will be needed in SnoozeReceiver and StopReceiver)
        if (alarmId > Integer.MAX_VALUE / 2) {
            isHidden = true;
            alarmId = Integer.MAX_VALUE - alarmId;
        }

        Notification notification = buildNotification(context, alarmId, salivaId);

        // Play alarm ringing sound
        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.playAlarmSound(context);

        Log.d(TAG, "Displaying notification for alarm " + alarmId);
        try {
            // create Json object and log information
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarmId);
            json.put(Constants.EXTRA_SALIVA_ID, salivaId);
            json.put(Constants.LOGGER_EXTRA_ALARM_IS_HIDDEN, isHidden);
            LoggerUtil.log(Constants.LOGGER_ACTION_ALARM_RING, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (notificationManager != null) {
            notificationManager.notify(alarmId, notification);
        }
    }

    private Notification buildNotification(Context context, int alarmId, int salivaId) {
        //PendingIntent snoozeIntent = createSnoozeAlarmIntent(context, alarmId);
        PendingIntent stopIntent = createStopAlarmIntent(context, alarmId, salivaId);

        // Full screen Intent
        Intent fullScreenIntent = new Intent(context, ShowAlarmActivity.class);
        fullScreenIntent.putExtra(Constants.EXTRA_ALARM_ID, alarmId);
        fullScreenIntent.putExtra(Constants.EXTRA_SALIVA_ID, salivaId);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

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
                // Snooze function disabled
                //.addAction(R.drawable.ic_snooze_white_24dp, context.getString(R.string.snooze), snoozeIntent)
                .addAction(R.drawable.ic_stop_white_24dp, context.getString(R.string.stop), stopIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true);

        return builder.build();
    }


    /*
      Get PendingIntent to Snooze Alarm
      @param context current App context
     * @param alarmId ID of alarm to handle
     * @return PendingIntent to AlarmSnooze(Broadcast)Receiver
     */
    /*
    private PendingIntent createSnoozeAlarmIntent(Context context, int alarmId) {
        Intent snoozeAlarmIntent = new Intent(context, AlarmSnoozeReceiver.class);
        snoozeAlarmIntent.putExtra(Constants.EXTRA_ALARM_ID, alarmId);
        snoozeAlarmIntent.putExtra(Constants.EXTRA_SOURCE, AlarmSource.SOURCE_NOTIFICATION);
        snoozeAlarmIntent.setAction(Constants.ACTION_SNOOZE_ALARM);
        return PendingIntent.getBroadcast(context, 0, snoozeAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }*/

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
        return PendingIntent.getBroadcast(context, 0, stopAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
