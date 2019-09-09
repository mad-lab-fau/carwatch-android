package de.fau.cs.mad.carwatch.ui;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmSnoozeReceiver;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmStopReceiver;
import de.fau.cs.mad.carwatch.ui.widgets.SwipeButton;

public class ShowAlarmActivity extends AppCompatActivity implements SwipeButton.OnSwipeListener {

    private static final String TAG = ShowAlarmActivity.class.getSimpleName();

    private int alarmId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_alarm);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (getIntent() != null && getIntent().hasExtra(Constants.EXTRA_ID)) {
            this.alarmId = getIntent().getIntExtra(Constants.EXTRA_ID, 0);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);

        SwipeButton swipeButton = findViewById(R.id.button_swipe);
        swipeButton.setOnSwipeListener(this);

    }

    private void snoozeAlarm() {
        Intent snoozeAlarmIntent = new Intent(this, AlarmSnoozeReceiver.class);
        snoozeAlarmIntent.putExtra(Constants.EXTRA_ID, alarmId);
        snoozeAlarmIntent.setAction("Snooze Alarm");
        sendBroadcast(snoozeAlarmIntent);
        finish();
    }

    private void stopAlarm() {
        Intent stopAlarmIntent = new Intent(this, AlarmStopReceiver.class);
        stopAlarmIntent.putExtra(Constants.EXTRA_ID, alarmId);
        stopAlarmIntent.setAction("Stop Alarm");
        sendBroadcast(stopAlarmIntent);
        finish();
    }

    @Override
    public void onSwipeLeft() {
        snoozeAlarm();
    }

    @Override
    public void onSwipeRight() {
        stopAlarm();
    }
}
