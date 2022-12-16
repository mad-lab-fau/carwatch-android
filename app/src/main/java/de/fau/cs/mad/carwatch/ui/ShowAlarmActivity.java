package de.fau.cs.mad.carwatch.ui;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmSource;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmStopReceiver;
import de.fau.cs.mad.carwatch.ui.alarm.ShowAlarmFragment;
import de.fau.cs.mad.carwatch.ui.barcode.BarcodeFragment;
import de.fau.cs.mad.carwatch.ui.widgets.SwipeButton;

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
            salivaId = getIntent().getIntExtra(Constants.EXTRA_SALIVA_ID, Constants.EXTRA_SALIVA_ID_INITIAL);
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
                    if (checkAlarmOngoing()) {
                        finish();
                    } else {
                        Intent alertIntent = new Intent(ShowAlarmActivity.this, AlertActivity.class);
                        startActivity(alertIntent);
                    }
                }

                BarcodeFragment fragment = new BarcodeFragment();
                fragment.setAlarmId(alarmId);
                fragment.setSalivaId(salivaId);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commitAllowingStateLoss();
            }
        }, null, Activity.RESULT_OK, null, null);
    }

    private boolean checkAlarmOngoing() {
        int alarmIdOngoing = PreferenceManager.getDefaultSharedPreferences(this).getInt(Constants.PREF_MORNING_ONGOING, Constants.EXTRA_ALARM_ID_INITIAL);
        // There's already a saliva procedure running at the moment
        return (alarmIdOngoing != Constants.EXTRA_ALARM_ID_INITIAL) && (alarmIdOngoing % Constants.ALARM_OFFSET != alarmId % Constants.ALARM_OFFSET);
    }
}
