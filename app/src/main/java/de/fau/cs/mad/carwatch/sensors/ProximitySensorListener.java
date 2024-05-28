package de.fau.cs.mad.carwatch.sensors;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.preference.PreferenceManager;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;

public class ProximitySensorListener implements SensorEventListener {
    private static final float MAX_OBJECT_NEAR_THRESHOLD = 5;
    private static final String TAG = ProximitySensorListener.class.getSimpleName();
    private SensorManager sensorManager;
    private float objectCloseThreshold = MAX_OBJECT_NEAR_THRESHOLD;
    private final Context context;

    public ProximitySensorListener(Context context) {
       this.context = context;
    }

    public void register() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (sensor != null) {
            objectCloseThreshold = Math.min(sensor.getMaximumRange(), MAX_OBJECT_NEAR_THRESHOLD);
        }
        LoggerUtil.log(TAG, "Proximity sensor registered");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float distance = sensorEvent.values[0];
            boolean isObjectClose = distance < objectCloseThreshold;
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putBoolean(Constants.PREF_IS_OBJECT_CLOSE, isObjectClose).apply();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }


    public void unregister() {
        sensorManager.unregisterListener(this);
    }
}
