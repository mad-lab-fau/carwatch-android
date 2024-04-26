package de.fau.cs.mad.carwatch.db;

import static de.fau.cs.mad.carwatch.Constants.EXTRA_ALARM_ID_INITIAL;
import static de.fau.cs.mad.carwatch.Constants.EXTRA_SALIVA_ID_INITIAL;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import org.joda.time.DateTime;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.db.converter.BooleanArrayConverter;
import de.fau.cs.mad.carwatch.db.converter.DateConverter;

@Entity(tableName = "alarm")
@TypeConverters({DateConverter.class, BooleanArrayConverter.class})
public class Alarm implements Parcelable {
    @Ignore
    private static final String TAG = Alarm.class.getSimpleName();

    // Class members

    @PrimaryKey()
    @ColumnInfo(name = "alarm_id")
    private int id;

    @ColumnInfo(name = "saliva_id")
    private int salivaId;

    @ColumnInfo(name = "alarm_time")
    private DateTime time;

    @ColumnInfo(name = "alarm_active")
    private boolean active;

    @ColumnInfo(name = "alarm_is_fixed")
    private boolean isFixed;

    @ColumnInfo(name = "was_sample_taken")
    private boolean wasSampleTaken;

    public Alarm() {
        this(
                Constants.DEFAULT_ALARM_TIME.toDateTimeToday(),
                false,
                false,
                EXTRA_ALARM_ID_INITIAL,
                EXTRA_SALIVA_ID_INITIAL,
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

    public void setSalivaId(int salivaId) {
        this.salivaId = salivaId;
    }

    public int getSalivaId() {
        return this.salivaId;
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

    public void setActive(boolean active) { this.active = active; }

    public boolean isFixed() { return isFixed; }

    public void setIsFixed(boolean isFixed) { this.isFixed = isFixed; }

    public boolean wasSampleTaken() { return wasSampleTaken; }

    public void setWasSampleTaken(boolean wasSampleTaken) { this.wasSampleTaken = wasSampleTaken; }

    // Ignored Members
    @Ignore
    public Alarm(DateTime time, boolean active, boolean isFixed, int id, int salivaId, boolean wasSampleTaken) {
        this.time = time;
        this.active = active;
        this.isFixed = isFixed;
        this.id = id;
        this.salivaId = salivaId;
        this.wasSampleTaken = wasSampleTaken;
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
        out.writeInt(isFixed ? 1 : 0);
        out.writeInt(wasSampleTaken ? 1 : 0);
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
        isFixed = in.readInt() != 0;
        wasSampleTaken = in.readInt() != 0;
    }


    @NonNull
    @Ignore
    @Override
    public String toString() {
        return "Alarm <" + getId() + "> next alarm: " + getTimeToNextRing().toString("HH:mm") + " [" + isActive() + "]";
    }
}
