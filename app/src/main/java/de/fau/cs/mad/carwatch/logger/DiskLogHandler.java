package de.fau.cs.mad.carwatch.logger;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DiskLogHandler extends Handler {

    private static final String TAG = DiskLogHandler.class.getSimpleName();

    private Context context;

    /**
     * Directory name where data will be stored on the external storage
     */
    private static final String DIR_NAME = "CarWatchLogger";

    public DiskLogHandler(Context context) {
        this(getDefaultLooper(), context);
    }

    public DiskLogHandler(Looper looper, Context context) {
        super(looper);
        this.context = context;
    }

    private static Looper getDefaultLooper() {
        HandlerThread ht = new HandlerThread("AndroidFileLogger");
        ht.start();
        return ht.getLooper();
    }

    @SuppressWarnings("checkstyle:emptyblock")
    @Override
    public void handleMessage(Message msg) {
        String content = (String) msg.obj;

        FileWriter fileWriter = null;
        File logFile = getLogFile();
        if (logFile == null) {
            Log.e(TAG, "NULL");
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
        String filename = "carwatch_" + DateTime.now().toString("YYYYMMdd");

        boolean storageWritable;
        boolean fileCreated;
        String state;
        File root;
        File path;

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
            if (root != null && root.canWrite()) {
                storageWritable = true;
            } else {
                Log.e(TAG, "External storage not readable and writable!");
            }
        }

        if (storageWritable) {
            try {
                // create directory
                path = new File(root, DIR_NAME);
                fileCreated = path.mkdirs();

                if (!fileCreated) {
                    fileCreated = path.exists();
                    if (!fileCreated) {
                        Log.e(TAG, "Directory could not be created!");
                        return null;
                    } else {
                        Log.i(TAG, "Working directory is " + path.getAbsolutePath());
                    }
                } else {
                    Log.i(TAG, "Directory created at " + path.getAbsolutePath());
                }

                return new File(path + "/" + String.format("%s.csv", filename));

            } catch (Exception e) {
                Log.e(TAG, "Exception on dir and file create!", e);
                return null;
            }
        }
        return null;
    }
}
