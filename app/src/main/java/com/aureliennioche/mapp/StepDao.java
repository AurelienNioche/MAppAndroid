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

    @Query("SELECT * FROM step WHERE ts > :ref ORDER BY ts ASC")
    List<Step> getRecordsNewerThan(long ref);

    @Query("SELECT * FROM step WHERE ts = (SELECT MAX(ts) FROM step)")
    List<Step> getLastRecord();

    // Return a list of zero or one element
    @Query("SELECT * FROM step WHERE ts = (SELECT MAX(ts) FROM step WHERE ts >= :lowerBound AND ts < :upperBound)")
    List<Step> getLastRecordOnInterval(long lowerBound, long upperBound);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Step step);

    @Query("DELETE FROM Step WHERE ts < :bound")
    void deleteRecordsOlderThan(long bound);

    default int getStepNumberSinceMidnightThatDay(long timestamp) {

        DateTime dt = new DateTime(timestamp, MainActivity.tz);
        DateTime midnight = dt.withTimeAtStartOfDay();
        DateTime nextMidnight = midnight.plusDays(1);

        long midnightTimestamp = midnight.getMillis();
        long nextMidnightTimestamp = nextMidnight.getMillis();

        List<Step> records = getLastRecordOnInterval(
                midnightTimestamp,
                nextMidnightTimestamp);
        int stepNumber = 0;
        if (records.size() > 0) {
            stepNumber = records.get(0).stepMidnight;
        }
        return stepNumber;
    }

    default Step recordNewSensorValue(
            int sensorValue) {

        long timestamp = System.currentTimeMillis();
        long lastBootTimestamp = timestamp - SystemClock.elapsedRealtime();

        long dayBegins = new DateTime(timestamp, MainActivity.tz).withTimeAtStartOfDay().getMillis();

        int stepNumberSinceMidnight = 0; // Default if no recording, or no recording that day

        List<Step> records = getLastRecord();
        // If there is some record
        if (records.size() > 0) {
            Step ref = records.get(0);
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
        Step rec = new Step();
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

    @Query("SELECT * FROM Step")
    List<Step> getAll();

    @Query("DELETE FROM Step")
    void nukeTable();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertIfNotExisting(List<Step> steps);
}
