package de.fau.cs.mad.carwatch;

import org.joda.time.LocalTime;

public final class Constants {

    public static final int NUM_DAYS = 7;

    /**
     * Timer duration in minutes.
     */
    public static final int TIMER_DURATION = 1;

    public static final int[] DELTA_HIDDEN_ALARMS = {21, 39};

    public static final long[] VIBRATION_PATTERN = {0, 500, 1000};

    /**
     * Time to the next saliva sample in minutes
     */
    public static final int[] SALIVA_TIMES = {0, 1, 1, 1};

    public static final String KEY_ACTIVE_DAYS = "active_days";

    public static final String PREF_FIRST_RUN = "first_run";
    public static final String PREF_SUBJECT_ID = "subject_id";
    public static final String PREF_SNOOZE_DURATION = "snooze_duration";
    public static final String PREF_DAY_ID = "day_id";

    public static final int REQUEST_CODE_ALARM = 0xBAD;
    public static final int REQUEST_CODE_ALARM_ACTIVITY = 0xF00;


    public static final int REQUEST_CODE_NEW_ALARM = 1;
    public static final int REQUEST_CODE_EDIT_ALARM = 2;


    public static final String EXTRA_ALARM = "extra_alarm";
    public static final String EXTRA_BUNDLE = "extra_bundle";
    public static final String EXTRA_EDIT = "extra_edit";
    public static final String EXTRA_DELETE = "extra_delete";
    public static final String EXTRA_ALARM_ID = "extra_alarm_id";
    public static final String EXTRA_TIMER_ID = "extra_timer_id";
    public static final String EXTRA_SALIVA_ID = "extra_saliva_id";
    public static final String EXTRA_SOURCE = "extra_source";

    public static final int EXTRA_ALARM_ID_DEFAULT = -1;
    public static final int EXTRA_TIMER_ID_DEFAULT = -1;
    public static final int EXTRA_SALIVA_ID_DEFAULT = 0;
    public static final int ALARM_OFFSET = Short.MAX_VALUE;
    public static final int ALARM_OFFSET_TIMER = Byte.MAX_VALUE;

    public static final String ACTION_SNOOZE_ALARM = "Snooze Alarm";
    public static final String ACTION_STOP_ALARM = "Stop Alarm";


    // Actions that the Logger should log
    public static final String LOGGER_ACTION_ALARM_SET = "alarm_set";
    public static final String LOGGER_ACTION_ALARM_CANCEL = "alarm_cancel";
    public static final String LOGGER_ACTION_ALARM_RING = "alarm_ring";
    public static final String LOGGER_ACTION_ALARM_SNOOZE = "alarm_snooze";
    public static final String LOGGER_ACTION_ALARM_STOP = "alarm_stop";
    public static final String LOGGER_ACTION_BARCODE_SCANNED = "barcode_scanned";
    public static final String LOGGER_ACTION_DAY_FINISHED = "day_finished";

    // Extras that can be added to the Logger
    public static final String LOGGER_EXTRA_ALARM_ID = "id"; // int
    public static final String LOGGER_EXTRA_ALARM_TIMESTAMP = "timestamp"; // long
    public static final String LOGGER_EXTRA_ALARM_IS_HIDDEN = "is_hidden"; // boolean
    public static final String LOGGER_EXTRA_ALARM_HIDDEN_TIMESTAMP = "timestamp_hidden"; // long
    public static final String LOGGER_EXTRA_ALARM_IS_REPEATING = "is_repeating"; // boolean
    public static final String LOGGER_EXTRA_ALARM_REPEATING_DAYS = "repeating_days"; // boolean[]
    public static final String LOGGER_EXTRA_ALARM_SNOOZE_DURATION = "snooze_duration"; // int
    public static final String LOGGER_EXTRA_ALARM_SOURCE = "source"; // int
    public static final String LOGGER_EXTRA_SALIVA_ID = "saliva_id"; // int
    public static final String LOGGER_EXTRA_BARCODE_VALUE = "barcode_value"; // String
    public static final String LOGGER_EXTRA_DAY_ID = "day_id"; // int


    public static final LocalTime DEFAULT_ALARM_TIME = new LocalTime(7, 0);

}
