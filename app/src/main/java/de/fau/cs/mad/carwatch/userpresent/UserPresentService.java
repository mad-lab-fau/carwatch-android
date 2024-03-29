package de.fau.cs.mad.carwatch.userpresent;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import org.json.JSONObject;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.MainActivity;

public class UserPresentService extends Service {

    private static final String TAG = UserPresentService.class.getSimpleName();


    private static final int NOTIFICATION_ID = 1994;
    private static final String CHANNEL_ID = TAG + "Channel";

    public static boolean serviceRunning = false;
    public static boolean receiverRegistered = false;

    private UserPresentReceiver userPresentReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        userPresentReceiver = new UserPresentReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Notification channel
        createNotificationChannel();
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
        serviceRunning = true;
        LoggerUtil.log(Constants.LOGGER_ACTION_SERVICE_STARTED, new JSONObject());

        if (userPresentReceiver != null && !receiverRegistered) {
            IntentFilter screenTimeFilter = new IntentFilter();
            screenTimeFilter.addAction(Intent.ACTION_SCREEN_ON);
            screenTimeFilter.addAction(Intent.ACTION_SCREEN_OFF);
            screenTimeFilter.addAction(Intent.ACTION_USER_PRESENT);
            registerReceiver(userPresentReceiver, screenTimeFilter);
            receiverRegistered = true;
        }

        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (userPresentReceiver != null && receiverRegistered) {
            unregisterReceiver(userPresentReceiver);
            receiverRegistered = false;
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH);
        serviceRunning = false;
        LoggerUtil.log(Constants.LOGGER_ACTION_SERVICE_STOPPED, new JSONObject());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // service cannot run as bound service
    }

    public static void startService(Context context) {
        Intent serviceIntent = new Intent(context, UserPresentService.class);
        context.startService(serviceIntent);
    }

    public static void stopService(Context context) {
        Intent serviceIntent = new Intent(context, UserPresentService.class);
        context.stopService(serviceIntent);
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        int pendingFlags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        } else {
            pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bedtime_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.app_active))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setShowWhen(false)
                .setContentIntent(pendingIntent);

        return builder.build();
    }

    private void createNotificationChannel() {
        // creates notification channel (only API 26+ because notification channel class is new and not in support library)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, TAG, NotificationManager.IMPORTANCE_LOW);
                channel.enableLights(false);
                channel.enableVibration(false);
                notificationManager.createNotificationChannel(channel); // register notification channel
            }
        }
    }


}
