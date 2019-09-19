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
    private LiveData<List<Alarm>> allAlarms;


    public AlarmViewModel(Application application) {
        super(application);
        repository = AlarmRepository.getInstance(application);
        allAlarms = repository.getAllAlarms();
    }

    // List is wrapped in LiveData in order to be observed and updated efficiently
    public LiveData<List<Alarm>> getAllAlarms() {
        return allAlarms;
    }

    public void insert(Alarm alarm) {
        repository.insert(alarm);
    }

    public void replace(Alarm alarm) {
        repository.replace(alarm);
    }

    public void update(Alarm alarm) {
        repository.update(alarm);
    }

    public void updateActive(Alarm alarm) {
        repository.updateActive(alarm);
    }

    public void delete(Alarm alarm) {
        repository.delete(alarm);
    }

    public Alarm getAlarmById(int id) {
        try {
            return repository.getAlarmById(id);
        } catch (InterruptedException | ExecutionException e) {
            Log.e(TAG, "Error when retrieving alarm by id: " + id);
            e.printStackTrace();
        }
        return new Alarm();
    }
}