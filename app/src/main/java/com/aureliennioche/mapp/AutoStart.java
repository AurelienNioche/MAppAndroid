package com.aureliennioche.mapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoStart extends BroadcastReceiver {
    String tag = this.getClass().getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.w(tag, "Detected re-boot, yeah!!");
        if (intent != null) {
            String action = intent.getAction();
            {
                if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                    Log.w(tag, "Action is: " + action);
                    context.startForegroundService(new Intent(context, StepService.class));
                }
            }
        }
    }
}
