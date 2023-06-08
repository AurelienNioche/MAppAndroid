package com.aureliennioche.stepcounterplugin;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Bridge {
    public static DateTimeZone TIMEZONE = DateTimeZone.forID("Europe/London");
    public static long MIN_DELAY_BETWEEN_TWO_RECORDS = TimeUnit.MINUTES.toMillis(6);

    String tag = "testing"; // this.getClass().getSimpleName()
    Activity mainActivity;
    StepDao stepDao;

    public Bridge (Activity mainActivity) {
        this.mainActivity = mainActivity;
        stepDao = StepDatabase.getInstance(mainActivity.getApplicationContext()).stepDao();
    }

    public void launchService() {
        Context context = mainActivity.getApplicationContext();
        if (isServiceAlive(context, StepService.class)) {
            Log.d(tag, "Service is already running");
        } else {
            Log.d(tag, "Creating the service");
            Intent intent = new Intent(context, StepService.class);
            context.startForegroundService(intent);

            Log.d(tag, "The service is supposed to be running now");
        }
    }
    @SuppressWarnings("deprecation")
    public static boolean isServiceAlive(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public List<StepRecord> getRecordsNewerThan(DateTime dateTime) {

        long ref = dateTime.getMillis();
        return stepDao.getRecordsNewerThan(ref);
    }

    public String getRecordNewerThanJsonFormat(DateTime dateTime) throws JsonProcessingException {

        // TODO: (OPTIONAL FOR NOW) delete older records, as they are already on the server

        List<StepRecord> list = getRecordsNewerThan(dateTime);
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(list);
    }

    public int getStepNumberSinceMidnightThatDay(DateTime dateTime) {
        DateTime midnight = dateTime.withTimeAtStartOfDay();
        DateTime nextMidnight = midnight.plusDays(1);

        long midnightTimestamp = midnight.getMillis();
        long nextMidnightTimestamp = nextMidnight.getMillis();

        Log.d(tag, "timezone ID:" + dateTime.getZone().getID());
        List<StepRecord> records = stepDao.getLastRecordOnInterval(
                midnightTimestamp,
                nextMidnightTimestamp);
        int stepNumber = 0;
        if (records.size() > 0) {
            stepNumber = records.get(0).stepMidnight;
        }
        return stepNumber;
    }

//    // --------------------------------------------------------------- //
//    // For debugging
//    // --------------------------------------------------------------- //
//
//    public void addFakeRecord(int sensorValue) {
//
//        long timestamp = System.currentTimeMillis();
//        long lastBootTimestamp = timestamp - SystemClock.elapsedRealtime();
//
//        DateTime dt = new DateTime(timestamp, TIMEZONE);
//        DateTime midnight = dt.withTimeAtStartOfDay();
//        long midnightTimestamp = midnight.getMillis();
//
//        int stepNumberSinceMidnight = 0; // Default if no recording, or no recording that day
//
//        List<StepRecord> records = stepDao.getLastRecord();
//        // If there is some record
//        if (records.size() > 0)
//        {
//            StepRecord ref = records.get(0);
//            // If there is some record /for today/
//            if (ref.ts > midnightTimestamp)
//            {
//                // If phone has been reboot in the between (leave some error margin),
//                // Then take the meter reading as the number of steps done since the last log
//                // So, steps since midnight is step since midnight from last log plus the meter reading
//                if (lastBootTimestamp > ref.tsLastBoot + 500) { // 500: error margin
//                    stepNumberSinceMidnight = ref.stepMidnight + sensorValue;
//                }
//                // If they were no boot, just consider the progression,
//                // and add it to the previous log
//                else
//                {
//                    stepNumberSinceMidnight = ref.stepMidnight
//                            + (sensorValue - ref.stepLastBoot);
//                }
//            }
//        }
//
//        // Record new entry
//        Log.d(tag, "Let's record new stuff");
//        stepDao.insertStepRecord(new StepRecord(
//                timestamp,
//                lastBootTimestamp,
//                sensorValue,
//                stepNumberSinceMidnight
//        ));
//
//        // Delete older ones within a 6 min range
//        long lowerBound = Math.max(midnightTimestamp, timestamp - MIN_DELAY_BETWEEN_TWO_RECORDS);
//        stepDao.deleteRecordsOnInterval(lowerBound, timestamp); // Upper bound is the timestamp of that recording
//    }
//
//    public List<StepRecord> getAllRecords() {
//        return stepDao.getAll();
//    }
//
//    public void logRecords(List<StepRecord> stepRecords) {
//        Log.d(tag, "Data base records are:");
//        for (StepRecord r : stepRecords) {
//            DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
//
//            DateTime dt = new DateTime(r.ts, TIMEZONE);
//            String timestamp = fmt.print(dt);
//
//            DateTime dtLb = new DateTime(r.tsLastBoot, TIMEZONE);
//            String lastBootTimestamp = fmt.print(dtLb);
//
//            String stepMid = String.valueOf(r.stepMidnight);
//            String stepBoot = String.valueOf(r.stepLastBoot);
//
//            Log.d(tag, "[" + timestamp + " | Last boot: "+ lastBootTimestamp +"] Steps since midnight: " + stepMid + "; Steps since last boot: " + stepBoot);
//        }
//    }
}