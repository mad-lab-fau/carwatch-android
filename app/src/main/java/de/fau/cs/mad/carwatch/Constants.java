package de.fau.cs.mad.carwatch;

import org.joda.time.LocalTime;

public class Constants {

    public static final String KEY_ALARM_TIME = "alarm_time";
    public static final String KEY_ALARM_ENABLED = "alarm_enabled";

    public static final int REQUEST_CODE_ALARM = 0xBAD;
    public static final int REQUEST_CODE_ALARM_ACTIVITY = 0xF00;

    public static final LocalTime DEFAULT_ALARM_TIME = new LocalTime(7, 0);
}
