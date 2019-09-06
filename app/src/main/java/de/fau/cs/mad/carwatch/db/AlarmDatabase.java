package de.fau.cs.mad.carwatch.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import de.fau.cs.mad.carwatch.db.converter.BooleanArrayConverter;
import de.fau.cs.mad.carwatch.db.converter.DateConverter;

/**
 * Backend Database
 */
@Database(entities = {Alarm.class}, version = 1, exportSchema = false)
@TypeConverters({DateConverter.class, BooleanArrayConverter.class})
public abstract class AlarmDatabase extends RoomDatabase {

    private static AlarmDatabase sInstance;

    @VisibleForTesting
    public static final String DATABASE_NAME = "alarm-db";

    public abstract AlarmDao alarmModel();

    public static AlarmDatabase getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (AlarmDatabase.class) {
                if (sInstance == null) {
                    sInstance = Room.databaseBuilder(context.getApplicationContext(),
                            AlarmDatabase.class, DATABASE_NAME)
                            .fallbackToDestructiveMigration() // TODO Add Proper Migration
                            .addCallback(roomDatabaseCallback)
                            .build();
                }
            }
        }
        return sInstance;
    }

    /**
     * Override the onCreate method to populate the database.
     */
    private static RoomDatabase.Callback roomDatabaseCallback = new RoomDatabase.Callback() {

        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
        }
    };
}
