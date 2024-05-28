package de.fau.cs.mad.carwatch.sensors;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.preference.PreferenceManager;
import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.logger.DiskLogHandler;

public class LightIntensityLoggerService implements SensorEventListener {

    private static final String TAG = LightIntensityLoggerService.class.getSimpleName();
    private static final String FILE_NAME = "light_intensity_data.csv";
    private static final String CSV_HEADER = "unix_time,date_time,light_intensity_in_lx,is_object_near";
    private final Context context;
    private final ProximitySensorListener proximitySensorListener;
    private SensorManager sensorManager;
    private int logIntervalMinutes = 5;
    private DateTime lastLogTime;


    public LightIntensityLoggerService(Context context) {
        this.context = context;
        proximitySensorListener = new ProximitySensorListener(context);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT && isLogIntervalElapsed()) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            float lightIntensity = sensorEvent.values[0];
            long lastLogTime = System.currentTimeMillis();
            boolean isObjectClose = sp.getBoolean(Constants.PREF_IS_OBJECT_CLOSE, false);
            LightLoggingData lld = new LightLoggingData(lastLogTime, lightIntensity, isObjectClose);
            new SensorEventLoggerTask().execute(lld);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    public void setLogIntervalMinutes(int logIntervalMinutes) {
        this.logIntervalMinutes = logIntervalMinutes;
    }

    public void register() {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        proximitySensorListener.register();
    }

    public void unregister() {
        sensorManager.unregisterListener(this);
        proximitySensorListener.unregister();
    }

    private boolean isLogIntervalElapsed() {
        if (lastLogTime == null) {
            lastLogTime = DateTime.now();
            return true;
        }

        DateTime now = DateTime.now();
        if (now.isAfter(lastLogTime.plusMinutes(logIntervalMinutes))) {
            lastLogTime = now;
            return true;
        }

        return false;
    }

    private void logLightData(LightLoggingData lightLoggingData) {
        try {
            File file = getLightDataFile(context);
            FileWriter fw = new FileWriter(file, true);

            fw.append(Long.toString(lightLoggingData.getTimestamp())).append(",");
            fw.append(lightLoggingData.getTranslatedTimestamp()).append(",");
            fw.append(lightLoggingData.getLightIntensity().toString()).append(",");
            fw.append(lightLoggingData.isObjectClose() ? "1" : "0").append(",");
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
        void execute(LightLoggingData loggingData) {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                Log.d("SensorEventLoggerTask", "Logging sensor event: " + loggingData.getLightIntensity() + " lx");
                logLightData(loggingData);
                return null;
            });
            executorService.shutdown(); // Remember to shut down the executor
        }
    }
}
