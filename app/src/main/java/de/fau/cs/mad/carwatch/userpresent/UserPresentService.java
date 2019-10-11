package de.fau.cs.mad.carwatch.userpresent;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.ui.MainActivity;

public class UserPresentService extends Service {

    private static final String TAG = UserPresentService.class.getSimpleName();


    private static final int NOTIFICATION_ID = 1994;
    private static final String CHANNEL_ID = TAG + "Channel";

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

        if (userPresentReceiver != null) {
            IntentFilter screenTimeFilter = new IntentFilter();
            screenTimeFilter.addAction(Intent.ACTION_SCREEN_ON);
            screenTimeFilter.addAction(Intent.ACTION_SCREEN_OFF);
            screenTimeFilter.addAction(Intent.ACTION_USER_PRESENT);
            registerReceiver(userPresentReceiver, screenTimeFilter);
        }

        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (userPresentReceiver != null) {
            unregisterReceiver(userPresentReceiver);
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // service cannot run as bound service
    }


    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
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
