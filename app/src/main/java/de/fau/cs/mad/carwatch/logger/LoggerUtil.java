package de.fau.cs.mad.carwatch.logger;

import android.content.Context;

import com.orhanobut.logger.CsvFormatStrategy;
import com.orhanobut.logger.DiskLogStrategy;
import com.orhanobut.logger.Logger;

import java.io.File;

public class LoggerUtil {

    private static final String TAG = LoggerUtil.class.getSimpleName();

    private static CsvFormatStrategy sFormatStrategy;

    public static CsvFormatStrategy getFormatStrategy(Context context) {

        if (sFormatStrategy == null) {
            DiskLogHandler diskLogHandler = new DiskLogHandler(context);
            DiskLogStrategy diskLogStrategy = new DiskLogStrategy(diskLogHandler);
            sFormatStrategy = CsvFormatStrategy.newBuilder()
                    .tag("CarWatch")
                    .logStrategy(diskLogStrategy)
                    .build();
            return sFormatStrategy;
        }

        return sFormatStrategy;
    }


    public static void log(String tag, String message) {
        Logger.log(Logger.INFO, tag, message, null);
    }


    public static File zipDirectory(Context context, String subjectId) {
        return DiskLogHandler.zipDirectory(context, subjectId);
    }

}
