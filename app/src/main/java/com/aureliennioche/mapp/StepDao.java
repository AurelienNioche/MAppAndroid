package com.aureliennioche.mapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;


@Dao
public interface StepDao {

    @Query("SELECT * FROM steprecord WHERE ts > :ref ORDER BY ts ASC")
    List<StepRecord> getRecordsNewerThan(long ref);

    @Query("SELECT * FROM steprecord WHERE ts = (SELECT MAX(ts) FROM steprecord)")
    List<StepRecord> getLastRecord();

    // Return a list of zero or one element
    @Query("SELECT * FROM steprecord WHERE ts = (SELECT MAX(ts) FROM steprecord WHERE ts >= :lowerBound AND ts < :upperBound)")
    List<StepRecord> getLastRecordOnInterval(long lowerBound, long upperBound);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStepRecord(StepRecord stepRecord);

    @Query("DELETE FROM steprecord WHERE ts >= :lowerBound AND ts < :upperBound " +
            "AND stepMidnight % 100 != 0")  // We don't want to erase when 100's are reached so, we know when the goal was reached
    void deleteRecordsOnInterval(long lowerBound, long upperBound);

    @Query("DELETE FROM steprecord WHERE ts < :bound")  // We don't want to erase when 100's are reached so, we know when the goal was reached
    void deleteRecordsOlderThan(long bound);

    default int getStepNumberSinceMidnightThatDay(long timestamp) {
        // Log.d(tag, "hello");
        // long timestamp = System.currentTimeMillis();

        DateTime dt = new DateTime(timestamp, DateTimeZone.getDefault());
        DateTime midnight = dt.withTimeAtStartOfDay();
        DateTime nextMidnight = midnight.plusDays(1);

        long midnightTimestamp = midnight.getMillis();
        long nextMidnightTimestamp = nextMidnight.getMillis();

        // Log.d(tag, "timezone ID:" + dt.getZone().getID());
        List<StepRecord> records = getLastRecordOnInterval(
                midnightTimestamp,
                nextMidnightTimestamp);
        int stepNumber = 0;
        if (records.size() > 0) {
            stepNumber = records.get(0).stepMidnight;
        }
        // Log.d(tag, "step number: " + stepNumber);
        return stepNumber;
    }

//    @Query("SELECT * FROM steprecord ORDER BY ts ASC")
//    List<StepRecord> getAll();
//
//    @Query("DELETE FROM steprecord")
//    void nukeTable();
}
