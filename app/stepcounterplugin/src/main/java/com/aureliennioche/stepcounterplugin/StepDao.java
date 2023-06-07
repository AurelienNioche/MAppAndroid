package com.aureliennioche.stepcounterplugin;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;


@Dao
public interface StepDao {
    @Query("SELECT * FROM steprecord ORDER BY timestamp ASC")
    List<StepRecord> getAll();

    @Query("SELECT * FROM steprecord WHERE timestamp > :ref ORDER BY timestamp ASC")
    List<StepRecord> getRecordsNewerThan(long ref);

    @Query("SELECT * FROM steprecord WHERE timestamp = (SELECT MAX(timestamp) FROM steprecord)")
    List<StepRecord> getLastRecord();

    @Query("SELECT * FROM steprecord WHERE timestamp = (SELECT MAX(timestamp) FROM steprecord WHERE :lowerBound <= timestamp < :upperBound )")
    List<StepRecord> getLastRecordOnInterval(long lowerBound, long upperBound);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStepRecord(StepRecord stepRecord);

    @Query("DELETE FROM steprecord")
    void nukeTable();
}
