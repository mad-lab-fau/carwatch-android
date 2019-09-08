package de.fau.cs.mad.carwatch.db.converter;

import androidx.room.TypeConverter;

import org.joda.time.DateTime;

public class DateConverter {

    @TypeConverter
    public static DateTime toDate(Long timestamp) {
        return timestamp == null ? null : new DateTime(timestamp.longValue());
    }

    @TypeConverter
    public static Long toTimestamp(DateTime date) {
        return date == null ? null : date.getMillis();
    }
}
