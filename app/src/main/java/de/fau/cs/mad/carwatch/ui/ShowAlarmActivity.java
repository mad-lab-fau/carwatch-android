package de.fau.cs.mad.carwatch.ui;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmSource;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmStopReceiver;
import de.fau.cs.mad.carwatch.ui.widgets.SwipeButton;

public class ShowAlarmActivity extends AppCompatActivity implements SwipeButton.OnSwipeListener {

    private static final String TAG = ShowAlarmActivity.class.getSimpleName();

    private int alarmId = Constants.EXTRA_ALARM_ID_DEFAULT;
    private int salivaId = Constants.EXTRA_SALIVA_ID_DEFAULT;

    Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_alarm);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (getIntent() != null) {
            this.alarmId = getIntent().getIntExtra(Constants.EXTRA_ALARM_ID, Constants.EXTRA_ALARM_ID_DEFAULT);
            this.salivaId = getIntent().getIntExtra(Constants.EXTRA_SALIVA_ID, Constants.EXTRA_SALIVA_ID_DEFAULT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }

        SwipeButton swipeButton = findViewById(R.id.button_swipe);
        swipeButton.setOnSwipeListener(this);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(Constants.VIBRATION_PATTERN, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    /*private void snoozeAlarm() {
        Intent snoozeAlarmIntent = new Intent(this, AlarmSnoozeReceiver.class);
        snoozeAlarmIntent.putExtra(Constants.EXTRA_ALARM_ID, alarmId);
        snoozeAlarmIntent.putExtra(Constants.EXTRA_SOURCE, AlarmSource.SOURCE_ACTIVITY);
        snoozeAlarmIntent.setAction("Snooze Alarm");
        sendBroadcast(snoozeAlarmIntent);
        finish();
    }*/

    private void stopAlarm() {
        Intent stopAlarmIntent = new Intent(this, AlarmStopReceiver.class);
        stopAlarmIntent.putExtra(Constants.EXTRA_ALARM_ID, alarmId);
        stopAlarmIntent.putExtra(Constants.EXTRA_SALIVA_ID, salivaId);
        stopAlarmIntent.putExtra(Constants.EXTRA_SOURCE, AlarmSource.SOURCE_ACTIVITY);
        stopAlarmIntent.setAction("Stop Alarm");
        sendBroadcast(stopAlarmIntent);
        finish();
    }

    @Override
    public void onSwipeLeft() {
        stopAlarm();
    }

    @Override
    public void onSwipeRight() {
        stopAlarm();
    }
}
