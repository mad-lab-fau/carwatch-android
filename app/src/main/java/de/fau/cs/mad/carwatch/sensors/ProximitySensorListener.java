package de.fau.cs.mad.carwatch.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


public class ProximitySensorListener implements SensorEventListener {
    private static final float MAX_OBJECT_NEAR_THRESHOLD = 5;
    private static final String TAG = ProximitySensorListener.class.getSimpleName();
    private SensorManager sensorManager;
    private float objectCloseThreshold = MAX_OBJECT_NEAR_THRESHOLD;
    private boolean isObjectClose = false;
    private final Context context;

    public ProximitySensorListener(Context context) {
       this.context = context;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float distance = sensorEvent.values[0];
            isObjectClose = distance < objectCloseThreshold;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    public void register() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (sensor != null) {
            objectCloseThreshold = Math.min(sensor.getMaximumRange(), MAX_OBJECT_NEAR_THRESHOLD);
        }
    }


    public void unregister() {
        sensorManager.unregisterListener(this);
    }

    public boolean isObjectClose() {
        return isObjectClose;
    }
}
