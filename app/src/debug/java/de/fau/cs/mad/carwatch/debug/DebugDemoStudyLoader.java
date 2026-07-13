package de.fau.cs.mad.carwatch.debug;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.collection.ArraySet;
import androidx.preference.PreferenceManager;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import java.util.List;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.db.AlarmDao;
import de.fau.cs.mad.carwatch.db.AlarmDatabase;

/** Creates a deterministic in-progress study for manual layout testing in debug builds. */
@SuppressWarnings("unused") // Loaded reflectively by DemoStudyLoader in debug builds.
public final class DebugDemoStudyLoader {
    private static final String TAG = DebugDemoStudyLoader.class.getSimpleName();

    private DebugDemoStudyLoader() {}

    public static void load(Activity activity, Runnable onLoaded, Runnable onError) {
        DateTime todayAtEight = LocalTime.parse("08:00").toDateTimeToday();
        DateTime nextWakeup = todayAtEight.isAfterNow() ? todayAtEight : todayAtEight.plusDays(1);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        preferences.edit()
                .putInt(Constants.PREF_CURRENT_SLIDE_SHOW_SLIDE, Constants.SLIDESHOW_FINISHED_SLIDE_ID)
                .putBoolean(Constants.PREF_FIRST_RUN_QR, false)
                .putBoolean(Constants.PREF_PARTICIPANT_ID_WAS_SET, true)
                .putString(Constants.PREF_STUDY_NAME, "Demo Study")
                .putString(Constants.PREF_PARTICIPANT_ID, "DEMO")
                .putInt(Constants.PREF_NUM_PARTICIPANTS, 1)
                .putInt(Constants.PREF_NUM_DAYS, 3)
                .putInt(Constants.PREF_DAY_COUNTER, 2)
                .putInt(Constants.PREF_ID_ONGOING_ALARM, Constants.EXTRA_ALARM_ID_INITIAL)
                .putInt(Constants.PREF_CURRENT_ALARM_ID, 3)
                .putString(Constants.PREF_SALIVA_DISTANCES, "0,15,15")
                .putString(Constants.PREF_SALIVA_TIMES, "")
                .putInt(Constants.PREF_TOTAL_NUM_SAMPLES, 4)
                .putBoolean(Constants.PREF_HAS_EVENING, true)
                .putInt(Constants.PREF_EVENING_SALIVA_ID, 3)
                .putString(Constants.PREF_START_SAMPLE, "S1")
                .putBoolean(Constants.PREF_CHECK_DUPLICATES, true)
                .putStringSet(Constants.PREF_SCANNED_BARCODES, new ArraySet<>())
                .putLong(Constants.PREF_LAST_WAKE_UP_ALARM_RING_TIME, todayAtEight.getMillis())
                .putLong(Constants.PREF_WAKEUP_SAMPLE_TAKEN_TIME, todayAtEight.getMillis())
                .putBoolean(Constants.PREF_WAKEUP_SCAN_PENDING, false)
                .putBoolean(Constants.PREF_STUDY_DAY_MANUALLY_ADVANCED, false)
                .putBoolean(Constants.PREF_TIMER_NOTIFICATION_IS_SHOWN, false)
                .putInt(Constants.PREF_EVENING_REMINDER_TIME_MINUTES, 21 * 60)
                .remove(Constants.PREF_EVENING_TAKEN)
                .remove(Constants.PREF_WAKEUP_SCAN_PENDING_TIME)
                .remove(Constants.PREF_WAKEUP_ALERT_TYPE)
                .remove(Constants.PREF_WAKEUP_DELAYED_SAMPLE_MINUTES)
                .apply();

        Thread databaseThread = new Thread(() -> {
            try {
                AlarmDao dao = AlarmDatabase.getInstance(activity.getApplicationContext()).alarmModel();
                List<Alarm> existingAlarms = dao.getAll();
                for (Alarm existingAlarm : existingAlarms) {
                    dao.delete(existingAlarm);
                }

                Alarm wakeupAlarm = new Alarm(
                        nextWakeup, true, false, Constants.EXTRA_ALARM_ID_INITIAL,
                        Constants.EXTRA_SALIVA_ID_INITIAL, true);
                Alarm firstSampleAlarm = new Alarm(
                        todayAtEight.plusMinutes(15), true, false, 1, 1, false);
                Alarm secondSampleAlarm = new Alarm(
                        todayAtEight.plusMinutes(30), true, false, 2, 2, false);
                Alarm eveningAlarm = new Alarm(
                        LocalTime.parse("21:00").toDateTimeToday(), true, true,
                        Constants.EXTRA_ALARM_ID_EVENING, 3, false);

                dao.insertOrReplaceAlarm(wakeupAlarm);
                dao.insertOrReplaceAlarm(firstSampleAlarm);
                dao.insertOrReplaceAlarm(secondSampleAlarm);
                dao.insertOrReplaceAlarm(eveningAlarm);

                AlarmHandler.scheduleWakeUpAlarm(activity, wakeupAlarm);
                scheduleSampleAlarmIfFuture(activity, firstSampleAlarm);
                scheduleSampleAlarmIfFuture(activity, secondSampleAlarm);
                scheduleSampleAlarmIfFuture(activity, eveningAlarm);
                activity.runOnUiThread(onLoaded);
            } catch (RuntimeException e) {
                Log.e(TAG, "Could not create demo study", e);
                activity.runOnUiThread(onError);
            }
        }, "demo-study-loader");
        databaseThread.start();
    }

    private static void scheduleSampleAlarmIfFuture(Activity activity, Alarm alarm) {
        if (alarm.getTime().isAfterNow()) {
            AlarmHandler.scheduleSalivaAlarm(activity, alarm, null);
        }
    }
}
