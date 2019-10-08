package de.fau.cs.mad.carwatch.db;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.List;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.db.converter.BooleanArrayConverter;
import de.fau.cs.mad.carwatch.db.converter.DateConverter;

@Entity(tableName = "alarms")
@TypeConverters({DateConverter.class, BooleanArrayConverter.class})
public class Alarm implements Parcelable {
    @Ignore
    private static final String TAG = Alarm.class.getSimpleName();

    // Class members

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "alarm_id")
    private int id;

    @ColumnInfo(name = "alarm_time")
    private DateTime time;

    @ColumnInfo(name = "hidden_delta")
    private int hiddenDelta;

    @ColumnInfo(name = "has_hidden_time")
    private boolean hasHiddenTime;

    @ColumnInfo(name = "alarm_active")
    private boolean active;

    // Must have length 7 (= Constants.NUM_DAYS)
    // i'th item being true means alarm is active on i'th day
    @ColumnInfo(name = "alarm_active_days")
    private boolean[] activeDays;

    public Alarm() {
        this(
                Constants.DEFAULT_ALARM_TIME.toDateTimeToday(),
                false,
                0,
                false,
                new boolean[]{false, true, true, true, true, true, false}
        );
    }

    // Getters/Setters
    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setTime(DateTime time) {
        this.time = time;
    }

    public DateTime getTime() {
        return time;
    }

    public void setHasHiddenTime(boolean hasHiddenTime) {
        this.hasHiddenTime = hasHiddenTime;
    }

    public boolean hasHiddenTime() {
        return hasHiddenTime;
    }

    public void setHiddenDelta(int hiddenDelta) {
        this.hiddenDelta = hiddenDelta;
        this.hasHiddenTime = true;
    }

    public int getHiddenDelta() {
        return hiddenDelta;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setActiveDays(boolean[] activeDays) {
        this.activeDays = activeDays;
    }

    public boolean[] getActiveDays() {
        return activeDays;
    }

    public boolean isRepeating() {
        // Iterate through active days to see if one is true
        boolean isRepeat = false;
        for (boolean bool : activeDays) {
            if (bool) isRepeat = true;
        }
        return isRepeat;
    }


    // Ignored Members

    // Used to get user-readable String representation of activeDays
    @Ignore
    private static final String[] daysOfWeek =
            {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    @Ignore
    public Alarm(DateTime time, boolean hasHiddenTime, int hiddenDelta, boolean active, boolean[] activeDays) {
        this.time = time;
        this.hasHiddenTime = hasHiddenTime;
        this.hiddenDelta = hiddenDelta;
        this.active = active;
        this.activeDays = activeDays;
    }

    /**
     * Get String of alarm ring time in 12 hour format
     */
    @Ignore
    public String getStringTime() {
        return this.time.toString("HH:mm");
    }

    @Ignore
    public String getStringOfActiveDays() {
        return getStringOfActiveDays(activeDays);
    }

    /**
     * Get a simple user-readable representation of activeDays
     *
     * @param activeDays boolean array of days alarm is active
     * @return a String of alarm's active days
     */
    @Ignore
    public static String getStringOfActiveDays(boolean[] activeDays) {
        // Build string based on which indices are true in activeDays
        StringBuilder builder = new StringBuilder();
        int activeCount = 0;
        for (int i = 0; i < daysOfWeek.length; i++) {
            if (activeDays[i]) {
                String formattedDay = daysOfWeek[i].substring(0, 3) + ", ";
                builder.append(formattedDay);
                activeCount++;
            }
        }

        if (activeCount == daysOfWeek.length) {
            return "everyday";
        } else if (activeCount == 0) {
            return "never";
        }

        boolean satInArray = activeDays[daysOfWeek.length - 2]; // "Saturday" in activeDays
        boolean sunInArray = activeDays[daysOfWeek.length - 1]; // "Sunday" in activeDays

        if (satInArray && sunInArray && activeCount == 2) {
            return "weekends";
        } else if (!satInArray && !sunInArray && activeCount == 5) {
            return "weekdays";
        }

        if (builder.length() > 1) {
            builder.setLength(builder.length() - 2);
        }

        return builder.toString();
    }

    /**
     * Get time until next alarm ring
     */
    @Ignore
    public DateTime getTimeToNextRing() {
        if (time.isBefore(DateTime.now())) { // alarm time has passed for today
            time = time.plusDays(1); // set alarm to ring tomorrow
        }

        return time;
    }

    /**
     * Get the time to ring in milliseconds since epoch for each day alarm is active
     *
     * @return a List of {@link DateTime} objects to the next alarm ring for each active day
     */
    @Ignore
    public List<DateTime> getTimeToWeeklyRings() {
        List<DateTime> weekRingTimes = new ArrayList<>();

        for (int i = 0; i < activeDays.length; i++) {
            if (activeDays[i]) { // if alarm is active on that day
                weekRingTimes.add(getCorrectRingDay(time.toLocalTime(), i));
            }
        }

        return weekRingTimes;
    }

    /**
     * Correctly get the next alarm ring day based on the day its active and current day of week
     *
     * @param alarmTime {@link LocalTime} since epoch of alarm time **today**
     * @param activeDay int of day alarm is active on
     * @return long of milliseconds since epoch of next alarm ring time on active day
     */
    @Ignore
    private DateTime getCorrectRingDay(LocalTime alarmTime, int activeDay) {
        // JodaTime DateTimeConstants count from 1 (Monday) to 7 (Sunday)
        activeDay += 1;

        DateTime currTime = DateTime.now();
        int currDay = currTime.getDayOfWeek();

        DateTime alarmDate = alarmTime.toDateTimeToday();

        // alarm time has passed for today
        if (alarmDate.isBefore(currTime) || currDay != activeDay) { // current day is not an active day
            if (activeDay > currDay) { // Alarm is active a later day of the week
                alarmDate = alarmDate.plusDays(activeDay - currDay);
            } else { // Have to move the alarm time to next week's active day
                alarmDate = alarmDate.plusDays(Constants.NUM_DAYS - currDay);
                alarmDate = alarmDate.plusDays(activeDay);
            }
        }
        return alarmDate;
    }

    @Ignore
    public DateTime getHiddenTime() {
        return getTime().minusMinutes(hiddenDelta);
    }

    // Parcelable implementation
    @Ignore
    public int describeContents() {
        return 0;
    }

    /**
     * Write all alarm contents to Parcel out
     */
    @Ignore
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(id);
        out.writeLong(DateConverter.toTimestamp(time));
        out.writeInt(hasHiddenTime ? 1 : 0);
        out.writeInt(hiddenDelta);
        out.writeBooleanArray(activeDays);
        out.writeInt(active ? 1 : 0);
    }

    @Ignore
    public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<Alarm>() {
        public Alarm createFromParcel(Parcel in) {
            return new Alarm(in);
        }

        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };

    /**
     * Construct alarm from Parcel of data written to using writeToParcel (above)
     */
    @Ignore
    private Alarm(Parcel in) {
        id = in.readInt();
        Long timestamp = in.readLong();
        time = DateConverter.toDate(timestamp);
        hasHiddenTime = in.readInt() != 0;
        hiddenDelta = in.readInt();
        activeDays = new boolean[Constants.NUM_DAYS];
        in.readBooleanArray(activeDays);
        active = in.readInt() != 0;
    }


    @NonNull
    @Ignore
    @Override
    public String toString() {
        String ret = "Alarm <" + getId() + "> next alarm: " + getTimeToNextRing().toString("HH:mm") + " [" + isActive() + "]";
        if (hasHiddenTime()) {
            ret += ", hidden: [" + getHiddenTime() + "]";
        }

        return ret;
    }
}
