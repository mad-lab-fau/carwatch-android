package de.fau.cs.mad.carwatch;

import org.joda.time.LocalTime;

public class Constants {

    public static final String KEY_ALARM_TIME = "alarm_time";
    public static final String KEY_ALARM_ENABLED = "alarm_enabled";
    public static final String KEY_ACTIVE_DAYS = "active_days";

    public static final String PREF_SUBJECT_ID = "subject_id";

    public static final int REQUEST_CODE_ALARM = 0xBAD;
    public static final int REQUEST_CODE_ALARM_ACTIVITY = 0xF00;


    public static final int REQUEST_CODE_NEW_ALARM = 1;
    public static final int REQUEST_CODE_EDIT_ALARM = 2;


    public static final String EXTRA_ALARM = "extra_alarm";
    public static final String EXTRA_BUNDLE = "extra_bundle";
    public static final String EXTRA_EDIT = "extra_edit";
    public static final String EXTRA_DELETE = "extra_delete";
    public static final String EXTRA_ID = "extra_id";

    public static final String ACTION_SNOOZE_ALARM = "Snooze Alarm";
    public static final String ACTION_STOP_ALARM = "Stop Alarm";



    public static final LocalTime DEFAULT_ALARM_TIME = new LocalTime(7, 0);

}
