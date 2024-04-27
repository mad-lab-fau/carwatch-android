package de.fau.cs.mad.carwatch.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.logger.DiskLogHandler;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;

public class LightIntensityLogger implements SensorEventListener {

    private static final String TAG = LightIntensityLogger.class.getSimpleName();
    private static final String FILE_NAME = "light_intensity_data.csv";
    private static final String CSV_HEADER = "Date,Time,Light Intensity";
    private static final String UNIT = "lx";
    private static final int PERIOD = 10; // in seconds
    private static LightIntensityLogger instance;
    private SensorManager sensorManager;
    private boolean isRecording = false;
    private float lightIntensity;
    private long lastLogTime = 0; // in milliseconds
    private List<Pair<Long, Float>> lightIntensityData = new ArrayList<>();


    private LightIntensityLogger() { }

    public static LightIntensityLogger getInstance() {
        if (instance == null)
            instance = new LightIntensityLogger();
        return instance;
    }

    public void startRecording(@NonNull Context context) {
        if (isRecording)
            return;

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        isRecording = true;
        Log.d(TAG, "Light sensor registered");
    }

    public void stopRecording() {
        if (!isRecording)
            return;
        sensorManager.unregisterListener(this);
        isRecording = false;
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

    public void storeLightData(Context context) {
        try {
            File file = getLightDataFile(context);
            FileWriter fw = new FileWriter(file, true);

            for (Pair<Long, Float> data : lightIntensityData) {
                DateTime time = new DateTime(data.first);

                fw.append(time.toString("yyyy-MM-dd")).append(",");
                fw.append(time.toString("HH:mm:ss")).append(",");
                fw.append(data.second.toString()).append(" ").append(UNIT);
                fw.append("\n");
            }

            fw.flush();
            fw.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing light data to file", e);
        }
    }

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
