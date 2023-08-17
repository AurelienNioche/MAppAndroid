package com.aureliennioche.mapp.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.aureliennioche.mapp.BuildConfig;
import com.aureliennioche.mapp.R;
import com.aureliennioche.mapp.config.Config;
import com.aureliennioche.mapp.database.MAppDatabase;
import com.aureliennioche.mapp.step.StepService;
import com.aureliennioche.mapp.dao.ChallengeDao;
import com.aureliennioche.mapp.dao.InteractionDao;
import com.aureliennioche.mapp.dao.ProfileDao;
import com.aureliennioche.mapp.dao.StatusDao;
import com.aureliennioche.mapp.dao.StepDao;


public class MainActivity extends AppCompatActivity {

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
            // Log.d(tag, "service bounded");
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
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        checkVersionAndCleanUpIfNecessary();
        checkPermissions();
    }

    private void checkVersionAndCleanUpIfNecessary() {

        final String PREFS_NAME = "MyPrefsFile";
        final String PREF_VERSION_NAME_KEY = "version_code";
        final String DOES_NOT_EXIST = "DOES_NOT_EXIST";

        // Log.d(tag, "Git hash: " + BuildConfig.VERSION_NAME);

        // Get current version code
        String currentVersionCode = BuildConfig.VERSION_NAME;

        // Get saved version code
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedVersionCode = prefs.getString(PREF_VERSION_NAME_KEY, DOES_NOT_EXIST);

        // Check for first run or upgrade
        if (currentVersionCode.equals(savedVersionCode)) {
            // Log.d(tag, "App already installed before");
            // This is just a normal run
            return;
        }
//        } else if (savedVersionCode.equals(DOES_NOT_EXIST)) {
//            // This is a new install (or the user cleared the shared preferences)
//            // Log.d(tag, "New install");
//        } else {
//            // Log.d(tag, "Upgrade");
//        }

        // Get a reference to the db interface
        MAppDatabase db = MAppDatabase.getInstance(this.getApplicationContext());

        // Only if upgrade or new install
        if (Config.eraseChallengeTableAfterUpdate) {
            // Log.d(tag, "Deleting previously existing tables");
            ChallengeDao challengeDao = db.rewardDao();
            challengeDao.nukeTable();
        }
        if (Config.eraseStepTableAfterUpdate) {
            StepDao stepDao = db.stepDao();
            stepDao.nukeTable();
        }
        if (Config.eraseStatusTableAfterUpdate) {
            StatusDao statusDao = db.statusDao();
            statusDao.nukeTable();
        }
        if (Config.eraseProfileTableAfterUpdate) {
            ProfileDao profileDao = db.profileDao();
            profileDao.nukeTable();
        }
        if (Config.eraseInteractionTableAfterUpdate) {
            InteractionDao interactionDao = db.interactionDao();
            interactionDao.nukeTable();
        }

        // Update the shared preferences with the current version code
        prefs.edit().putString(PREF_VERSION_NAME_KEY, currentVersionCode).apply();
    }

    public void allPermissionsHaveBeenGranted() {

        // Log.d(tag, "MainActivity => Starting the service if not already started");
        startForegroundService(new Intent(this.getApplication(), StepService.class));

        Intent intent = new Intent(this, StepService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        // StrictMode.enableDefaults(); // TODO: REMOVE FOR PRODUCTION

        launchUnity();
    }

    public void launchUnity() {

        // Log.d(tag, "MainActivity => Launching the Unity activity");
        Intent intent = new Intent(MainActivity.this, MainUnityActivity.class);
        MainActivity.this.startActivity(intent);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIntent.launch(intent);
    }

    public void onPause() {
        // Log.d(tag, "MainActivity => onPause");
        super.onPause();
    }

    public void onDestroy() {
        // Log.d(tag, "MainActivity => onDestroy");
        if (stepService != null) {
            // Detach the service connection.
            unbindService(connection);
        }
        super.onDestroy();
    }

    public void onResume() {
        super.onResume();
        // Log.d(tag, "MainActivity => onResume");
    }

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