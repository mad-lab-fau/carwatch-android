package de.fau.cs.mad.carwatch.ui.alarm;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutionException;

import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.util.AlarmRepository;

public class AlarmViewModel extends AndroidViewModel {

    private static final String TAG = AlarmViewModel.class.getSimpleName();

    private AlarmRepository repository;
    private LiveData<Alarm> alarm;


    public AlarmViewModel(Application application) {
        super(application);
        repository = AlarmRepository.getInstance(application);
        alarm = repository.getAlarm();
    }

    // List is wrapped in LiveData in order to be observed and updated efficiently
    public LiveData<Alarm> getAlarm() {
        return alarm;
    }

    public void insert(Alarm alarm) {
        repository.insert(alarm);
    }

    public void update(Alarm alarm) {
        repository.update(alarm);
    }

    public void delete(Alarm alarm) {
        repository.delete(alarm);
    }
}