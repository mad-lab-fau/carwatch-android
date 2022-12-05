package de.fau.cs.mad.carwatch.db;

import android.content.Context;
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
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.db.converter.BooleanArrayConverter;
import de.fau.cs.mad.carwatch.db.converter.DateConverter;

@Entity(tableName = "alarm")
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

    @ColumnInfo(name = "alarm_active")
    private boolean active;

    public Alarm() {
        this(
                Constants.DEFAULT_ALARM_TIME.toDateTimeToday(),
                false
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


    // Ignored Members
    @Ignore
    public Alarm(DateTime time, boolean active) {
        this.time = time;
        this.active = active;
    }

    /**
     * Get String of alarm ring time in 12 hour format
     */
    @Ignore
    public String getStringTime() {
        return this.time.toString("HH:mm");
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
        active = in.readInt() != 0;
    }


    @NonNull
    @Ignore
    @Override
    public String toString() {
        return "Alarm <" + getId() + "> next alarm: " + getTimeToNextRing().toString("HH:mm") + " [" + isActive() + "]";
    }
}
