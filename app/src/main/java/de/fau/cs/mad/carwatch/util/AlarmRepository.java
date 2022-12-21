package de.fau.cs.mad.carwatch.util;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutionException;

import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.db.AlarmDao;
import de.fau.cs.mad.carwatch.db.AlarmDatabase;

/**
 * Abstracted Repository to handle interactions between ViewModels and Database
 */
public class AlarmRepository {

    private static AlarmRepository sAlarmRepository;

    private final AlarmDao alarmModel;
    private final LiveData<Alarm> alarm;

    private AlarmRepository(Application application) {
        // Application is used instead of Context in order to prevent memory leaks
        // between Activity switches
        AlarmDatabase db = AlarmDatabase.getInstance(application);
        alarmModel = db.alarmModel();
        alarm = alarmModel.getAlarm();
    }

    public static AlarmRepository getInstance(Application application) {
        if (sAlarmRepository == null) {
            sAlarmRepository = new AlarmRepository(application);
        }
        return sAlarmRepository;
    }

    // Observed LiveData will notify the observer when data has changed
    public LiveData<Alarm> getAlarm() {
        return alarm;
    }

    public void insert(Alarm alarm) {
        new InsertAsyncTask(alarmModel).execute(alarm);
    }

    public void replace(Alarm alarm) {
        new ReplaceAsyncTask(alarmModel).execute(alarm);
    }

    public void update(Alarm alarm) {
        new UpdateAsyncTask(alarmModel).execute(alarm);
    }

    public void updateActive(Alarm alarm) {
        new UpdateActiveAsyncTask(alarmModel).execute(alarm);
    }

    public void delete(Alarm alarm) {
        new DeleteAsyncTask(alarmModel).execute(alarm);
    }

    public Alarm getAlarmById(int id) throws ExecutionException, InterruptedException {
        return new GetByIdAsyncTask(alarmModel).execute(id).get();
    }


    /*
     * Asynchronous Tasks
     *
     * One for each interaction with database.
     * All classes are static to prevent memory leaks.
     */

    private static class InsertAsyncTask extends AsyncTask<Alarm, Void, Void> {

        private final AlarmDao alarmModel;

        InsertAsyncTask(AlarmDao alarmModel) {
            this.alarmModel = alarmModel;
        }

        @Override
        protected Void doInBackground(final Alarm... params) {
            alarmModel.insert(params[0]);
            return null;
        }
    }

    private static class ReplaceAsyncTask extends AsyncTask<Alarm, Void, Void> {

        private final AlarmDao alarmModel;

        ReplaceAsyncTask(AlarmDao alarmModel) {
            this.alarmModel = alarmModel;
        }

        @Override
        protected Void doInBackground(final Alarm... params) {
            alarmModel.insertOrReplaceAlarm(params[0]);
            return null;
        }
    }

    private static class UpdateAsyncTask extends AsyncTask<Alarm, Void, Void> {

        private final AlarmDao alarmModel;

        UpdateAsyncTask(AlarmDao alarmModel) {
            this.alarmModel = alarmModel;
        }

        @Override
        protected Void doInBackground(final Alarm... params) {
            alarmModel.update(params[0]);
            return null;
        }
    }

    private static class UpdateActiveAsyncTask extends AsyncTask<Alarm, Void, Void> {

        private final AlarmDao alarmModel;

        UpdateActiveAsyncTask(AlarmDao alarmModel) {
            this.alarmModel = alarmModel;
        }

        @Override
        protected Void doInBackground(final Alarm... params) {
            alarmModel.updateActive(params[0].getId(), params[0].isActive());
            return null;
        }
    }

    private static class DeleteAsyncTask extends AsyncTask<Alarm, Void, Void> {

        private final AlarmDao alarmModel;

        DeleteAsyncTask(AlarmDao alarmModel) {
            this.alarmModel = alarmModel;
        }

        @Override
        protected Void doInBackground(final Alarm... params) {
            alarmModel.delete(params[0]);
            return null;
        }
    }

    private static class GetByIdAsyncTask extends AsyncTask<Integer, Void, Alarm> {

        private final AlarmDao alarmModel;

        GetByIdAsyncTask(AlarmDao alarmModel) {
            this.alarmModel = alarmModel;
        }

        @Override
        protected Alarm doInBackground(final Integer... params) {
            return alarmModel.getById(params[0]);
        }
    }
}
