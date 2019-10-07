package de.fau.cs.mad.carwatch.alarmmanager;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.subject.Condition;
import de.fau.cs.mad.carwatch.subject.SubjectMap;
import de.fau.cs.mad.carwatch.ui.MainActivity;
import de.fau.cs.mad.carwatch.util.AlarmRepository;


/**
 * Class to control alarm scheduling/cancelling
 */
public class AlarmHandler {

    private static final String TAG = AlarmHandler.class.getSimpleName();

    private static PeriodFormatter formatter = new PeriodFormatterBuilder()
            .appendHours()
            .appendSuffix(" hour", " hours")
            .appendSeparatorIfFieldsBefore(" from ")
            .appendMinutes()
            .appendSuffix(" minute", " minutes")
            .appendSeparatorIfFieldsBefore(" from ")
            .toFormatter();

    public static void scheduleAlarm(@NonNull Context context, Alarm alarm) {
        scheduleAlarm(context, alarm, null);
    }

    /**
     * Schedule alarm TimeShiftReceiver an hour before alarm's time using AlarmManager
     *
     * @param alarm Alarm to schedule
     */
    public static void scheduleAlarm(@NonNull Context context, Alarm alarm, View snackBarAnchor) {
        if (!alarm.isActive()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            alarm.setActive(false);
            if (snackBarAnchor != null) {
                Snackbar.make(snackBarAnchor, context.getString(R.string.alarm_set_error), Snackbar.LENGTH_SHORT).show();
            }
            return;
        }

        DateTime nextAlarmRing = null; // used in Snackbar

        PendingIntent pendingIntent = getPendingIntent(context, alarm.getId());
        PendingIntent pendingIntentShow = getPendingIntentShow(context, alarm.getId());

        if (alarm.isRepeating()) {
            // get list of time to ring in milliseconds for each active day, and repeat weekly
            List<DateTime> timeToWeeklyRings = alarm.getTimeToWeeklyRings();

            for (DateTime time : timeToWeeklyRings) {
                Log.d(TAG, "Setting weekly repeat at " + time);

                if (nextAlarmRing == null || time.isBefore(nextAlarmRing)) {
                    nextAlarmRing = time;
                }

                // TODO Currently: fixed hidden delta for repeating alarms... change? Leave as it is? Disable repeating function?
                if (alarm.hasHiddenTime()) {
                    time = time.minusMinutes(alarm.getHiddenDelta());
                    Log.d(TAG, "Condition " + Condition.UNKNOWN_ALARM + "! Setting hidden repeating alarm for " + time);
                }
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, time.getMillis(), AlarmManager.INTERVAL_DAY * 7, pendingIntent);
            }
            if (nextAlarmRing != null) {
                logAlarmSet(alarm, nextAlarmRing);

                Log.d(TAG, "Setting next alarm to " + nextAlarmRing);
                AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(nextAlarmRing.getMillis(), pendingIntentShow);
                alarmManager.setAlarmClock(info, pendingIntent);
            }
        } else {
            AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(alarm.getTimeToNextRing().getMillis(), pendingIntentShow);
            alarmManager.setAlarmClock(info, pendingIntent);

            logAlarmSet(alarm, alarm.getTimeToNextRing());

            Log.d(TAG, "Setting alarm for " + alarm);

            if (alarm.hasHiddenTime()) {
                PendingIntent pendingIntentUnknown = getPendingIntent(context, Integer.MAX_VALUE - alarm.getId());
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarm.getHiddenTime().getMillis(), pendingIntentUnknown);

                Log.d(TAG, "Condition " + Condition.UNKNOWN_ALARM + "! Setting hidden alarm for " + alarm.getHiddenTime());
            }

        }

        if (!alarm.isRepeating()) {
            Period timeDiff = new Period(DateTime.now(), alarm.getTimeToNextRing());
            if (snackBarAnchor != null) {
                Snackbar.make(snackBarAnchor, "Alarm set for " + formatter.print(timeDiff) + "now.", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    public static void scheduleAlarmAtTime(Context context, DateTime timeToRing, int alarmId) {
        scheduleAlarmAtTime(context, timeToRing, alarmId, -1);
    }

    /**
     * Schedule alarm notification based on absolute time
     *
     * @param timeToRing time to next alarm
     * @param alarmId    ID of alarm to ring
     */
    public static void scheduleAlarmAtTime(Context context, DateTime timeToRing, int alarmId, int salivaId) {
        PendingIntent pendingIntent = getPendingIntent(context, alarmId, salivaId);
        PendingIntent pendingIntentShow = getPendingIntentShow(context, alarmId, salivaId);

        scheduleAlarmAtTime(context, timeToRing, alarmId, pendingIntent, pendingIntentShow);
    }

    /**
     * Schedule alarm notification based on absolute time
     *
     * @param timeToRing time to next alarm
     * @param alarmId    ID of alarm to ring
     */
    public static void scheduleAlarmAtTime(Context context, DateTime timeToRing, int alarmId, PendingIntent pendingIntent, PendingIntent pendingIntentShow) {
        Log.d(TAG, "Setting timed alarm " + alarmId + " at " + timeToRing);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(timeToRing.getMillis(), pendingIntentShow);
            alarmManager.setAlarmClock(info, pendingIntent);
        }
    }

    public static void cancelAlarm(Context context, Alarm alarm) {
        cancelAlarm(context, alarm, null);
    }

    /**
     * Cancel alarm notification AlarmManager
     *
     * @param alarm Alarm to cancel
     */
    public static void cancelAlarm(Context context, Alarm alarm, View snackBarAnchor) {
        // Get PendingIntent to AlarmReceiver Broadcast channel
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarm.getId(), intent, PendingIntent.FLAG_NO_CREATE);
        String subjectId = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREF_SUBJECT_ID, null);

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

            if (subjectId != null && SubjectMap.getConditionForSubject(subjectId) == Condition.UNKNOWN_ALARM) {
                PendingIntent pendingIntentUnknown = getPendingIntent(context, Integer.MAX_VALUE - alarm.getId());
                if (pendingIntentUnknown != null) {
                    Log.d(TAG, "Cancelling unknown alarm for " + alarm.getId());
                    alarmManager.cancel(pendingIntentUnknown);
                }
            }
        }

        if (snackBarAnchor != null) {
            // Show snackbar to notify user
            Snackbar.make(snackBarAnchor, context.getString(R.string.alarm_cancelled), Snackbar.LENGTH_SHORT).show();
        }
    }


    public static void cancelAlarmAtTime(Context context, int alarmId) {
        // Get PendingIntent to AlarmReceiver Broadcast channel
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarmId, intent, PendingIntent.FLAG_NO_CREATE);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Log.d(TAG, "Cancelling alarm " + alarmId);
        Log.d(TAG, "Cancelling alarm " + pendingIntent);

        // PendingIntent may be null if the alarm hasn't been set
        if (alarmManager != null && pendingIntent != null) {

            alarmManager.cancel(pendingIntent);
        }
    }

    private static PendingIntent getPendingIntent(Context context, int alarmId) {
        return getPendingIntent(context, alarmId, -1);
    }

    private static PendingIntent getPendingIntent(Context context, int alarmId, int salivaId) {
        // Get PendingIntent to AlarmReceiver Broadcast
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(Constants.EXTRA_ALARM_ID, alarmId);
        if (salivaId != -1) {
            intent.putExtra(Constants.EXTRA_SALIVA_ID, salivaId);
        }
        return PendingIntent.getBroadcast(context, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent getPendingIntentShow(Context context, int alarmId) {
        return getPendingIntentShow(context, alarmId, -1);
    }

    private static PendingIntent getPendingIntentShow(Context context, int alarmId, int salivaId) {
        Intent intentShow = new Intent(context, MainActivity.class);
        intentShow.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intentShow.putExtra(Constants.EXTRA_ALARM_ID, alarmId);
        if (salivaId != -1) {
            intentShow.putExtra(Constants.EXTRA_SALIVA_ID, salivaId);
        }

        return PendingIntent.getActivity(context, Constants.REQUEST_CODE_ALARM_ACTIVITY, intentShow, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static void logAlarmSet(Alarm alarm, DateTime nextRing) {
        try {
            // create Json object and log information
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarm.getId());
            json.put(Constants.LOGGER_EXTRA_ALARM_TIMESTAMP, nextRing.getMillis());
            json.put(Constants.LOGGER_EXTRA_ALARM_IS_REPEATING, alarm.isRepeating());
            if (alarm.isRepeating()) {
                json.put(Constants.LOGGER_EXTRA_ALARM_REPEATING_DAYS, new JSONArray(alarm.getActiveDays()));
            }
            json.put(Constants.LOGGER_EXTRA_ALARM_IS_HIDDEN, alarm.hasHiddenTime());
            if (alarm.hasHiddenTime()) {
                json.put(Constants.LOGGER_EXTRA_ALARM_HIDDEN_TIMESTAMP, alarm.getHiddenTime().getMillis());
            }

            LoggerUtil.log(Constants.LOGGER_ACTION_ALARM_SET, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void killAll(Application application) {
        AlarmRepository repo = AlarmRepository.getInstance(application);

        if (repo.getAllAlarms() == null || repo.getAllAlarms().getValue() == null) {
            return;
        }

        // cancel everything that's there: all alarms, all hidden alarms, all timer alarms...
        for (Alarm alarm : repo.getAllAlarms().getValue()) {
            alarm.setActive(false);

            TimerHandler.cancelTimer(application, alarm.getId());
            killAllOngoingAlarms(application, alarm.getId());
            repo.update(alarm);
        }
    }

    private static void killAllOngoingAlarms(Context context, int alarmId) {
        for (int ignored : Constants.SALIVA_TIMES) {
            cancelAlarmAtTime(context, alarmId);
            alarmId += Constants.ALARM_OFFSET;
        }
    }
}
