package com.aureliennioche.mapp;

import android.os.SystemClock;
import android.util.Log;

import androidx.room.Dao;
import androidx.room.Ignore;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.joda.time.DateTime;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Dao
public interface StepDao {

    @Ignore
    String tag = "testing";

    @Query("SELECT * FROM step_record WHERE ts > :ref ORDER BY ts ASC")
    List<StepRecord> getRecordsNewerThan(long ref);

    @Query("SELECT * FROM step_record WHERE ts = (SELECT MAX(ts) FROM step_record)")
    List<StepRecord> getLastRecord();

    // Return a list of zero or one element
    @Query("SELECT * FROM step_record WHERE ts = (SELECT MAX(ts) FROM step_record WHERE ts >= :lowerBound AND ts < :upperBound)")
    List<StepRecord> getLastRecordOnInterval(long lowerBound, long upperBound);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(StepRecord stepRecord);

    @Query("DELETE FROM step_record WHERE ts >= :lowerBound AND ts < :upperBound " +
            "AND stepMidnight % 100 != 0")  // We don't want to erase when 100's are reached so, we know when the goal was reached
    void deleteRecordsOnInterval(long lowerBound, long upperBound);

    @Query("DELETE FROM step_record WHERE ts < :bound")
    void deleteRecordsOlderThan(long bound);

    default int getStepNumberSinceMidnightThatDay(long timestamp) {

        DateTime dt = new DateTime(timestamp, MainActivity.tz);
        DateTime midnight = dt.withTimeAtStartOfDay();
        DateTime nextMidnight = midnight.plusDays(1);

        long midnightTimestamp = midnight.getMillis();
        long nextMidnightTimestamp = nextMidnight.getMillis();

        List<StepRecord> records = getLastRecordOnInterval(
                midnightTimestamp,
                nextMidnightTimestamp);
        int stepNumber = 0;
        if (records.size() > 0) {
            stepNumber = records.get(0).stepMidnight;
        }
        return stepNumber;
    }

    default StepRecord recordNewSensorValue(
            int sensorValue) {

        long timestamp = System.currentTimeMillis();
        long lastBootTimestamp = timestamp - SystemClock.elapsedRealtime();

        long dayBegins = new DateTime(timestamp, MainActivity.tz).withTimeAtStartOfDay().getMillis();

        int stepNumberSinceMidnight = 0; // Default if no recording, or no recording that day

        List<StepRecord> records = getLastRecord();
        // If there is some record
        if (records.size() > 0) {
            StepRecord ref = records.get(0);
            // If there is some record /for today/
            if (ref.ts > dayBegins) {
                // If phone has been reboot in the between (leave some error margin),
                // Then take the meter reading as the number of steps done since the last log
                // So, steps since midnight is step since midnight from last log plus the meter reading
                if (lastBootTimestamp > ref.tsLastBoot + 500) { // 500: error margin
                    stepNumberSinceMidnight = ref.stepMidnight + sensorValue;
                }
                // If they were no boot, just consider the progression,
                // and add it to the previous log
                else {
                    stepNumberSinceMidnight = ref.stepMidnight
                            + (sensorValue - ref.stepLastBoot);
                }
            }
        }

        // Create new record
        StepRecord rec = new StepRecord();
        rec.ts = timestamp;
        rec.tsLastBoot = lastBootTimestamp;
        rec.stepLastBoot = sensorValue;
        rec.stepMidnight = stepNumberSinceMidnight;

        // Record new entry
        insert(rec);

        Log.d(tag, "recordNewSensorValue => step midnight " + rec.stepMidnight);

        // Delete
        long bound = dayBegins - TimeUnit.DAYS.toMillis(ConfigAndroid.keepDataNoLongerThanXdays);
        deleteRecordsOlderThan(bound);

        // Delete older ones within a X min range (we assume we don't need data more than every X minutes)
        // long lowerBound = dayBegins - TimeUnit.MINUTES.toMillis(ConfigAndroid.minDelayBetweenTwoRecords);
        // lowerBound = Math.max(dayBegins, lowerBound); // Bound the bound to midnight that day
        // deleteRecordsOnInterval(lowerBound, timestamp); // Upper bound is the timestamp of that recording

        return rec;
    }

    @Query("SELECT * FROM step_record")
    List<StepRecord> getAll();

    @Query("DELETE FROM step_record")
    void nukeTable();
}
