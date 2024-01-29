package de.fau.cs.mad.carwatch.logger;

import android.content.Context;
import android.util.Log;

import com.orhanobut.logger.DiskLogStrategy;
import com.orhanobut.logger.Logger;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;

public class LoggerUtil {

    private static final String TAG = LoggerUtil.class.getSimpleName();

    private static LogFormatStrategy sFormatStrategy;

    public static LogFormatStrategy getFormatStrategy(Context context) {

        if (sFormatStrategy == null) {
            DiskLogHandler diskLogHandler = new DiskLogHandler(context);
            DiskLogStrategy diskLogStrategy = new DiskLogStrategy(diskLogHandler);
            sFormatStrategy = new LogFormatStrategy(diskLogStrategy);
            return sFormatStrategy;
        }

        return sFormatStrategy;
    }

    public static void log(String tag, JSONObject json) {
        log(tag, json.toString());
    }

    public static void log(String tag, String message) {
        Log.d(TAG, tag + "\t" + message);
        Logger.log(Logger.INFO, tag, message, null);
    }


    public static File zipDirectory(Context context, String studyName, String participantId) throws FileNotFoundException {
        return DiskLogHandler.zipDirectory(context, studyName, participantId);
    }

}
