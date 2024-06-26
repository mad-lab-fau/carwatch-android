package de.fau.cs.mad.carwatch.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import de.fau.cs.mad.carwatch.db.converter.BooleanArrayConverter;
import de.fau.cs.mad.carwatch.db.converter.DateConverter;

/**
 * Backend Database
 */
@Database(entities = {Alarm.class}, version = 2, exportSchema = false)
@TypeConverters({DateConverter.class, BooleanArrayConverter.class})
public abstract class AlarmDatabase extends RoomDatabase {

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE alarm ADD COLUMN was_sample_taken INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static volatile AlarmDatabase sInstance;

    @VisibleForTesting
    private static final String DATABASE_NAME = "alarm-db";

    public abstract AlarmDao alarmModel();

    public static AlarmDatabase getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (AlarmDatabase.class) {
                if (sInstance == null) {
                    sInstance = Room.databaseBuilder(context.getApplicationContext(),
                            AlarmDatabase.class, DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .addMigrations(MIGRATION_1_2)
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
    private static final RoomDatabase.Callback roomDatabaseCallback = new RoomDatabase.Callback() {

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
