package de.fau.cs.mad.carwatch;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

public final class Constants {

    public static final int NUM_DAYS = 7;

    /**
     * Timer duration in minutes.
     */
    public static final int TIMER_DURATION = 1;

    public static final int[] DELTA_HIDDEN_ALARMS = {21, 39};

    public static final long[] VIBRATION_PATTERN = {0, 500, 1000};

    public static final DateTime[] MORNING_TIMES = {new LocalTime(5, 0).toDateTimeToday(), new LocalTime(12, 0).toDateTimeToday()};
    public static final DateTime[] EVENING_TIMES = {new LocalTime(20, 0).toDateTimeToday(), new LocalTime(5, 0).toDateTimeToday().plusDays(1)};

    public static final int[] BARCODE_RANGE = {0, 30104};

    public static final String SHARE_EMAIL_ADDRESS = "CAR_Studie2019@gmx.de";

    /**
     * Time to the next saliva sample in minutes
     */
    public static final int[] SALIVA_TIMES = {0, 15, 15, 15, 15};

    public static final String KEY_ACTIVE_DAYS = "active_days";

    public static final String PREF_FIRST_RUN = "first_run"; // boolean
    public static final String PREF_SUBJECT_ID = "subject_id"; // int
    public static final String PREF_SNOOZE_DURATION = "snooze_duration"; // int (min)
    public static final String PREF_DAY_COUNTER = "day_counter"; // int (auto-incrementing)
    public static final String PREF_MORNING_TAKEN = "morning_taken"; // long (day)
    public static final String PREF_MORNING_ONGOING = "morning_ongoing"; // int (alarmId)
    public static final String PREF_EVENING_TAKEN = "evening_taken"; // long (day)
    public static final String PREF_SCANNED_BARCODES = "scanned_barcodes"; // String Set (barcode values)
    public static final String PREF_NIGHT_MODE_ENABLED = "night_mode"; // boolean

    public static final int REQUEST_CODE_ALARM = 0xBAD;
    public static final int REQUEST_CODE_ALARM_ACTIVITY = 0xF00;

    public static final int REQUEST_CODE_NEW_ALARM = 1;
    public static final int REQUEST_CODE_EDIT_ALARM = 2;

    public static final int REQUEST_CODE_SCAN = 0xCAFE;


    public static final String EXTRA_ALARM = "extra_alarm";
    public static final String EXTRA_ALARM_TIME = "extra_alarm_time";
    public static final String EXTRA_BUNDLE = "extra_bundle";
    public static final String EXTRA_EDIT = "extra_edit";
    public static final String EXTRA_DELETE = "extra_delete";
    public static final String EXTRA_ALARM_ID = "extra_alarm_id";
    public static final String EXTRA_TIMER_ID = "extra_timer_id";
    public static final String EXTRA_SALIVA_ID = "extra_saliva_id";
    public static final String EXTRA_SOURCE = "extra_source";

    public static final int EXTRA_ALARM_ID_SPONTANEOUS = 4711;
    public static final int EXTRA_ALARM_ID_DEFAULT = -1;
    public static final int EXTRA_TIMER_ID_DEFAULT = -1;
    public static final int EXTRA_SALIVA_ID_DEFAULT = 0;

    public static final int EXTRA_ALARM_ID_EVENING = 815;
    public static final int EXTRA_SALIVA_ID_EVENING = SALIVA_TIMES.length;

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
    public static final String LOGGER_ACTION_ALARM_KILLALL = "alarm_killall";
    public static final String LOGGER_ACTION_EVENING_SALIVETTE = "evening_salivette";
    public static final String LOGGER_ACTION_BARCODE_SCANNED = "barcode_scanned";
    public static final String LOGGER_ACTION_INVALID_BARCODE_SCANNED = "invalid_barcode_scanned";
    public static final String LOGGER_ACTION_DUPLICATE_BARCODE_SCANNED = "duplicate_barcode_scanned";
    public static final String LOGGER_ACTION_SPONTANEOUS_AWAKENING = "spontaneous_awakening";
    public static final String LOGGER_ACTION_LIGHTS_OUT = "lights_out";
    public static final String LOGGER_ACTION_DAY_FINISHED = "day_finished";
    public static final String LOGGER_ACTION_SERVICE_STARTED = "service_started";
    public static final String LOGGER_ACTION_SERVICE_STOPPED = "service_stopped";
    public static final String LOGGER_ACTION_SCREEN_OFF = "screen_off";
    public static final String LOGGER_ACTION_SCREEN_ON = "screen_on";
    public static final String LOGGER_ACTION_USER_PRESENT = "user_present";
    public static final String LOGGER_ACTION_PHONE_BOOT_INIT = "phone_boot_init";
    public static final String LOGGER_ACTION_PHONE_BOOT_COMPLETE = "phone_boot_complete";
    public static final String LOGGER_ACTION_SUBJECT_ID_SET = "subject_id_set";

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
    public static final String LOGGER_EXTRA_DAY_COUNTER = "day_counter"; // int
    public static final String LOGGER_EXTRA_SUBJECT_ID = "subject_id"; // String
    public static final String LOGGER_EXTRA_SUBJECT_CONDITION = "subject_condition"; // String


    public static final LocalTime DEFAULT_ALARM_TIME = new LocalTime(7, 0);

}
