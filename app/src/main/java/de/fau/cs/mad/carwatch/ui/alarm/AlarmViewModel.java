package de.fau.cs.mad.carwatch.ui.alarm;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.arch.core.util.Function;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import org.joda.time.DateTime;

import de.fau.cs.mad.carwatch.Constants;

public class AlarmViewModel extends AndroidViewModel {

    private static final String TAG = AlarmViewModel.class.getSimpleName();

    private SharedPreferences sharedPreferences;

    private MutableLiveData<Boolean> alarmEnabled = new MutableLiveData<>();
    private MutableLiveData<DateTime> alarm = new MutableLiveData<>();

    private LiveData<String> alarmString = Transformations.map(alarm, new Function<DateTime, String>() {
        @Override
        public String apply(DateTime time) {
            return time.toString("HH:mm");
        }
    });


    public AlarmViewModel(Application application) {
        super(application);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext());
        DateTime time = new DateTime(sharedPreferences.getLong(Constants.KEY_ALARM_TIME, Constants.DEFAULT_ALARM_TIME.toDateTimeToday().getMillis()));

        setAlarm(time);
        setAlarmEnabled(sharedPreferences.getBoolean(Constants.KEY_ALARM_ENABLED, false));
    }

    public void setAlarm(DateTime time) {
        if (time.isBefore(DateTime.now())) {
            time = time.plusDays(1);
        }

        if (getAlarm().getValue() != null && getAlarm().getValue().equals(time)) {
            return;
        }

        alarm.setValue(time);
        sharedPreferences.edit().putLong(Constants.KEY_ALARM_TIME, time.getMillis()).apply();
    }

    public LiveData<DateTime> getAlarm() {
        return alarm;
    }

    public LiveData<String> getAlarmString() {
        return alarmString;
    }


    public LiveData<Boolean> getAlarmEnabled() {
        return alarmEnabled;
    }

    public void setAlarmEnabled(boolean enable) {
        if (getAlarm().getValue() != null) {
            setAlarm(getAlarm().getValue());
        }
        alarmEnabled.setValue(enable);
        sharedPreferences.edit().putBoolean(Constants.KEY_ALARM_ENABLED, enable).apply();
    }
}