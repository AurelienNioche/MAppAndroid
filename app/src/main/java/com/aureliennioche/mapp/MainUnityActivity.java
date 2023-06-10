package com.aureliennioche.mapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.unity3d.player.UnityPlayerActivity;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

public class MainUnityActivity extends UnityPlayerActivity {
    public static MainUnityActivity instance = null;

    public static String tag = "testing";

    StepDao stepDao;

    public int getStepNumberSinceMidnightThatDay(long timestamp) {
        Log.d(tag, "hello");
        // long timestamp = System.currentTimeMillis();

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
        Log.d(tag, "step number: " + stepNumber);
        return stepNumber;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Interface to the database
        stepDao = StepDatabase.getInstance(this.getApplicationContext()).stepDao();

        // For starting Unity
        Intent intent = getIntent();
        handleIntent(intent);

        instance = this;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
        setIntent(intent);
    }

    void handleIntent(Intent intent) {
        if(intent == null || intent.getExtras() == null) return;

        if(intent.getExtras().containsKey("doQuit"))
            if(mUnityPlayer != null) {
                finish();
            }
    }
}
