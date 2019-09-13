package de.fau.cs.mad.carwatch.logger;

import android.content.Context;

import com.orhanobut.logger.CsvFormatStrategy;
import com.orhanobut.logger.DiskLogStrategy;

public class LoggerUtil {

    private static final String TAG = LoggerUtil.class.getSimpleName();

    private static CsvFormatStrategy sFormatStrategy;

    public static CsvFormatStrategy getFormatStrategy(Context context) {

        if (sFormatStrategy == null) {
            DiskLogHandler diskLogHandler = new DiskLogHandler(context);
            DiskLogStrategy diskLogStrategy = new DiskLogStrategy(diskLogHandler);
            sFormatStrategy = CsvFormatStrategy.newBuilder()
                    .logStrategy(diskLogStrategy)
                    .build();
            return sFormatStrategy;
        }

        return sFormatStrategy;
    }

}
