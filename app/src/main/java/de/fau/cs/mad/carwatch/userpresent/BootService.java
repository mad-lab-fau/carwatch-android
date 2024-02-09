package de.fau.cs.mad.carwatch.userpresent;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.lifecycle.Observer;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.json.JSONObject;

import java.util.List;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.util.AlarmRepository;

public class BootService extends JobIntentService implements Observer<List<Alarm>> {

    private static final String TAG = BootService.class.getSimpleName();
    private static final int JOB_ID = 0x34;

    private AlarmRepository repository;

    @Override
    public void onCreate() {
        super.onCreate();
        repository = AlarmRepository.getInstance(getApplication());
        repository.getAlarms().observeForever(this);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        LoggerUtil.log(Constants.LOGGER_ACTION_PHONE_BOOT_COMPLETE, new JSONObject());
        Interval night = new Interval(Constants.EVENING_TIMES[0], Constants.EVENING_TIMES[1]);

        if (night.contains(DateTime.now()) && !UserPresentService.serviceRunning)
            UserPresentService.startService(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (repository != null) {
            repository.getAlarms().removeObserver(this);
        }
    }

    public static void enqueueWork(Context context) {
        enqueueWork(context, BootService.class, JOB_ID, new Intent());
    }

    @Override
    public void onChanged(List<Alarm> alarms) {
        // reschedule all active alarms
        if (alarms == null)
            return;

        for (Alarm alarm : alarms) {
            if (!alarm.isActive())
                continue;

            if (alarm.getId() == Constants.EXTRA_ALARM_ID_INITIAL) {
                AlarmHandler.scheduleWakeUpAlarm(this, alarm);
            } else {
                AlarmHandler.scheduleSalivaAlarm(this, alarm, null);
            }
        }
    }
}
