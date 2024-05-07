package de.fau.cs.mad.carwatch.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.fau.cs.mad.carwatch.logger.DiskLogHandler;

public class LightIntensityLogger implements SensorEventListener {

    private static final String TAG = LightIntensityLogger.class.getSimpleName();
    private static final String FILE_NAME = "light_intensity_data.csv";
    private static final String CSV_HEADER = "unix_time,date_time,light_intensity_in_lx,is_object_near";
    private static final int PERIOD = 60; // in seconds
    private static LightIntensityLogger instance;
    private final List<LightLoggingData> lightIntensityData = new ArrayList<>();
    private final ProximitySensorListener proximitySensorListener;
    private SensorManager sensorManager;
    private boolean isRecording = false;
    private float lightIntensity;
    private long lastLogTime = 0; // in milliseconds


    private LightIntensityLogger() {
        proximitySensorListener = new ProximitySensorListener();
    }

    public static LightIntensityLogger getInstance() {
        if (instance == null)
            instance = new LightIntensityLogger();
        return instance;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT) {
            lightIntensity = sensorEvent.values[0];
        }
        if (System.currentTimeMillis() - lastLogTime > PERIOD * 1000) {
            lastLogTime = System.currentTimeMillis();
            boolean isObjectClose = proximitySensorListener.isObjectClose();
            LightLoggingData lld = new LightLoggingData(lastLogTime, lightIntensity, isObjectClose);
            lightIntensityData.add(lld);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    public void startRecording(@NonNull Context context) {
        if (isRecording)
            return;

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        proximitySensorListener.startListening(context);
        isRecording = true;
        Log.d(TAG, "Light sensor registered");
    }

    public void stopRecording() {
        if (!isRecording)
            return;
        sensorManager.unregisterListener(this);
        isRecording = false;
        proximitySensorListener.stopListening();
        Log.d(TAG, "Light sensor unregistered");
    }

    public void storeLightData(Context context) {
        try {
            File file = getLightDataFile(context);
            FileWriter fw = new FileWriter(file, true);

            for (LightLoggingData data : lightIntensityData) {
                fw.append(Long.toString(data.getTimestamp())).append(",");
                fw.append(data.getTranslatedTimestamp()).append(",");
                fw.append(data.getLightIntensity().toString()).append(",");
                fw.append(data.isObjectClose() ? "1" : "0").append(",");
                fw.append("\n");
            }

            fw.flush();
            fw.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing light data to file", e);
        }
    }

    public void clearData() {
        lightIntensityData.clear();
    }

    private File getLightDataFile(Context context) throws FileNotFoundException {
        File dir = DiskLogHandler.getLogDirectory(context);
        if (!dir.exists())
            throw new FileNotFoundException("Could not create directory");

        File file = new File(dir, FILE_NAME);
        if (file.exists())
            return file;

        try {
            FileWriter fw = new FileWriter(file, true);
            fw.append(CSV_HEADER);
            fw.append("\n");
            fw.flush();
            fw.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing headline for sleep data file", e);
        }

        return file;
    }
}
