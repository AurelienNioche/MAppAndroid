package com.aureliennioche.mapp;

import android.os.SystemClock;
import android.util.Log;

import androidx.room.Dao;
import androidx.room.Ignore;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;


@Dao
public interface StepDao {

    @Ignore
    String tag = "testing";
    @Ignore
    public static final int MIN_DELAY_BETWEEN_TWO_RECORDS_MINUTES = 6;
    @Ignore
    public static final int KEEP_DATA_NO_LONGER_THAN_X_MONTH = 3;

    @Query("SELECT * FROM steprecord WHERE ts > :ref ORDER BY ts ASC")
    List<StepRecord> getRecordsNewerThan(long ref);

    @Query("SELECT * FROM steprecord WHERE ts = (SELECT MAX(ts) FROM steprecord)")
    List<StepRecord> getLastRecord();

    // Return a list of zero or one element
    @Query("SELECT * FROM steprecord WHERE ts = (SELECT MAX(ts) FROM steprecord WHERE ts >= :lowerBound AND ts < :upperBound)")
    List<StepRecord> getLastRecordOnInterval(long lowerBound, long upperBound);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(StepRecord stepRecord);

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

    default StepRecord recordNewSensorValue(int sensorValue)
    {
        Log.d(tag, "recordNewSensorValue => Let's record new stuff");

        long timestamp = System.currentTimeMillis();
        long lastBootTimestamp = timestamp - SystemClock.elapsedRealtime();

        DateTime dt = new DateTime(timestamp, DateTimeZone.getDefault());
        DateTime midnight = dt.withTimeAtStartOfDay();
        long midnightTimestamp = midnight.getMillis();

        int stepNumberSinceMidnight = 0; // Default if no recording, or no recording that day

        List<StepRecord> records = getLastRecord();
        // If there is some record
        if (records.size() > 0)
        {
            StepRecord ref = records.get(0);
            // If there is some record /for today/
            if (ref.ts > midnightTimestamp)
            {
                // If phone has been reboot in the between (leave some error margin),
                // Then take the meter reading as the number of steps done since the last log
                // So, steps since midnight is step since midnight from last log plus the meter reading
                if (lastBootTimestamp > ref.tsLastBoot + 500) { // 500: error margin
                    stepNumberSinceMidnight = ref.stepMidnight + sensorValue;
                }
                // If they were no boot, just consider the progression,
                // and add it to the previous log
                else
                {
                    stepNumberSinceMidnight = ref.stepMidnight
                            + (sensorValue - ref.stepLastBoot);
                }
            }
        }

        // Create new record
        StepRecord rec = new StepRecord(
                timestamp,
                lastBootTimestamp,
                sensorValue,
                stepNumberSinceMidnight
        );

        // Record new entry
        insert(rec);

        // Delete
        long bound = midnight.minusMonths(KEEP_DATA_NO_LONGER_THAN_X_MONTH).getMillis();
        deleteRecordsOlderThan(bound);

        // Delete older ones within a 6 min range (we assume we don't need data more than every 6 minutes)
        long lowerBound = midnight.minusMinutes(MIN_DELAY_BETWEEN_TWO_RECORDS_MINUTES).getMillis();
        lowerBound = Math.max(midnightTimestamp, lowerBound); // Bound the bound to midnight that dat
        deleteRecordsOnInterval(lowerBound, timestamp); // Upper bound is the timestamp of that recording

        return rec;
    }

//    @Query("SELECT * FROM steprecord ORDER BY ts ASC")
//    List<StepRecord> getAll();
//
    @Query("DELETE FROM steprecord")
    void nukeTable();

    @Query("SELECT COUNT(id) FROM steprecord")
    int getRowCount();

    @Query("SELECT * FROM steprecord")
    List<StepRecord> getAll();
}
