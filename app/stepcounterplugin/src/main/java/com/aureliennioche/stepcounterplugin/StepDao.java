package com.aureliennioche.stepcounterplugin;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;


@Dao
public interface StepDao {
    @Query("SELECT * FROM steprecord ORDER BY ts ASC")
    List<StepRecord> getAll();

    @Query("SELECT * FROM steprecord WHERE ts > :ref ORDER BY ts ASC")
    List<StepRecord> getRecordsNewerThan(long ref);

    @Query("SELECT * FROM steprecord WHERE ts = (SELECT MAX(ts) FROM steprecord)")
    List<StepRecord> getLastRecord();

    @Query("SELECT * FROM steprecord WHERE ts = (SELECT MAX(ts) FROM steprecord WHERE ts >= :lowerBound AND ts < :upperBound)")
    List<StepRecord> getLastRecordOnInterval(long lowerBound, long upperBound);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStepRecord(StepRecord stepRecord);

    @Query("DELETE FROM steprecord WHERE ts >= :lowerBound AND ts < :upperBound")
    void deleteRecordsOnInterval(long lowerBound, long upperBound);

    @Query("DELETE FROM steprecord")
    void nukeTable();
}
