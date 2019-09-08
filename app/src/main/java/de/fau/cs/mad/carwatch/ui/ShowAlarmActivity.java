package de.fau.cs.mad.carwatch.ui;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmSnoozeReceiver;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmStopReceiver;

public class ShowAlarmActivity extends AppCompatActivity implements View.OnClickListener {

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

        Button snoozeButton = findViewById(R.id.button_snooze);
        snoozeButton.setOnClickListener(this);

        Button stopButton = findViewById(R.id.button_stop);
        stopButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_snooze:
                snoozeAlarm();
                break;
            case R.id.button_stop:
                stopAlarm();
                break;
        }
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
}
