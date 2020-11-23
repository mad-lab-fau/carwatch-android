package de.fau.cs.mad.carwatch.notification;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;

import org.json.JSONException;
import org.json.JSONObject;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;

public class NotificationListenerService extends android.service.notification.NotificationListenerService {

    private static final String TAG = NotificationListenerService.class.getSimpleName();

    private static final String[] SKIP_NOTIFICATIONS = {
            "com.android.vending", // Google Play Store
            "com.google.android.as" // Android System
    };

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // skip own notifications
        if ("de.fau.cs.mad.carwatch".equals(sbn.getPackageName())) {
            return;
        }

        if (isUserPresentServiceRunning() && checkLogNotification(sbn)) {
            JSONObject json = new JSONObject();
            try {
                json.put(Constants.LOGGER_EXTRA_NOTIFICATION_PACKAGE, sbn.getPackageName());
                json.put(Constants.LOGGER_EXTRA_NOTIFICATION_ID, sbn.getId());
                json.put(Constants.LOGGER_EXTRA_NOTIFICATION_KEY, sbn.getKey());
                json.put(Constants.LOGGER_EXTRA_NOTIFICATION_POST_TIME, sbn.getPostTime());
                json.put(Constants.LOGGER_EXTRA_NOTIFICATION_CATEGORY, sbn.getNotification().category);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            LoggerUtil.log(Constants.LOGGER_ACTION_NOTIFICATION_RECEIVED, json);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap, int reason) {
        super.onNotificationRemoved(sbn, rankingMap, reason);
        // skip own notifications
        if ("de.fau.cs.mad.carwatch".equals(sbn.getPackageName())) {
            return;
        }

        if (isUserPresentServiceRunning() && checkLogNotification(sbn)) {
            JSONObject json = new JSONObject();
            try {
                json.put(Constants.LOGGER_EXTRA_NOTIFICATION_PACKAGE, sbn.getPackageName());
                json.put(Constants.LOGGER_EXTRA_NOTIFICATION_ID, sbn.getId());
                json.put(Constants.LOGGER_EXTRA_NOTIFICATION_KEY, sbn.getKey());
                json.put(Constants.LOGGER_EXTRA_NOTIFICATION_REMOVED_REASON, reason);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            LoggerUtil.log(Constants.LOGGER_ACTION_NOTIFICATION_REMOVED, json);
        }
    }

    private boolean isUserPresentServiceRunning() {
        // check if UserPresentService is running (i.e. during night) => log notifications
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (service.service.getClassName().contains("UserPresentService")) {
                return true;
            }
        }
        return false;
    }

    private boolean checkLogNotification(StatusBarNotification sbn) {
        for (String packageName : SKIP_NOTIFICATIONS) {
            if (sbn.getPackageName().contains(packageName)) {
                return false;
            }
        }
        return true;
    }
}
