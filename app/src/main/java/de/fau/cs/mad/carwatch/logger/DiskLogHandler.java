package de.fau.cs.mad.carwatch.logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import org.joda.time.DateTime;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

import de.fau.cs.mad.carwatch.Constants;

public class DiskLogHandler extends Handler {

    private static final String TAG = DiskLogHandler.class.getSimpleName();

    /**
     * Directory name where data will be stored on the external storage
     */
    private static final String DIR_NAME = "CarWatchLogger";

    private final Context context;

    public DiskLogHandler(Context context) {
        this(getDefaultLooper(), context);
    }

    private DiskLogHandler(Looper looper, Context context) {
        super(looper);
        this.context = context;
    }

    @SuppressWarnings("checkstyle:emptyblock")
    @Override
    public void handleMessage(Message msg) {
        String content = (String) msg.obj;

        FileWriter fileWriter = null;
        File logFile = getLogFile();
        if (logFile == null) {
            return;
        }

        try {
            fileWriter = new FileWriter(logFile, true);

            writeLog(fileWriter, content);

            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            if (fileWriter != null) {
                try {
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e1) { /* fail silently */ }
            }
        }
    }

    public static File zipDirectory(Context context, String studyName, String participantId) throws FileNotFoundException {
        File directory = getDirectory(context);
        File root = getRootDirectory(context);
        String filename = participantId == null ? "logs.zip" : String.format("logs_%s_%s.zip", studyName, participantId);
        File file = new File(root, filename);
        if (directory != null && Objects.requireNonNull(directory.list()).length > 0) {
            ZipUtil.pack(directory, file);
            return file;
        }

        throw new FileNotFoundException("No log files to zip!");
    }

    /**
     * Deletes all log files
     * @param context the context
     * @return true if all files were deleted, false otherwise
     */
    public static boolean deleteLogFiles(Context context) {
        File directory = getDirectory(context);
        String noLogFilesMsg = "No log files to delete!";

        if (directory == null) {
            Log.i(TAG, noLogFilesMsg);
            return true;
        }

        File[] files = directory.listFiles();

        if (files == null || files.length == 0) {
            Log.i(TAG, noLogFilesMsg);
            return true;
        }

        boolean deletedAll = true;
        for (File file : files) {
            if (!file.delete()) {
                Log.e(TAG, "Could not delete file " + file.getName());
                deletedAll = false;
            }
        }
        return deletedAll;
    }

    private static Looper getDefaultLooper() {
        HandlerThread ht = new HandlerThread("AndroidFileLogger");
        ht.start();
        return ht.getLooper();
    }

    private static File getRootDirectory(Context context) {
        boolean storageWritable;
        String state;
        File root;

        root = context.getExternalFilesDir(null);
        // try to write on SD card
        state = Environment.getExternalStorageState();
        switch (state) {
            case Environment.MEDIA_MOUNTED:
                // media readable and writable
                storageWritable = true;
                break;
            case Environment.MEDIA_MOUNTED_READ_ONLY:
                // media only readable
                storageWritable = false;
                Log.e(TAG, "SD card only readable!");
                break;
            default:
                // not readable or writable
                storageWritable = false;
                Log.e(TAG, "SD card not readable and writable!");
                break;
        }

        if (!storageWritable) {
            // try to write on external storage
            root = ContextCompat.getDataDir(context);
            if (root == null || !root.canWrite()) {
                Log.e(TAG, "External storage not readable and writable!");
            }
        }
        return root;
    }

    private static File getDirectory(Context context) {
        boolean fileCreated;
        File directory;
        File root = getRootDirectory(context);

        if (root != null) {
            try {
                // create directory
                directory = new File(root, DIR_NAME);
                fileCreated = directory.mkdirs();

                if (!fileCreated) {
                    fileCreated = directory.exists();
                    if (!fileCreated) {
                        Log.e(TAG, "Directory could not be created!");
                        return null;
                    } else {
                        Log.i(TAG, "Working directory is " + directory.getAbsolutePath());
                    }
                } else {
                    Log.i(TAG, "Directory created at " + directory.getAbsolutePath());
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception on dir and file create!", e);
                return null;
            }
            return directory;
        } else {
            return null;
        }
    }

    /**
     * This is always called on a single background thread.
     * Implementing classes must ONLY write to the fileWriter and nothing more.
     * The abstract class takes care of everything else including close the stream and catching IOException
     *
     * @param fileWriter an instance of FileWriter already initialised to the correct file
     */
    private void writeLog(FileWriter fileWriter, String content) throws IOException {
        fileWriter.append(content);
    }

    private File getLogFile() {
        String filename;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String studyName = sp.getString(Constants.PREF_STUDY_NAME, null);
        String participantId = sp.getString(Constants.PREF_PARTICIPANT_ID, null);
        if (participantId != null && studyName != null) {
            filename = "carwatch_" + studyName.toLowerCase() + "_" + participantId.toLowerCase() + "_" + DateTime.now().toString("YYYYMMdd");
        } else {
            filename = "carwatch_" + DateTime.now().toString("YYYYMMdd");
        }

        File directory = getDirectory(context);

        if (directory != null) {
            return new File(directory + "/" + String.format("%s.csv", filename));
        } else {
            return null;
        }
    }
}
