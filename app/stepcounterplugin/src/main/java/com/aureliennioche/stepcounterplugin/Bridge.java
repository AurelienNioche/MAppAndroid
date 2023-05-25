package com.aureliennioche.stepcounterplugin;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

public class Bridge {

    String tag = this.getClass().getSimpleName();
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

    public int numberOfStepSinceLastBoot() {
        int stepNumber = 0;
        List<StepRecord>stepRecords = stepDao.getAll();
        if (stepRecords.size() > 0 ) {
            StepRecord lastStepRecord = stepRecords.get(0);
            stepNumber = lastStepRecord.stepNumber;
        } else {
            Log.w(tag, "Empty database");
        }
        Log.d(tag, "Step number is " + stepNumber);
        return stepNumber;
    }
}