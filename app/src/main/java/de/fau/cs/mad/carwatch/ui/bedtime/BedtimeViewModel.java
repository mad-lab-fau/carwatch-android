package de.fau.cs.mad.carwatch.ui.bedtime;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import de.fau.cs.mad.carwatch.Constants;

public class BedtimeViewModel extends AndroidViewModel {

    private static final String TAG = BedtimeViewModel.class.getSimpleName();

    private MutableLiveData<Boolean> salivaTaken;

    public BedtimeViewModel(Application application) {
        super(application);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(application);
        DateTime date = new DateTime(sp.getLong(Constants.PREF_EVENING_TAKEN, 0));
        if (date.equals(LocalTime.MIDNIGHT.toDateTimeToday())) {
            salivaTaken = new MutableLiveData<>(true);
        } else {
            salivaTaken = new MutableLiveData<>(false);
        }
    }


    public LiveData<Boolean> getSalivaTaken() {
        return salivaTaken;
    }

    public void setSalivaTaken(boolean salivaTaken) {
        if (salivaTaken) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplication());
            DateTime time = LocalTime.MIDNIGHT.toDateTimeToday();
            // still consider this the "night before"
            if (LocalTime.now().isBefore(new LocalTime(5, 0))) {
                time = time.minusDays(1);
            }

            sp.edit().putLong(Constants.PREF_EVENING_TAKEN, time.getMillis()).apply();
        }

        this.salivaTaken.setValue(salivaTaken);
    }
}