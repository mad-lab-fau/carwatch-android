package de.fau.cs.mad.carwatch.db;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import static androidx.room.OnConflictStrategy.IGNORE;
import static androidx.room.OnConflictStrategy.REPLACE;


/**
 * Dao to interact with database at the lowest level
 */
@Dao
public interface AlarmDao {
    @Query("SELECT * FROM alarm WHERE alarm_is_fixed = 1 ORDER BY alarm_time ASC")
    LiveData<List<Alarm>> getFixedAlarms();

    @Query("select * from alarm")
    LiveData<Alarm> getAlarm();

    @Query("SELECT * FROM alarm WHERE alarm_id=:id")
    Alarm getById(int id);

    @Insert(onConflict = IGNORE)
    void insert(Alarm alarm);

    @Update
    void update(Alarm alarm);

    @Delete
    void delete(Alarm alarm);

    @Query("UPDATE alarm set alarm_active=:active where alarm_id=:id")
    void updateActive(int id, boolean active);

    @Insert(onConflict = REPLACE)
    void insertOrReplaceAlarm(Alarm alarm);
}
