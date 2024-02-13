package de.fau.cs.mad.carwatch.ui;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.util.concurrent.ExecutionException;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmSource;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmStopReceiver;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.ui.alarm.ShowAlarmFragment;
import de.fau.cs.mad.carwatch.ui.barcode.Ean8Fragment;
import de.fau.cs.mad.carwatch.ui.widgets.SwipeButton;
import de.fau.cs.mad.carwatch.util.AlarmRepository;

public class ShowAlarmActivity extends AppCompatActivity implements SwipeButton.OnSwipeListener {

    private static final String TAG = ShowAlarmActivity.class.getSimpleName();

    private int alarmId = Constants.EXTRA_ALARM_ID_INITIAL;
    private int salivaId = Constants.EXTRA_SALIVA_ID_INITIAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_alarm);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (getIntent() != null) {
            alarmId = getIntent().getIntExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_INITIAL);
        }

        AlarmRepository repository = AlarmRepository.getInstance(getApplication());
        Alarm alarm;

        try {
            alarm = repository.getAlarmById(alarmId);
            salivaId = alarm.getSalivaId();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error while getting alarm with id " + alarmId + " from database");
            e.printStackTrace();
            return;
        }

        keepScreenOn();

        ShowAlarmFragment fragment = new ShowAlarmFragment();
        fragment.setOnSwipeListener(this);
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commitAllowingStateLoss();

    }

    private void keepScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_FULLSCREEN |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        keepScreenOn();
    }

    @Override
    public void onSwipe() {
        stopAlarm();
    }

    private void stopAlarm() {
        Intent stopAlarmIntent = new Intent(this, AlarmStopReceiver.class);
        stopAlarmIntent.putExtra(Constants.EXTRA_ALARM_ID, alarmId);
        stopAlarmIntent.putExtra(Constants.EXTRA_SALIVA_ID, salivaId);
        stopAlarmIntent.putExtra(Constants.EXTRA_SOURCE, AlarmSource.SOURCE_ACTIVITY);
        stopAlarmIntent.setAction("Stop Alarm");

        sendOrderedBroadcast(stopAlarmIntent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getResultCode() == Activity.RESULT_CANCELED) {
                    if (checkAlarmOngoing() || salivaId == -1) {
                        finish();
                    } else {
                        Intent alertIntent = new Intent(ShowAlarmActivity.this, AlertActivity.class);
                        startActivity(alertIntent);
                        finish();
                    }
                }

                Ean8Fragment fragment = new Ean8Fragment();
                fragment.setAlarmId(alarmId);
                fragment.setSalivaId(salivaId);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commitAllowingStateLoss();
            }
        }, null, Activity.RESULT_OK, null, null);
    }

    private boolean checkAlarmOngoing() {
        int alarmIdOngoing = PreferenceManager.getDefaultSharedPreferences(this).getInt(Constants.PREF_ID_ONGOING_ALARM, Constants.EXTRA_ALARM_ID_INITIAL);
        // There's already a saliva procedure running at the moment
        return (alarmIdOngoing != Constants.EXTRA_ALARM_ID_INITIAL) && (alarmIdOngoing % Constants.ALARM_OFFSET != alarmId % Constants.ALARM_OFFSET);
    }
}
