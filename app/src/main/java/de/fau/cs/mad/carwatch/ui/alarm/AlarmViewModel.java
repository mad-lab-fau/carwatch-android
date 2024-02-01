package de.fau.cs.mad.carwatch.ui.alarm;

import android.app.Application;

import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.util.AlarmRepository;

public class AlarmViewModel extends AndroidViewModel {

    private static final String TAG = AlarmViewModel.class.getSimpleName();
    private final AlarmRepository repository;
    private final LiveData<List<Alarm>> alarms;


    public AlarmViewModel(Application application) {
        super(application);
        repository = AlarmRepository.getInstance(application);
        alarms = repository.getAlarms();
    }

    // List is wrapped in LiveData in order to be observed and updated efficiently
    public LiveData<Alarm> getAlarmLiveData(int id) {
        return repository.getAlarmLiveData(id);
    }
    
    public Alarm getAlarm(int id) throws ExecutionException, InterruptedException {
        return repository.getAlarmById(id);
    }

    public LiveData<List<Alarm>> getAlarms() {
        return alarms;
    }

    public void insert(Alarm alarm) {
        repository.insert(alarm);
    }

    public void update(Alarm alarm) {
        repository.update(alarm);
    }

}