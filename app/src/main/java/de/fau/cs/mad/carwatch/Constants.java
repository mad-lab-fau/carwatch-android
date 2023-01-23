package de.fau.cs.mad.carwatch;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

public final class Constants {

    /**
     * Timer duration in minutes.
     */
    public static final int TIMER_DURATION = 10; // TODO change to 1

    public static final long[] VIBRATION_PATTERN = {0, 500, 1000};

    public static final DateTime[] MORNING_TIMES = {
            new LocalTime(5, 0).toDateTimeToday(),
            new LocalTime(12, 0).toDateTimeToday()
    };
    public static final DateTime[] EVENING_TIMES = {
            new LocalTime(20, 0).toDateTimeToday(),
            new LocalTime(5, 0).toDateTimeToday().plusDays(1)
    };

    public static final int FIRST_SALIVA_SAMPLE_OFFSET = 0;

    public static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    public static final String SETTINGS_NIGHT_DISPLAY_ACTIVATED = "night_display_activated";
    public static final String SETTINGS_ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

    public static final String PREF_FIRST_RUN_QR = "first_run_qr"; // boolean
    public static final String PREF_FIRST_RUN_SUBJECT_ID = "first_run_subject_id"; // boolean
    public static final String PREF_STUDY_NAME = "study_name"; // String
    public static final String PREF_SUBJECT_ID = "subject_id"; // String
    public static final String PREF_DAY_COUNTER = "day_counter"; // int (auto-incrementing)
    public static final String PREF_MORNING_TAKEN = "morning_taken"; // long (day)
    public static final String PREF_MORNING_ONGOING = "morning_ongoing"; // int (alarmId)
    public static final String PREF_EVENING_TAKEN = "evening_taken"; // long (day)
    public static final String PREF_SCANNED_BARCODES = "scanned_barcodes"; // String Set (barcode values)
    public static final String PREF_NIGHT_MODE_ENABLED = "night_mode"; // boolean
    public static final String PREF_SUBJECT_LIST = "subject_list"; //Array<String>
    public static final String PREF_SALIVA_TIMES = "saliva_times"; //Array<Integer>
    public static final String PREF_NUM_DAYS = "study_days"; // int
    public static final String PREF_HAS_EVENING = "has_evening"; // boolean
    public static final String PREF_SHARE_EMAIL_ADDRESS = "share_email_address"; // boolean

    public static final int REQUEST_CODE_ALARM_ACTIVITY = 0xF00;
    public static final int REQUEST_CODE_NOTIFICATION_ACCESS = 0x35;

    public static final int REQUEST_CODE_SCAN = 0xCAFE;

    public static final LocalTime DEFAULT_ALARM_TIME = new LocalTime(7, 0);
    public static final int DEFAULT_ALARM_ID = 1; // id of morning alarm

    public static final String EXTRA_ALARM_TIME = "extra_alarm_time";
    public static final String EXTRA_ALARM_ID = "extra_alarm_id";
    public static final String EXTRA_TIMER_ID = "extra_timer_id";
    public static final String EXTRA_SALIVA_ID = "extra_saliva_id";
    public static final String EXTRA_SOURCE = "extra_source";
    public static final String EXTRA_DAY_FINISHED = "day_finished";

    public static final int EXTRA_ALARM_ID_INITIAL = 0;
    public static final int EXTRA_TIMER_ID_INITIAL = 0;
    public static final int EXTRA_SALIVA_ID_INITIAL = 0;

    public static final int EXTRA_ALARM_ID_EVENING = 815;

    public static final int ALARM_OFFSET = Short.MAX_VALUE;
    public static final int ALARM_OFFSET_TIMER = Byte.MAX_VALUE;

    public static final String ACTION_STOP_ALARM = "Stop Alarm";

    public static final String BARCODE_TYPE_EAN8 = "ean8";
    public static final String BARCODE_TYPE_QR = "qr";

    // Actions that the Logger should log
    public static final String LOGGER_ACTION_APP_METADATA = "app_metadata";
    public static final String LOGGER_ACTION_PHONE_METADATA = "phone_metadata";
    public static final String LOGGER_ACTION_ALARM_SET = "alarm_set";
    public static final String LOGGER_ACTION_TIMER_SET = "timer_set";
    public static final String LOGGER_ACTION_ALARM_CANCEL = "alarm_cancel";
    public static final String LOGGER_ACTION_ALARM_RING = "alarm_ring";
    public static final String LOGGER_ACTION_ALARM_STOP = "alarm_stop";
    public static final String LOGGER_ACTION_ALARM_KILLALL = "alarm_killall";
    public static final String LOGGER_ACTION_EVENING_SALIVETTE = "evening_salivette";
    public static final String LOGGER_ACTION_BARCODE_SCAN_INIT = "barcode_scan_init";
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
    public static final String LOGGER_ACTION_NOTIFICATION_RECEIVED = "notification_received";
    public static final String LOGGER_ACTION_NOTIFICATION_REMOVED = "notification_removed";

    // Extras that can be added to the Logger
    public static final String LOGGER_EXTRA_ALARM_ID = "id"; // int
    public static final String LOGGER_EXTRA_ALARM_TIMESTAMP = "timestamp"; // long (utc timestamp)
    public static final String LOGGER_EXTRA_ALARM_SOURCE = "source"; // int
    public static final String LOGGER_EXTRA_SALIVA_ID = "saliva_id"; // int
    public static final String LOGGER_EXTRA_BARCODE_VALUE = "barcode_value"; // String
    public static final String LOGGER_EXTRA_OTHER_BARCODES = "other_barcodes"; // String Set
    public static final String LOGGER_EXTRA_DAY_COUNTER = "day_counter"; // int
    public static final String LOGGER_EXTRA_SUBJECT_ID = "subject_id"; // String
    public static final String LOGGER_EXTRA_APP_VERSION_CODE = "version_code"; // int
    public static final String LOGGER_EXTRA_APP_VERSION_NAME = "version_name"; // String
    public static final String LOGGER_EXTRA_PHONE_BRAND = "brand"; // String
    public static final String LOGGER_EXTRA_PHONE_MANUFACTURER = "manufacturer"; // String
    public static final String LOGGER_EXTRA_PHONE_MODEL = "model"; // String
    public static final String LOGGER_EXTRA_PHONE_VERSION_SDK_LEVEL = "version_sdk_level"; // int
    public static final String LOGGER_EXTRA_PHONE_VERSION_SECURITY_PATCH = "version_security_patch"; // String
    public static final String LOGGER_EXTRA_PHONE_VERSION_RELEASE = "version_release"; // String
    public static final String LOGGER_EXTRA_SCREEN_BRIGHTNESS = "screen_brightness"; // float
    public static final String LOGGER_EXTRA_DISPLAY_NIGHT_MODE = "display_night_mode"; // int
    public static final String LOGGER_EXTRA_NOTIFICATION_PACKAGE = "notification_package"; // String
    public static final String LOGGER_EXTRA_NOTIFICATION_ID = "notification_id"; // int
    public static final String LOGGER_EXTRA_NOTIFICATION_KEY = "notification_key"; // String
    public static final String LOGGER_EXTRA_NOTIFICATION_POST_TIME = "notification_post_time"; // int
    public static final String LOGGER_EXTRA_NOTIFICATION_CATEGORY = "notification_category"; // String
    public static final String LOGGER_EXTRA_NOTIFICATION_REMOVED_REASON = "notification_removed_reason"; // int

    /**
     * Constants used in QR-encoded study data
     */
    public static final String QR_PARSER_APP_ID = "CARWATCH";
    public static final String QR_PARSER_SEPARATOR = ";";
    public static final String QR_PARSER_SPECIFIER = ":";
    public static final String QR_PARSER_LIST_SEPARATOR = ",";
    public static final String QR_PARSER_PROPERTY_STUDY_NAME = "N";
    public static final String QR_PARSER_PROPERTY_STUDY_DAYS = "D";
    public static final String QR_PARSER_PROPERTY_PARTICIPANTS = "S";
    public static final String QR_PARSER_PROPERTY_SALIVA_TIMES = "T";
    public static final String QR_PARSER_PROPERTY_EVENING = "E";
    public static final String QR_PARSER_PROPERTY_CONTACT = "M";

}