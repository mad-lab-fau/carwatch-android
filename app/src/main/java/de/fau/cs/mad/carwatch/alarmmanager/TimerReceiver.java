package de.fau.cs.mad.carwatch.alarmmanager;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.util.Utils;

import static android.os.Build.VERSION;
import static android.os.Build.VERSION_CODES;

import androidx.preference.PreferenceManager;

/**
 * Called when countdown timer to take saliva sample is over.
 */
public class TimerReceiver extends BroadcastReceiver {

    private static final String TAG = TimerReceiver.class.getSimpleName();

    private static final String CHANNEL_ID = "TimerReceiverChannel";

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

        int timerId = intent.getIntExtra(Constants.EXTRA_TIMER_ID, Constants.EXTRA_TIMER_ID_INITIAL);
        int salivaId = intent.getIntExtra(Constants.EXTRA_SALIVA_ID, Constants.EXTRA_SALIVA_ID_INITIAL);

        Notification notification = TimerHandler.buildAlarmNotification(context, timerId, salivaId);

        // Play alarm ringing sound
        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.playAlarmSound(context);

        if (notificationManager != null) {
            notificationManager.notify(timerId, notification);
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(Constants.PREF_TIMER_NOTIFICATION_IS_SHOWN, true).apply();
    }


}
