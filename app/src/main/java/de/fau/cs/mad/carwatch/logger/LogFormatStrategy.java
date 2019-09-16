package de.fau.cs.mad.carwatch.logger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.LogStrategy;

public class LogFormatStrategy implements FormatStrategy {

    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String NEW_LINE_REPLACEMENT = " <br> ";
    private static final String SEPARATOR = ";";

    @NonNull
    private final LogStrategy logStrategy;

    public LogFormatStrategy(@NonNull LogStrategy logStrategy) {
        this.logStrategy = logStrategy;
    }

    @Override
    public void log(int priority, @Nullable String tag, @NonNull String message) {

        long date = System.currentTimeMillis();

        StringBuilder builder = new StringBuilder();

        // machine-readable date/time
        builder.append(date);

        // tag
        builder.append(SEPARATOR);
        builder.append(tag);

        // message
        if (message.contains(NEW_LINE)) {
            // a new line would break the CSV format, so we replace it here
            message = message.replaceAll(NEW_LINE, NEW_LINE_REPLACEMENT);
        }

        builder.append(SEPARATOR);
        builder.append(message);

        // new line
        builder.append(NEW_LINE);

        logStrategy.log(priority, tag, builder.toString());
    }
}
