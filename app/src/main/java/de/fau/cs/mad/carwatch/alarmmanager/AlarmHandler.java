package de.fau.cs.mad.carwatch.alarmmanager;

import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.MainActivity;
import de.fau.cs.mad.carwatch.userpresent.BootCompletedReceiver;
import de.fau.cs.mad.carwatch.util.AlarmRepository;


/**
 * Class to control alarm scheduling/cancelling
 */
public class AlarmHandler {

    private static final String TAG = AlarmHandler.class.getSimpleName();

    private static final PeriodFormatter formatter;

    static {
        switch(Locale.getDefault().getLanguage()) {
            case "de":
                formatter = new PeriodFormatterBuilder()
                        .appendHours()
                        .appendSuffix(" Stunde", " Stunden")
                        .appendSeparator(" und ")
                        .appendMinutes()
                        .appendSuffix(" Minute", " Minuten")
                        .toFormatter();
                break;
            case "fr":
                formatter = new PeriodFormatterBuilder()
                        .appendHours()
                        .appendSuffix(" heure", " heures")
                        .appendSeparator(" et ")
                        .appendMinutes()
                        .appendSuffix(" minute", " minutes")
                        .toFormatter();
                break;
            default:
                formatter = new PeriodFormatterBuilder()
                        .appendHours()
                        .appendSuffix(" hour", " hours")
                        .appendSeparator(" and ")
                        .appendMinutes()
                        .appendSuffix(" minute", " minutes")
                        .toFormatter();
        }
    }

    public static void scheduleWakeUpAlarm(@NonNull Context context, Alarm alarm) {
        scheduleWakeUpAlarm(context, alarm, null);
    }

    /**
     * Schedule first alarm notification
     *
     * @param alarm Alarm to schedule
     */
    public static void scheduleWakeUpAlarm(@NonNull Context context, Alarm alarm, View snackBarAnchor) {
        if (!alarm.isActive())
            return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            alarm.setActive(false);
            if (snackBarAnchor != null) {
                Snackbar.make(snackBarAnchor, context.getString(R.string.alarm_set_error), Snackbar.LENGTH_SHORT).show();
            }
            return;
        }

        PendingIntent showIntent = getPendingIntentShow(context, alarm);
        PendingIntent operation = getPendingIntent(context, alarm);

        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(alarm.getTimeToNextRing().getMillis(), showIntent);
        alarmManager.setAlarmClock(info, operation);

        logAlarmSet(alarm, alarm.getTimeToNextRing());

        ComponentName receiver = new ComponentName(context, BootCompletedReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        showAlarmSetMessage(context, snackBarAnchor, alarm.getTimeToNextRing());
    }

    /**
     * Schedule all saliva alarms with relative and fixed times except for the wake-up alarm
     *
     * @param context Context to use
     */
    public static void scheduleSalivaAlarms(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        AlarmRepository repo = AlarmRepository.getInstance((Application) context.getApplicationContext());

        String fixedTimesString = sp.getString(Constants.PREF_SALIVA_TIMES, "");
        String timeDistancesString = sp.getString(Constants.PREF_SALIVA_DISTANCES, "");
        String[] timeDistances = timeDistancesString.split(",");
        List<DateTime> alarmTimes = new ArrayList<>();
        List<Boolean> isFixed = new ArrayList<>();
        DateTime lastAlarmTime = DateTime.now();

        for (String distanceString : timeDistances) {
            if (distanceString.isEmpty() || distanceString.equals("0"))
                continue;
            int distance = Integer.parseInt(distanceString);
            lastAlarmTime = lastAlarmTime.plusMinutes(distance);
            alarmTimes.add(lastAlarmTime);
            isFixed.add(false);
        }

        for (String timeRaw : fixedTimesString.split(",")) {
            if (timeRaw.isEmpty())
                continue;
            String time = timeRaw.substring(0, 2) + ":" + timeRaw.substring(2);
            alarmTimes.add(DateTime.now().withTime(LocalTime.parse(time)));
            isFixed.add(true);
        }

        int id = sp.getInt(Constants.PREF_CURRENT_ALARM_ID, 1);
        int salivaId = Constants.EXTRA_SALIVA_ID_INITIAL;
        if (timeDistancesString.startsWith("0"))
            // if first sample request has no offset, it was already scheduled with the first alarm
            salivaId++;

        for (int i = 0; i < alarmTimes.size(); i++) {
            Alarm alarm = new Alarm(alarmTimes.get(i), true, isFixed.get(i), id++, salivaId++);
            repo.insert(alarm);
            AlarmHandler.scheduleSalivaAlarm(context, alarm, null);
        }
        sp.edit().putInt(Constants.PREF_CURRENT_ALARM_ID, id).apply();
        sp.edit().putBoolean(Constants.PREF_SALIVA_ALARMS_ARE_SCHEDULED, true).apply();
    }

    public static void showAlarmSetMessage(Context context, View snackBarAnchor, DateTime time) {
        Period timeDiff = new Period(DateTime.now(), time);
        if (snackBarAnchor != null) {
            String timeDiffString = formatter.print(timeDiff);

            switch (Locale.getDefault().getLanguage()) {
                case "de":
                    if (timeDiffString.length() == 0) {
                        timeDiffString += "jetzt";
                    } else {
                        timeDiffString = "in " + timeDiffString;
                    }
                    break;
                case "fr":
                    if (timeDiffString.length() != 0) {
                        timeDiffString = "pour " + timeDiffString;
                    }
                    break;
                default:
                    if (timeDiffString.length() != 0) {
                        timeDiffString += " from ";
                    }
            }

            Snackbar.make(snackBarAnchor, context.getString(R.string.alarm_set, timeDiffString), Snackbar.LENGTH_SHORT).show();
        }
    }

    public static void scheduleSalivaAlarm(Context context, Alarm alarm, View snackbarAnchor) {
        DateTime alarmTime = alarm.getTimeToNextRing();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            Log.e(TAG, "Could not set alarm with id: " + alarm.getId() + " at " + alarmTime.getMillis() + " because alarmManager is null");
            return;
        }

        PendingIntent showIntent = getPendingIntentShow(context, alarm);
        PendingIntent subsequentOperation = getPendingIntent(context, alarm);

        Log.d(TAG, "Setting timed alarm " + alarm.getId() + " at " + alarmTime);
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(alarmTime.getMillis(), showIntent);
        alarmManager.setAlarmClock(info, subsequentOperation);

        try {
            // create Json object and log information
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarm.getId());
            json.put(Constants.LOGGER_EXTRA_ALARM_TIMESTAMP, alarmTime.getMillis());
            LoggerUtil.log(Constants.LOGGER_ACTION_TIMER_SET, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        showAlarmSetMessage(context, snackbarAnchor, alarmTime);
    }

    /**
     * Cancel alarm notification AlarmManager
     *
     * @param alarm Alarm to cancel
     */
    public static void cancelAlarm(Context context, Alarm alarm, View snackBarAnchor) {
        // Get PendingIntent to AlarmReceiver Broadcast channel
        Intent intent = new Intent(context, AlarmReceiver.class);

        int pendingFlags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags = PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE;
        } else {
            pendingFlags = PendingIntent.FLAG_NO_CREATE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarm.getId(), intent, pendingFlags);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // PendingIntent may be null if the alarm hasn't been set
        if (alarmManager != null && pendingIntent != null) {
            Log.d(TAG, "Cancelling alarm " + alarm.getId());

            try {
                // create Json object and log information
                JSONObject json = new JSONObject();
                json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarm.getId());
                LoggerUtil.log(Constants.LOGGER_ACTION_ALARM_CANCEL, json);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            alarmManager.cancel(pendingIntent);

            ComponentName receiver = new ComponentName(context, BootCompletedReceiver.class);
            PackageManager pm = context.getPackageManager();

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }

        if (snackBarAnchor != null) {
            // Show snackbar to notify user
            Snackbar.make(snackBarAnchor, context.getString(R.string.alarm_cancelled), Snackbar.LENGTH_SHORT).show();
        }
    }


    private static void cancelAlarmAtTime(Context context, int alarmId) {
        // Get PendingIntent to AlarmReceiver Broadcast channel
        Intent intent = new Intent(context, AlarmReceiver.class);

        int pendingFlags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags = PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE;
        } else {
            pendingFlags = PendingIntent.FLAG_NO_CREATE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarmId, intent, pendingFlags);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Log.d(TAG, "Cancelling alarm " + alarmId);
        Log.d(TAG, "Cancelling alarm " + pendingIntent);

        // PendingIntent may be null if the alarm hasn't been set
        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }

        if (notificationManager != null) {
            notificationManager.cancel(alarmId);
        }
    }

    private static PendingIntent getPendingIntent(Context context, Alarm alarm) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(Constants.EXTRA_ALARM_ID, alarm.getId());

        return PendingIntent.getBroadcast(context, alarm.getId(), intent, getPendingIntentFlags());
    }

    private static PendingIntent getPendingIntentShow(Context context, Alarm alarm) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constants.EXTRA_ALARM_ID, alarm.getId());

        return PendingIntent.getActivity(
                context,
                Constants.REQUEST_CODE_ALARM_ACTIVITY,
                intent,
                getPendingIntentFlags());
    }

    private static int getPendingIntentFlags() {
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        return pendingFlags;
    }

    private static void logAlarmSet(Alarm alarm, DateTime nextRing) {
        try {
            // create Json object and log information
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarm.getId());
            json.put(Constants.LOGGER_EXTRA_ALARM_TIMESTAMP, nextRing.getMillis());

            LoggerUtil.log(Constants.LOGGER_ACTION_ALARM_SET, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void killAll(Application application) {
        LoggerUtil.log(Constants.LOGGER_ACTION_ALARM_KILLALL, new JSONObject());

        AlarmRepository repo = AlarmRepository.getInstance(application);
        List<Alarm> alarms = repo.getAlarms().getValue();

        if (alarms == null)
            return;

        for (Alarm alarm : alarms) {
            cancelAlarmAtTime(application, alarm.getId());
            TimerHandler.cancelTimer(application, alarm.getId());
            alarm.setActive(false);
            repo.update(alarm);
        }
    }
}
