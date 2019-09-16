package de.fau.cs.mad.carwatch.alarmmanager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

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


/**
 * Class to control alarm scheduling/cancelling
 */
public class AlarmHandler {

    private final String TAG = AlarmHandler.class.getSimpleName();

    private Context context;
    private View snackBarAnchor;

    private PeriodFormatter formatter = new PeriodFormatterBuilder()
            .appendHours()
            .appendSuffix(" hour", " hours")
            .appendSeparatorIfFieldsBefore(" from ")
            .appendMinutes()
            .appendSuffix(" minute", " minutes")
            .appendSeparatorIfFieldsBefore(" from ")
            .toFormatter();

    public AlarmHandler(Context context, View snackBarAnchor) {
        this.context = context;
        this.snackBarAnchor = snackBarAnchor;
    }

    /**
     * Schedule alarm TimeShiftReceiver an hour before alarm's time using AlarmManager
     *
     * @param alarm Alarm to schedule
     */
    public void scheduleAlarm(Alarm alarm) {
        if (!alarm.isActive()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            alarm.setActive(false);
            Snackbar.make(snackBarAnchor,
                    context.getString(R.string.alarm_set_error),
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        DateTime nextAlarmRing = null; // used in Snackbar

        PendingIntent pendingIntent = getPendingIntent(alarm.getId());
        PendingIntent pendingIntentShow = getPendingIntentShow(alarm.getId());

        if (alarm.isRepeating()) {
            // get list of time to ring in milliseconds for each active day, and repeat weekly
            List<DateTime> timeToWeeklyRings = alarm.getTimeToWeeklyRings();

            for (DateTime time : timeToWeeklyRings) {
                Log.d(TAG, "Setting weekly repeat at " + time);

                if (time.isBefore(nextAlarmRing) || nextAlarmRing == null) {
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
                try {
                    // create Json object and log information
                    JSONObject json = new JSONObject();
                    json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarm.getId());
                    json.put(Constants.LOGGER_EXTRA_ALARM_TIMESTAMP, nextAlarmRing.getMillis());
                    json.put(Constants.LOGGER_EXTRA_ALARM_IS_REPEATING, alarm.isRepeating());
                    json.put(Constants.LOGGER_EXTRA_ALARM_REPEATING_DAYS, new JSONArray(alarm.getActiveDays()));
                    json.put(Constants.LOGGER_EXTRA_ALARM_IS_HIDDEN, alarm.hasHiddenTime());
                    if (alarm.hasHiddenTime()) {
                        json.put(Constants.LOGGER_EXTRA_ALARM_HIDDEN_TIMESTAMP, alarm.getHiddenTime().getMillis());
                    }

                    LoggerUtil.log(Constants.LOGGER_ACTION_ALARM_SET, json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "Setting next alarm to " + nextAlarmRing);
                AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(nextAlarmRing.getMillis(), pendingIntentShow);
                alarmManager.setAlarmClock(info, pendingIntent);
            }
        } else {
            AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(alarm.getTimeToNextRing().getMillis(), pendingIntentShow);
            alarmManager.setAlarmClock(info, pendingIntent);

            try {
                // create Json object and log information
                JSONObject json = new JSONObject();
                json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarm.getId());
                json.put(Constants.LOGGER_EXTRA_ALARM_TIMESTAMP, alarm.getTimeToNextRing().getMillis());
                json.put(Constants.LOGGER_EXTRA_ALARM_IS_REPEATING, alarm.isRepeating());
                json.put(Constants.LOGGER_EXTRA_ALARM_IS_HIDDEN, alarm.hasHiddenTime());
                if (alarm.hasHiddenTime()) {
                    json.put(Constants.LOGGER_EXTRA_ALARM_HIDDEN_TIMESTAMP, alarm.getHiddenTime().getMillis());
                }
                LoggerUtil.log(Constants.LOGGER_ACTION_ALARM_SET, json);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d(TAG, "Setting alarm for " + alarm);

            if (alarm.hasHiddenTime()) {
                PendingIntent pendingIntentUnknown = getPendingIntent(Integer.MAX_VALUE - alarm.getId());
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarm.getHiddenTime().getMillis(), pendingIntentUnknown);

                Log.d(TAG, "Condition " + Condition.UNKNOWN_ALARM + "! Setting hidden alarm for " + alarm.getHiddenTime());
            }

        }

        if (!alarm.isRepeating()) {
            Period timeDiff = new Period(DateTime.now(), alarm.getTimeToNextRing());
            Snackbar.make(snackBarAnchor, "Alarm set for " + formatter.print(timeDiff) + "now.", Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Schedule alarm notification based on absolute time
     *
     * @param timeToRing time to next alarm in milliseconds
     * @param alarmId    ID of alarm to ring
     */
    public void scheduleAlarmAtTime(DateTime timeToRing, int alarmId) {
        PendingIntent pendingIntent = getPendingIntent(alarmId);
        PendingIntent pendingIntentShow = getPendingIntentShow(alarmId);

        Log.d(TAG, "Setting timed alarm " + alarmId + " at " + timeToRing);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(timeToRing.getMillis(), pendingIntentShow);
            alarmManager.setAlarmClock(info, pendingIntent);
        }
    }

    /**
     * Cancel alarm notification and TimeShiftIntent using AlarmManager
     *
     * @param alarm Alarm to cancel
     */
    public void cancelAlarm(Alarm alarm) {

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
                PendingIntent pendingIntentUnknown = getPendingIntent(Integer.MAX_VALUE - alarm.getId());
                if (pendingIntentUnknown != null) {
                    Log.d(TAG, "Cancelling unknown alarm for " + alarm.getId());
                    alarmManager.cancel(pendingIntentUnknown);
                }
            }
        }

        // Show snackbar to notify user
        Snackbar.make(snackBarAnchor, context.getString(R.string.alarm_cancelled), Snackbar.LENGTH_SHORT).show();
    }


    private PendingIntent getPendingIntent(int alarmId) {
        // Get PendingIntent to AlarmReceiver Broadcast
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(Constants.EXTRA_ID, alarmId);

        return PendingIntent.getBroadcast(context, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getPendingIntentShow(int alarmId) {
        Intent intentShow = new Intent(context, MainActivity.class);
        intentShow.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intentShow.putExtra(Constants.EXTRA_ID, alarmId);

        return PendingIntent.getActivity(context, Constants.REQUEST_CODE_ALARM_ACTIVITY, intentShow, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
