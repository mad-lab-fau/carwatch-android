package de.fau.cs.mad.carwatch.ui;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.concurrent.ExecutionException;

import androidx.appcompat.app.AppCompatActivity;

import androidx.preference.PreferenceManager;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmSoundControl;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.ui.barcode.Ean8Fragment;
import de.fau.cs.mad.carwatch.util.AlarmRepository;

public class BarcodeActivity extends AppCompatActivity {

    private static final String TAG = BarcodeActivity.class.getSimpleName();
    private int alarmId = Constants.EXTRA_ALARM_ID_INITIAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);
        boolean cancelAlarm = true;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        HeaderUiHelper.configureTransparentStatusBar(this, sharedPreferences);
        TextView headerTitle = findViewById(R.id.tv_header_title);
        headerTitle.setText(R.string.title_activity_scan);

        if (getIntent() != null) {
            alarmId = getIntent().getIntExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_INITIAL);
            cancelAlarm = getIntent().getBooleanExtra(Constants.EXTRA_CANCEL_ALARM, true);
        }

        AlarmRepository repository = AlarmRepository.getInstance(this.getApplication());
        Alarm alarm;
        int salivaId = Constants.EXTRA_SALIVA_ID_INITIAL;

        try {
            alarm = repository.getAlarmById(alarmId);
            if (alarm != null)
                salivaId = alarm.getSalivaId();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error while getting alarm with id " + alarmId + " from database", e);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );
        }

        Ean8Fragment fragment = new Ean8Fragment();
        fragment.setAlarmId(alarmId);
        fragment.setSalivaId(salivaId);
        fragment.setCancelAlarmAfterScan(cancelAlarm);

        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commitAllowingStateLoss();

    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
        boolean stopNotification = sharedPreferences.getBoolean(Constants.PREF_TIMER_NOTIFICATION_IS_SHOWN, false);
        if (stopNotification) {
            stopAlarmNotification();
            sharedPreferences.edit().putBoolean(Constants.PREF_TIMER_NOTIFICATION_IS_SHOWN, false).apply();
        }
    }

    private void stopAlarmNotification() {
        int notificationId = alarmId + Constants.ALARM_OFFSET_TIMER;
        AlarmSoundControl alarmSoundControl = AlarmSoundControl.getInstance();
        alarmSoundControl.stopAlarmSound();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null)
            notificationManager.cancel(notificationId);
    }
}
