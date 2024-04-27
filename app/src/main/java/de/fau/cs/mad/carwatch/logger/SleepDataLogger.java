package de.fau.cs.mad.carwatch.logger;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import de.fau.cs.mad.carwatch.sleep.SleepPhase;

public class SleepDataLogger {
    private static final String TAG = SleepDataLogger.class.getSimpleName();
    private static final String FILE_NAME = "sleep_data.csv";
    private static final String CSV_HEADER = "Start Date,Stage,Start Time,End Time";
    private static final String TIME_PATTERN = "HH:mm";

    public static void log(Context context, List<SleepPhase> sleepPhases) {
        if (context == null || sleepPhases == null || sleepPhases.isEmpty())
            return;

        try {
            File file = getSleepDataFile(context);
            FileWriter fw = new FileWriter(file, true);

            for (SleepPhase phase : sleepPhases) {
                fw.append(phase.getStart().toString("yyyy-MM-dd"));
                fw.append(",");
                fw.append(phase.getName());
                fw.append(",");
                fw.append(phase.getStart().toString(TIME_PATTERN));
                fw.append(",");
                fw.append(phase.getEnd().toString(TIME_PATTERN));
                fw.append("\n");
            }

            fw.flush();
            fw.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while getting sleep data file", e);
        }
    }

    private static File getSleepDataFile(Context context) throws FileNotFoundException {
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
