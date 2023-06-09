package com.aureliennioche.mapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    String tag = "testing";  // this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(tag, "Start the MainActivity");
        launchUnity();

        // Erase everything first (we might want to do something else later on)
        // Log.d(tag, "Du passé faisons table rase");
        // stepDao.nukeTable();
//
//        Bridge bridge = new Bridge(this);
//
//        bridge.addFakeRecord(34);
//
//        bridge.addFakeRecord(38);
//
//        bridge.logRecords(bridge.getAllRecords());
//
//        // StepRecord record = bridge.GetLastRecordOnDay(DateTime.now());
//        // Long value = System.currentTimeMillis();
//
//        DateTime dt =DateTime.now(Bridge.TIMEZONE);
//        int nStep = bridge.getStepNumberSinceMidnightThatDay(dt);
//        Log.d(tag, "Number of steps since midnight = " + nStep);
//
//        Log.d(tag, "Testing recovering records");
//        List<StepRecord> records = bridge.getRecordsNewerThan(DateTime.now(Bridge.TIMEZONE).minusMinutes(5));
//        bridge.logRecords(records);
//
//        try {
//            Log.d(tag, "testing jsonify");
//            String out = bridge.getRecordNewerThanJsonFormat(DateTime.now(Bridge.TIMEZONE).minusMinutes(5));
//            Log.d(tag, out);
//            Log.d(tag, "done with json");
//        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
    }

    public void launchUnity() {

        Log.d(tag, "Launching the Unity activity");
        Intent intent = new Intent(MainActivity.this, MainUnityActivity.class);
        MainActivity.this.startActivity(intent);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIntent.launch(intent);
    }

    ActivityResultLauncher<Intent> startActivityIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {});
}