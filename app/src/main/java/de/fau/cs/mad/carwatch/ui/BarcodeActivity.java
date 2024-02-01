package de.fau.cs.mad.carwatch.ui;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import java.util.concurrent.ExecutionException;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.ArraySet;

import androidx.preference.PreferenceManager;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.ui.barcode.Ean8Fragment;
import de.fau.cs.mad.carwatch.util.AlarmRepository;

public class BarcodeActivity extends AppCompatActivity {

    private static final String TAG = BarcodeActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);

        int alarmId = Constants.EXTRA_ALARM_ID_INITIAL;
        int salivaId = Constants.EXTRA_SALIVA_ID_INITIAL;

        if (getIntent() != null) {
            alarmId = getIntent().getIntExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_INITIAL);
        }

        AlarmRepository repository = AlarmRepository.getInstance(this.getApplication());
        Alarm alarm;

        try {
            alarm = repository.getAlarmById(alarmId);
            if (alarm != null)
                salivaId = alarm.getSalivaId();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error while getting alarm with id " + alarmId + " from database");
            e.printStackTrace();
        }


        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_FULLSCREEN |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
        int dayCounter = sharedPreferences.getInt(Constants.PREF_DAY_COUNTER, 1);
        int numDailySamples = sharedPreferences.getInt(Constants.PREF_TOTAL_NUM_SAMPLES, 0);
        int numScannedBarcodes = sharedPreferences.getStringSet(Constants.PREF_SCANNED_BARCODES, new ArraySet<>()).size();
        boolean dayFinished = numScannedBarcodes >= numDailySamples * dayCounter;

        if (dayFinished) {
            Drawable icon = getResources().getDrawable(R.drawable.ic_warning_24dp);
            icon.setTint(getResources().getColor(R.color.colorPrimary));

            Intent intent = new Intent(BarcodeActivity.this, AlertActivity.class);
            startActivity(intent);
        } else {
            Ean8Fragment fragment = new Ean8Fragment();
            fragment.setAlarmId(alarmId);
            fragment.setSalivaId(salivaId);

            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commitAllowingStateLoss();
        }

    }

}
