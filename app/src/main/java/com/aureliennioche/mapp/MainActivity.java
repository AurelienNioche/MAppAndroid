package com.aureliennioche.mapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    static final String tag = "testing";  // this.getClass().getSimpleName();
    static StepService stepService;

    ActivityResultLauncher<Intent> startActivityIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {});


    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            checkPermissions();
                        }
                        else
                        {
                            finishAndRemoveTask();
                        }});

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            StepService.LocalBinder binder = (StepService.LocalBinder) service;
            stepService = binder.getService();
            Log.d(tag, "service bounded");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            stepService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        Log.d(tag, "MainActivity => Start the MainActivity");
        // Context context = this.getApplicationContext();


//        long ts = DateTime.now().getMillis();
//        DateTime dt = new DateTime(ts, DateTimeZone.getDefault());
//        String dayOfTheWeek = dt.dayOfWeek().getAsText();
//        String dayOfTheMonth = dt.dayOfMonth().getAsText();
//        String month = dt.monthOfYear().getAsText();
//        Log.d(tag, "date = "+ dayOfTheWeek + dayOfTheMonth + month);
        checkPermissions();
    }


    public void allPermissionsHaveBeenGranted() {

        Log.d(tag, "MainActivity => Starting the service if not already started");
        startForegroundService(new Intent(this.getApplication(), StepService.class));

        Intent intent = new Intent(this, StepService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        launchUnity();

//        IntentFilter filter = new IntentFilter("MAIN_UNITY_ACTIVITY_CALLBACK");

//        BroadcastReceiver receiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                //do something based on the intent's action
//                Log.d(tag,  "MainActivity => Received broadcast");
//                String callback = intent.getStringExtra("CALLBACK");
//                Log.d(tag, "Broadcast is " + callback);
//                if (Objects.equals(callback, "onStop")) {
//                    Status status = status
//                    // stepService.onStopApp();
//                }
//            }
//        };
//        registerReceiver(receiver, filter);
    }

    public void launchUnity() {

        Log.d(tag, "MainActivity => Launching the Unity activity");
        Intent intent = new Intent(MainActivity.this, MainUnityActivity.class);
        MainActivity.this.startActivity(intent);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIntent.launch(intent);
    }

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
    }

    // ----------------------------------------- //


    public void checkPermissions() {
        // Begin by requesting the notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION);
        } else {
            allPermissionsHaveBeenGranted();
        }
    }
}