package com.aureliennioche.mapp.restart;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.aureliennioche.mapp.step.StepService;

public class AutoStart extends BroadcastReceiver {
    String tag = "testing";  // this.getClass().getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w(tag, "Detected re-boot");
        if (intent != null) {
            String action = intent.getAction();
            if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                Log.w(tag, "Action is: " + action);
                // Restart the step service
                context.startForegroundService(new Intent(context, StepService.class));
            }
        }
    }
}
