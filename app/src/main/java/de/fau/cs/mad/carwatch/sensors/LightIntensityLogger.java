package de.fau.cs.mad.carwatch.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;

public class LightIntensityLogger implements SensorEventListener {

    private static final String TAG = LightIntensityLogger.class.getSimpleName();
    private static final String UNIT = "lx";
    private static final int PERIOD = 10; // in seconds
    private static LightIntensityLogger instance;
    private static SensorManager sensorManager;
    private static boolean isRegistered = false;
    private float lightIntensity;
    private long lastLogTime = 0; // in milliseconds
    private List<Pair<Long, Float>> lightIntensityData = new ArrayList<>();


    private LightIntensityLogger() { }

    public static LightIntensityLogger getInstance() {
        if (instance == null)
            instance = new LightIntensityLogger();
        return instance;
    }

    public static LightIntensityLogger startRecording(@NonNull Context context) {
        if (instance == null)
            instance = new LightIntensityLogger();

        if (isRegistered)
            return instance;

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(instance, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        isRegistered = true;
        Log.d(TAG, "Light sensor registered");
        return instance;
    }

    public static void stopRecording() {
        if (!isRegistered)
            return;
        sensorManager.unregisterListener(instance);
        isRegistered = false;
        Log.d(TAG, "Light sensor unregistered");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT) {
            lightIntensity = sensorEvent.values[0];
        }
        if (System.currentTimeMillis() - lastLogTime > PERIOD * 1000) {
            lastLogTime = System.currentTimeMillis();
            lightIntensityData.add(new Pair<>(lastLogTime, lightIntensity));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    public void logLightIntensity() {
        try {
            JSONObject json = new JSONObject();
            json.put(Constants.LOGGER_SENSOR_LIGHT_INTENSITY, lightIntensity);
            json.put(Constants.LOGGER_SENSOR_UNIT, UNIT);
            LoggerUtil.log(TAG, json);
        } catch (JSONException e) {
            LoggerUtil.log(TAG, "current light intensity: " + lightIntensity);
        }
    }
}
