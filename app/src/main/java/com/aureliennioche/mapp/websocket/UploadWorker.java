package com.aureliennioche.mapp.websocket;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class UploadWorker extends Worker {
    public UploadWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        WebSocketClient ws = WebSocketClient.getInstance();
        ws.syncServer();
        // Indicate whether the work finished successfully with the Result
        return Result.success();
    }
}
