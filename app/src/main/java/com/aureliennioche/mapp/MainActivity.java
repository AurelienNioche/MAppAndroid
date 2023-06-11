package com.aureliennioche.mapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    static final String tag = "testing";  // this.getClass().getSimpleName();

    static StepService stepService;

//    public static void sayHello() {
//        Log.d(tag, "hello");
//    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            StepService.LocalBinder binder = (StepService.LocalBinder) service;
            stepService = binder.getService();
            Log.d(tag, "service bounded");
            // now you have the instance of service.
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            stepService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(tag, "MainActivity => Start the MainActivity");
        // Context context = this.getApplicationContext();
        Log.d(tag, "MainActivity => Starting the service if not already started");
        startForegroundService(new Intent(this.getApplication(), StepService.class));

        Intent intent = new Intent(this, StepService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

//        Intent intent = new Intent("your_action_name");
//        sendBroadcast(intent);

        launchUnity();

        IntentFilter filter = new IntentFilter("MAIN_UNITY_ACTIVITY_CALLBACK");

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //do something based on the intent's action
                Log.d(tag,  "MainActivity => Received broadcast");
                String callback = intent.getStringExtra("CALLBACK");
                Log.d(tag, "Broadcast is " + callback);
                if (Objects.equals(callback, "onResume")) {
                    stepService.Tamere();
                }
            }
        };
        registerReceiver(receiver, filter);
    }

    public void launchUnity() {

        Log.d(tag, "MainActivity => Launching the Unity activity");
        Intent intent = new Intent(MainActivity.this, MainUnityActivity.class);
        MainActivity.this.startActivity(intent);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIntent.launch(intent);
    }

    ActivityResultLauncher<Intent> startActivityIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {});

    public void onPause() {
        Log.d(tag, "MainActivity => onPause");
        super.onPause();
    }

    public void onDestroy() {
        Log.d(tag, "MainActivity => onDestroy");
        if (stepService != null) {
            // Detach the service connection.
            unbindService(connection);
        }

        super.onDestroy();
    }

    public void onResume() {
        super.onResume();
        Log.d(tag, "MainActivity => onResume");
        // launchUnity();
    }
}