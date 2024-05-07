package de.fau.cs.mad.carwatch.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.annotation.NonNull;

public class ProximitySensorListener implements SensorEventListener {

    private static final String TAG = ProximitySensorListener.class.getSimpleName();
    private static final float MAX_OBJECT_NEAR_THRESHOLD = 5;
    private SensorManager sensorManager;
    private boolean isRecording = false;
    private float objectNearThreshold = MAX_OBJECT_NEAR_THRESHOLD;
    private float distance = 0;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            distance = sensorEvent.values[0];
            Log.d(TAG, "Proximity sensor: " + distance);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    public void startListening(@NonNull Context context) {
        if (isRecording) {
            return;
        }

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        isRecording = true;
        if (sensor != null)
            objectNearThreshold = Math.min(sensor.getMaximumRange(), MAX_OBJECT_NEAR_THRESHOLD);
        Log.d(TAG, "Proximity sensor registered; Is Object close: " + isObjectClose());
    }

    public void stopListening () {
        if (!isRecording) {
            return;
        }
        sensorManager.unregisterListener(this);
        isRecording = false;
    }

    public boolean isObjectClose() {
        return distance < objectNearThreshold;
    }
}
