package de.fau.cs.mad.carwatch.ui.alarm;

import android.app.Application;

import java.util.List;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.util.AlarmRepository;

public class AlarmViewModel extends AndroidViewModel {

    private static final String TAG = AlarmViewModel.class.getSimpleName();

    private final AlarmRepository repository;
    private final LiveData<Alarm> alarm;
    private final LiveData<List<Alarm>> fixedAlarms;


    public AlarmViewModel(Application application) {
        super(application);
        repository = AlarmRepository.getInstance(application);
        alarm = repository.getAlarm();
        fixedAlarms = repository.getFixedAlarms();
    }

    // List is wrapped in LiveData in order to be observed and updated efficiently
    public LiveData<Alarm> getAlarm() {
        return alarm;
    }

    public LiveData<List<Alarm>> getFixedAlarms() {
        return fixedAlarms;
    }

    public void insert(Alarm alarm) {
        repository.insert(alarm);
    }

    public void update(Alarm alarm) {
        repository.update(alarm);
    }

}