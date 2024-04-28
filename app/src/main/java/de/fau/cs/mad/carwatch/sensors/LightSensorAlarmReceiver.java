package de.fau.cs.mad.carwatch.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.joda.time.DateTime;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;

public class LightSensorAlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_START_RECORDING = "start";
    public static final String ACTION_STOP_RECORDING = "stop";
    private static final int RECORDING_DURATION = 90; // in minutes

    @Override
    public void onReceive(Context context, Intent intent) {
        LightIntensityLogger lightIntensityLogger = LightIntensityLogger.getInstance();
        String action = intent.getStringExtra(Constants.EXTRA_LIGHT_SENSOR_ACTION);

        if (ACTION_START_RECORDING.equals(action)) {
            Log.d(LightSensorAlarmReceiver.class.getSimpleName(), "Start recording light data");
            lightIntensityLogger.startRecording(context);
            AlarmHandler.scheduleLightSensorAlarm(context, new DateTime().plusMinutes(RECORDING_DURATION), false);
        } else if (ACTION_STOP_RECORDING.equals(action)) {
            Log.d(LightSensorAlarmReceiver.class.getSimpleName(), "Stop recording light data");
            lightIntensityLogger.stopRecording();
            lightIntensityLogger.storeLightData(context);
            lightIntensityLogger.clearData();
        }
    }
}
