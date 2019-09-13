package de.fau.cs.mad.carwatch.logger;

import android.content.Context;

import com.orhanobut.logger.CsvFormatStrategy;
import com.orhanobut.logger.DiskLogStrategy;

public class LoggerUtil {

    private static final String TAG = LoggerUtil.class.getSimpleName();


    public static CsvFormatStrategy getFormatStrategy(Context context) {
        CsvFormatStrategy.Builder formatStrategy = CsvFormatStrategy.newBuilder();

        DiskLogHandler diskLogHandler = new DiskLogHandler(context, 2048);

        DiskLogStrategy diskLogStrategy = new DiskLogStrategy(diskLogHandler);
        formatStrategy.tag("CarWatch").logStrategy(diskLogStrategy);

        return formatStrategy.build();
    }

}
