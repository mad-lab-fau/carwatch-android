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
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import de.fau.cs.mad.carwatch.ui.CarwatchSnackbar;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.MainActivity;
import de.fau.cs.mad.carwatch.userpresent.BootCompletedReceiver;
import de.fau.cs.mad.carwatch.userpresent.UserPresentService;
import de.fau.cs.mad.carwatch.util.AlarmRepository;
import de.fau.cs.mad.carwatch.util.Utils;


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
                CarwatchSnackbar.show(snackBarAnchor, context.getString(R.string.alarm_set_error), CarwatchSnackbar.LENGTH_SHORT);
            }
            return;
        }

        PendingIntent showIntent = getPendingIntentShow(context, alarm);
        PendingIntent operation = getPendingIntent(context, alarm);

        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(alarm.getTimeToNextRing().getMillis(), showIntent);
        alarmManager.setAlarmClock(info, operation);

        setBootCompletedReceiverEnabledSetting(context, true);

        logAlarmSet(alarm, alarm.getTimeToNextRing());
        showAlarmSetMessage(context, snackBarAnchor, alarm.getTimeToNextRing());
    }

    /**
     * Re-arms the wakeup alarm for the calendar day after a wakeup was recorded.
     * This deliberately touches only the initial alarm: today's saliva alarms keep
     * their original times when the next wakeup time is created or edited.
     */
    public static void scheduleNextDayWakeUpAlarm(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int currentDay = preferences.getInt(Constants.PREF_DAY_COUNTER, 0);
        int numberOfDays = preferences.getInt(Constants.PREF_NUM_DAYS, 0);
        if (currentDay <= 0 || currentDay >= numberOfDays) {
            return;
        }

        AlarmRepository repository = AlarmRepository.getInstance(context);
        try {
            Alarm alarm = repository.getAlarmById(Constants.EXTRA_ALARM_ID_INITIAL);
            if (alarm == null || alarm.getTime() == null) {
                return;
            }

            LocalTime wakeupTime = alarm.getTime().toLocalTime();
            alarm.setTime(wakeupTime.toDateTimeToday().plusDays(1));
            alarm.setActive(true);
            alarm.setWasSampleTaken(false);
            String distances = preferences.getString(Constants.PREF_SALIVA_DISTANCES, "");
            alarm.setSalivaId(requiresImmediateWakeupSample(distances)
                    ? Constants.EXTRA_SALIVA_ID_INITIAL
                    : -1);
            repository.updateAndWait(alarm);
            scheduleWakeUpAlarm(context, alarm);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Could not schedule the next-day wakeup alarm", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static boolean hasUnfinishedCurrentStudyDay(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (sp.getBoolean(Constants.PREF_CURRENT_STUDY_DAY_FINISHED, false)) {
            return false;
        }
        int dayCounter = sp.getInt(Constants.PREF_DAY_COUNTER, 0);
        return dayCounter > 0
                && !sp.getBoolean(Constants.PREF_STUDY_DAY_MANUALLY_ADVANCED, false)
                && hasRemainingSamplesForDay(sp, dayCounter);
    }

    /**
     * deletes and re-schedules all saliva alarms with relative and fixed times except for the wake-up alarm
     *
     * @param context Context to use
     */
    public static void rescheduleSalivaAlarms(Context context) {
        deleteSalivaAlarms(context);
        scheduleSalivaAlarms(context);
    }

    public static void resetStudyConfiguration(Context context) {
        AlarmRepository repository = AlarmRepository.getInstance(context);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
        AlarmSoundControl.getInstance().stopAlarmSound();
        UserPresentService.stopService(context);

        try {
            List<Alarm> alarms = repository.getAll();

            if (alarms != null) {
                boolean initialAlarmExists = false;
                for (Alarm alarm : alarms) {
                    cancelAlarmAtTime(context, alarm.getId());
                    TimerHandler.cancelTimer(context, alarm.getId());

                    if (alarm.getId() == Constants.EXTRA_ALARM_ID_INITIAL) {
                        initialAlarmExists = true;
                        alarm.setTime(Constants.DEFAULT_ALARM_TIME.toDateTimeToday());
                        alarm.setActive(false);
                        alarm.setIsFixed(false);
                        alarm.setSalivaId(Constants.EXTRA_SALIVA_ID_INITIAL);
                        alarm.setWasSampleTaken(false);
                        repository.update(alarm);
                    } else {
                        repository.delete(alarm);
                    }
                }

                if (!initialAlarmExists) {
                    repository.insert(new Alarm());
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Could not reset study configuration: failed to get alarms from database", e);
        }

        cancelAlarmAtTime(context, Constants.EXTRA_ALARM_ID_INITIAL);
        cancelAlarmAtTime(context, Constants.EXTRA_ALARM_ID_EVENING);
        TimerHandler.cancelTimer(context, Constants.EXTRA_ALARM_ID_INITIAL);
        TimerHandler.cancelTimer(context, Constants.EXTRA_ALARM_ID_EVENING);

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .clear()
                .putInt(Constants.PREF_CURRENT_SLIDE_SHOW_SLIDE, Constants.INITIAL_SLIDE_SHOW_SLIDE)
                .putInt(Constants.PREF_CURRENT_ALARM_ID, Constants.EXTRA_ALARM_ID_INITIAL + 1)
                .putInt(Constants.PREF_ID_ONGOING_ALARM, Constants.EXTRA_ALARM_ID_INITIAL)
                .putInt(Constants.PREF_DAY_COUNTER, 0)
                .putBoolean(Constants.PREF_FIRST_RUN_QR, true)
                .putBoolean(Constants.PREF_PARTICIPANT_ID_WAS_SET, false)
                .putBoolean(Constants.PREF_TIMER_NOTIFICATION_IS_SHOWN, false)
                .putBoolean(Constants.PREF_REREGISTRATION_MODE, true)
                .apply();

        setBootCompletedReceiverEnabledSetting(context, false);
    }

    public static boolean canFinishCurrentStudyDay(Context context) {
        return hasUnfinishedCurrentStudyDay(context);
    }

    public static boolean finishCurrentStudyDay(Context context) {
        if (!canFinishCurrentStudyDay(context)) {
            return false;
        }

        AlarmRepository repository = AlarmRepository.getInstance(context);
        try {
            List<Alarm> alarms = repository.getAll();
            if (alarms != null) {
                for (Alarm alarm : alarms) {
                    TimerHandler.cancelTimer(context, alarm.getId());
                    if (alarm.getId() == Constants.EXTRA_ALARM_ID_INITIAL) {
                        continue;
                    }

                    cancelAlarmAtTime(context, alarm.getId());
                    alarm.setActive(false);
                    repository.delete(alarm);
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Could not finish current study day: failed to get alarms from database", e);
            return false;
        }

        cancelAlarmAtTime(context, Constants.EXTRA_ALARM_ID_EVENING);
        TimerHandler.cancelTimer(context, Constants.EXTRA_ALARM_ID_EVENING);
        TimerHandler.finishDay(context);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int dayCounter = sp.getInt(Constants.PREF_DAY_COUNTER, 0);
        int numDays = sp.getInt(Constants.PREF_NUM_DAYS, 0);
        boolean hasNextDay = dayCounter < numDays;
        sp.edit()
                .putInt(Constants.PREF_DAY_COUNTER, dayCounter + 1)
                .putBoolean(Constants.PREF_STUDY_DAY_MANUALLY_ADVANCED, hasNextDay)
                .putInt(Constants.PREF_ID_ONGOING_ALARM, Constants.EXTRA_ALARM_ID_INITIAL)
                .putBoolean(Constants.PREF_TIMER_NOTIFICATION_IS_SHOWN, false)
                .remove(Constants.PREF_WAKEUP_ALERT_TYPE)
                .remove(Constants.PREF_WAKEUP_DELAYED_SAMPLE_MINUTES)
                .putBoolean(Constants.PREF_SHOULD_FINISH_PREVIOUS_DAY_ON_WAKEUP, false)
                .apply();

        return true;
    }

    private static boolean hasRemainingSamplesForDay(SharedPreferences sp, int dayCounter) {
        int totalNumSamples = sp.getInt(Constants.PREF_TOTAL_NUM_SAMPLES, 0);
        if (totalNumSamples <= 0) {
            return false;
        }
        if (!sp.getBoolean(Constants.PREF_CHECK_DUPLICATES, false)) {
            return true;
        }

        Set<String> scannedBarcodes = sp.getStringSet(Constants.PREF_SCANNED_BARCODES, Collections.emptySet());
        int scannedSamplesForDay = 0;
        for (String barcode : scannedBarcodes) {
            if (barcode == null || barcode.length() < 7) {
                continue;
            }

            try {
                int scannedDay = Integer.parseInt(barcode.substring(3, 5));
                if (scannedDay == dayCounter) {
                    scannedSamplesForDay++;
                }
            } catch (NumberFormatException e) {
                Log.d(TAG, "Could not parse scanned barcode day from " + barcode);
            }
        }

        return scannedSamplesForDay < totalNumSamples;
    }

    public static boolean isStudyOngoing(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (!sp.contains(Constants.PREF_NUM_DAYS)) {
            return false;
        }

        int numDays = sp.getInt(Constants.PREF_NUM_DAYS, 0);
        if (numDays <= 0) {
            return false;
        }

        int dayCounter = sp.getInt(Constants.PREF_DAY_COUNTER, 0);
        if (dayCounter < numDays) {
            return true;
        }
        if (dayCounter > numDays) {
            return false;
        }

        int totalNumSamples = sp.getInt(Constants.PREF_TOTAL_NUM_SAMPLES, 0);
        if (totalNumSamples <= 0) {
            return false;
        }

        Set<String> scannedBarcodes = sp.getStringSet(Constants.PREF_SCANNED_BARCODES, Collections.emptySet());
        return scannedBarcodes.size() < totalNumSamples * numDays;
    }

    public static boolean requiresImmediateWakeupSample(String timeDistancesString) {
        String[] timeDistances = timeDistancesString.split(",");
        for (String distanceString : timeDistances) {
            if (distanceString.isEmpty()) {
                continue;
            }

            return Integer.parseInt(distanceString) == 0;
        }

        return false;
    }

    public static int countMorningSamples(String timeDistancesString) {
        if (timeDistancesString.isEmpty()) {
            return 0;
        }

        int count = requiresImmediateWakeupSample(timeDistancesString) ? 1 : 0;
        for (String distanceString : timeDistancesString.split(",")) {
            if (distanceString.isEmpty() || distanceString.equals("0")) {
                continue;
            }
            count++;
        }

        return count;
    }

    public static void showMessageSalivaAlarmsScheduled(Context context, View anchor) {
        if (anchor == null || context == null)
            return;

        String message = context.getString(R.string.saliva_alarms_set);
        CarwatchSnackbar.show(anchor, message, CarwatchSnackbar.LENGTH_LONG);
    }

    public static void showAlarmSetMessage(Context context, View snackBarAnchor, DateTime time) {
        if (snackBarAnchor == null)
            return;

        String timeDiffString = createTimeDiffString(time);
        CarwatchSnackbar.show(snackBarAnchor, context.getString(R.string.alarm_set, timeDiffString), CarwatchSnackbar.LENGTH_SHORT);
    }

    public static void scheduleSalivaAlarm(Context context, Alarm alarm, View snackbarAnchor) {
        if (!alarm.isActive())
            return;

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

        setBootCompletedReceiverEnabledSetting(context, true);

        try {
            // create Json object and log information
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarm.getId());
            json.put(Constants.LOGGER_EXTRA_ALARM_TIMESTAMP, alarmTime.getMillis());
            json.put(Constants.LOGGER_TRANSLATED_TIMESTAMP, Utils.translateTimestamp(alarmTime.getMillis()));
            LoggerUtil.log(Constants.LOGGER_ACTION_TIMER_SET, json);
        } catch (JSONException e) {
            Log.e(TAG, "Could not log timer set", e);
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

        int pendingFlags = PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarm.getId(), intent, pendingFlags);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null || pendingIntent == null)
            // PendingIntent may be null if the alarm hasn't been set
            return;

        try {
            // create Json object and log information
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarm.getId());
            LoggerUtil.log(Constants.LOGGER_ACTION_ALARM_CANCEL, json);
        } catch (JSONException e) {
            Log.e(TAG, "Could not log alarm cancel", e);
        }

        alarmManager.cancel(pendingIntent);

        AlarmRepository repository = AlarmRepository.getInstance((Application) context.getApplicationContext());
        List<Alarm> alarms = repository.getAlarms().getValue();
        boolean enabledAlarmRemains = false;

        if (alarms == null)
            return;

        for (Alarm a : alarms) {
            if (a.isActive()) {
                enabledAlarmRemains = true;
                break;
            }
        }

        if (!enabledAlarmRemains)
            setBootCompletedReceiverEnabledSetting(context, false);

        if (snackBarAnchor != null) {
            // Show snackbar to notify user
            CarwatchSnackbar.show(snackBarAnchor, context.getString(R.string.alarm_cancelled), CarwatchSnackbar.LENGTH_SHORT);
        }
    }

    private static void deleteSalivaAlarms(Context context) {
        AlarmRepository repository = AlarmRepository.getInstance(context);

        try {
            List<Alarm> alarms = repository.getAll();

            if (alarms == null)
                return;

            for (Alarm alarm : alarms) {
                if (alarm.getId() == Constants.EXTRA_ALARM_ID_INITIAL)
                    continue;
                alarm.setActive(false);
                cancelAlarm(context, alarm, null);
                repository.delete(alarm);
                Log.d(TAG, "Deleted saliva alarm " + alarm.getId());
            }

            // reset alarm id counter
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putInt(Constants.PREF_CURRENT_ALARM_ID, Constants.EXTRA_ALARM_ID_INITIAL + 1).apply();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Could not delete yesterdays saliva alarms: failed to get alarms from database", e);
        }
    }

    private static void scheduleSalivaAlarms(Context context) {
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
        if (requiresImmediateWakeupSample(timeDistancesString))
            // if first sample request has no offset, it was already scheduled with the first alarm
            salivaId++;

        DateTime now = DateTime.now();
        for (int i = 0; i < alarmTimes.size(); i++) {
            DateTime alarmTime = alarmTimes.get(i);
            boolean fixedAlarmAlreadyDue = isFixed.get(i) && alarmTime.isBefore(now);
            Alarm alarm = new Alarm(alarmTime, !fixedAlarmAlreadyDue, isFixed.get(i), id++, salivaId++, false);
            repo.insert(alarm);
            AlarmHandler.scheduleSalivaAlarm(context, alarm, null);
        }
        sp.edit().putInt(Constants.PREF_CURRENT_ALARM_ID, id).apply();
    }

    private static void cancelAlarmAtTime(Context context, int alarmId) {
        // Get PendingIntent to AlarmReceiver Broadcast channel
        Intent intent = new Intent(context, AlarmReceiver.class);

        int pendingFlags = PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE;
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
        if (alarm.getId() == Constants.EXTRA_ALARM_ID_INITIAL) {
            intent.putExtra(Constants.EXTRA_TARGET_NAV_ELEMENT, R.id.navigation_wakeup);
        }

        return PendingIntent.getActivity(
                context,
                Constants.REQUEST_CODE_ALARM_ACTIVITY,
                intent,
                getPendingIntentFlags());
    }

    private static int getPendingIntentFlags() {
        return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    }

    private static void setBootCompletedReceiverEnabledSetting(Context context, boolean setEnabled) {
        int flag = setEnabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        ComponentName receiver = new ComponentName(context, BootCompletedReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver, flag, PackageManager.DONT_KILL_APP);
    }

    private static String createTimeDiffString(DateTime nextRingTime) {
        Period timeDiff = new Period(DateTime.now(), nextRingTime);
        String timeDiffString = formatter.print(timeDiff);

        return switch (Locale.getDefault().getLanguage()) {
            case "de" -> (timeDiffString.isEmpty() ? "jetzt" : "in " + timeDiffString);
            case "fr" -> timeDiffString.isEmpty() ? "" : "pour " + timeDiffString;
            default -> timeDiffString.isEmpty() ? "" : timeDiffString + " from ";
        };
    }

    private static void logAlarmSet(Alarm alarm, DateTime nextRing) {
        try {
            // create Json object and log information
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_EXTRA_ALARM_ID, alarm.getId());
            json.put(Constants.LOGGER_EXTRA_ALARM_TIMESTAMP, nextRing.getMillis());
            json.put(Constants.LOGGER_TRANSLATED_TIMESTAMP, Utils.translateTimestamp(nextRing.getMillis()));

            LoggerUtil.log(Constants.LOGGER_ACTION_ALARM_SET, json);
        } catch (JSONException e) {
            Log.e(TAG, "Could not log alarm set", e);
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
