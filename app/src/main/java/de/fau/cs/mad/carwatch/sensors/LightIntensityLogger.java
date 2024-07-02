package de.fau.cs.mad.carwatch.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.logger.DiskLogHandler;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;

public class LightIntensityLogger implements SensorEventListener {

    private static final String TAG = LightIntensityLogger.class.getSimpleName();
    private static final String FILE_NAME = "light_intensity_data.csv";
    private static final String CSV_HEADER = "unix_time,date_time,light_intensity_in_lx,is_object_near";
    private static final int DEFAULT_LOG_INTERVAL = 5;  // in minutes
    private static LightIntensityLogger instance;
    private final ProximitySensorListener proximitySensorListener;
    private SensorManager sensorManager;
    private Float lightIntensity;


    private LightIntensityLogger(Context context) {
        proximitySensorListener = new ProximitySensorListener(context);
    }

    public static LightIntensityLogger getInstance(Context context) {
        if (instance == null)
            instance = new LightIntensityLogger(context);
        return instance;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT) {
            lightIntensity = sensorEvent.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    public void startLogging(Context context) {
        register(context);
        LoggerUtil.log(Constants.LOGGER_START_LIGHT_LOGGING, new JSONObject());
    }

    public void log(Context context) {
        logCurrentLightData(context);
    }

    public void stopLogging() {
        unregister();
        LoggerUtil.log(Constants.LOGGER_STOP_LIGHT_LOGGING, new JSONObject());
    }

    private void logCurrentLightData(Context context) {
        if (lightIntensity == null) {
            return;
        }

        boolean isObjectClose = proximitySensorListener.isObjectClose();
        LightLoggingData lld = new LightLoggingData(System.currentTimeMillis(), lightIntensity, isObjectClose);
        new SensorEventLoggerTask().execute(lld, context);
    }

    private void register(Context context) {
        lightIntensity = null;  // Reset light intensity
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        proximitySensorListener.register();
    }

    private void unregister() {
        sensorManager.unregisterListener(this);
        proximitySensorListener.unregister();
    }

    private void logLightData(LightLoggingData lightLoggingData, Context context) {
        try {
            File file = getLightDataFile(context);
            FileWriter fw = new FileWriter(file, true);

            fw.append(Long.toString(lightLoggingData.getTimestamp())).append(",");
            fw.append(lightLoggingData.getTranslatedTimestamp()).append(",");
            fw.append(lightLoggingData.getLightIntensity().toString()).append(",");
            fw.append(lightLoggingData.isObjectClose() ? "1" : "0");
            fw.append("\n");

            fw.flush();
            fw.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing light data to file", e);
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

    private class SensorEventLoggerTask {
        void execute(LightLoggingData loggingData, Context context) {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                Log.d("SensorEventLoggerTask", "Logging sensor event: " + loggingData.getLightIntensity() + " lx");
                logLightData(loggingData, context);
                return null;
            });
            executorService.shutdown(); // Remember to shut down the executor
        }
    }
}
