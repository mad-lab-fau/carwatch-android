package de.fau.cs.mad.carwatch.logger;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.sleep.SleepPhase;

public class SleepDataLogger {
    private static final String TAG = SleepDataLogger.class.getSimpleName();
    private static final String FILE_NAME = "sleep_data.csv";
    private static final String CSV_HEADER = String.join(Constants.CSV_ROW_SEPARATOR,
            "start_unix_time", "end_unix_time", "start_date_time", "end_date_time", "stage");
    private static final String DATE_TIME_PATTERN = "dd.MM.yyy HH:mm";

    public static void log(Context context, List<SleepPhase> sleepPhases) {
        if (context == null || sleepPhases == null || sleepPhases.isEmpty())
            return;

        try {
            File file = getSleepDataFile(context);
            FileWriter fw = new FileWriter(file, true);

            for (SleepPhase phase : sleepPhases) {
                fw.append(String.valueOf(phase.getStart().getMillis()));
                fw.append(Constants.CSV_COL_SEPARATOR);
                fw.append(String.valueOf(phase.getEnd().getMillis()));
                fw.append(Constants.CSV_COL_SEPARATOR);
                fw.append(phase.getStart().toString(DATE_TIME_PATTERN));
                fw.append(Constants.CSV_COL_SEPARATOR);
                fw.append(phase.getEnd().toString(DATE_TIME_PATTERN));
                fw.append(Constants.CSV_COL_SEPARATOR);
                fw.append(phase.getName());
                fw.append(Constants.CSV_ROW_SEPARATOR);
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
            fw.append(Constants.CSV_ROW_SEPARATOR);
            fw.flush();
            fw.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while writing headline for sleep data file", e);
        }

        return file;
    }
}
