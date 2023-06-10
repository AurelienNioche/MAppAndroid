package com.aureliennioche.mapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unity3d.player.UnityPlayerActivity;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

public class MainUnityActivity extends UnityPlayerActivity {
    public static MainUnityActivity instance = null;

    public static String tag = "testing";

    StepDao stepDao;
//
//    StepService stepService;

    public int getStepNumberSinceMidnightThatDay(long timestamp) {
        // Log.d(tag, "hello");
        // long timestamp = System.currentTimeMillis();

        DateTime dt = new DateTime(timestamp, DateTimeZone.getDefault());
        DateTime midnight = dt.withTimeAtStartOfDay();
        DateTime nextMidnight = midnight.plusDays(1);

        long midnightTimestamp = midnight.getMillis();
        long nextMidnightTimestamp = nextMidnight.getMillis();

        // Log.d(tag, "timezone ID:" + dt.getZone().getID());
        List<StepRecord> records = stepDao.getLastRecordOnInterval(
                midnightTimestamp,
                nextMidnightTimestamp);
        int stepNumber = 0;
        if (records.size() > 0) {
            stepNumber = records.get(0).stepMidnight;
        }
        // Log.d(tag, "step number: " + stepNumber);
        return stepNumber;
    }

    public String getRecordNewerThanJsonFormat(long timestamp) throws JsonProcessingException {

        // TODO: (OPTIONAL FOR NOW) delete older records, as they are already on the server
        List<StepRecord> list = stepDao.getRecordsNewerThan(timestamp);
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(list);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Interface to the database
        stepDao = StepDatabase.getInstance(this.getApplicationContext()).stepDao();

        // For starting Unity
        Intent intentUnityPlayer = getIntent();
        handleIntent(intentUnityPlayer);

        instance = this;
    }

    @Override
    protected void onDestroy() {

        Log.d(tag, "UnityActivity => on destroy");
        Intent intent = new Intent("MAIN_UNITY_ACTIVITY_CALLBACK");
        intent.putExtra("CALLBACK", "onDestroy");
        sendBroadcast(intent);

        instance = null;

        super.onDestroy();
    }

    @Override
    protected void onStop() {
        Log.d(tag, "UnityActivity => on stop");
        Intent intent = new Intent("MAIN_UNITY_ACTIVITY_CALLBACK");
        intent.putExtra("CALLBACK", "onStop");
        sendBroadcast(intent);
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(tag, "UnityActivity => on pause");
        Intent intent = new Intent("MAIN_UNITY_ACTIVITY_CALLBACK");
        intent.putExtra("CALLBACK", "onPause");
        sendBroadcast(intent);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(tag, "UnityActivity => on resume");
        Intent intent = new Intent("MAIN_UNITY_ACTIVITY_CALLBACK");
        intent.putExtra("CALLBACK", "onResume");
        sendBroadcast(intent);
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
