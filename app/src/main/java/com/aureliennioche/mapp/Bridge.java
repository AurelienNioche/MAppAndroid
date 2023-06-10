package com.aureliennioche.mapp;

import android.app.Activity;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

public class Bridge {
    public static final int MIN_DELAY_BETWEEN_TWO_RECORDS_MINUTES = 6;

    public static final int KEEP_DATA_NO_LONGER_THAN_X_MONTH = 3;
    public static final String tag = "testing";

    public static String getRecordNewerThanJsonFormat(Activity mainActivity, long timestamp) throws JsonProcessingException {

        // TODO: (OPTIONAL FOR NOW) delete older records, as they are already on the server
        StepDao stepDao = StepDatabase.getInstance(mainActivity.getApplicationContext()).stepDao();
        List<StepRecord> list = stepDao.getRecordsNewerThan(timestamp);
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(list);
    }

    public static int getStepNumberSinceMidnightThatDay(Activity mainActivity, long timestamp) {

        StepDao stepDao = StepDatabase.getInstance(mainActivity.getApplicationContext()).stepDao();

        DateTime dt = new DateTime(timestamp, DateTimeZone.getDefault());
        DateTime midnight = dt.withTimeAtStartOfDay();
        DateTime nextMidnight = midnight.plusDays(1);

        long midnightTimestamp = midnight.getMillis();
        long nextMidnightTimestamp = nextMidnight.getMillis();

        Log.d(tag, "timezone ID:" + dt.getZone().getID());
        List<StepRecord> records = stepDao.getLastRecordOnInterval(
                midnightTimestamp,
                nextMidnightTimestamp);
        int stepNumber = 0;
        if (records.size() > 0) {
            stepNumber = records.get(0).stepMidnight;
        }
        return stepNumber;
    }
}