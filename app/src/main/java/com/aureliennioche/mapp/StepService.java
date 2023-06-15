package com.aureliennioche.mapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.List;

public class StepService extends Service implements SensorEventListener {
    private static final int ONGOING_NOTIFICATION_ID = -1;
    private static final String NOTIFICATION_CHANNEL_BACKGROUND_TASK_ID = "NOTIFICATION_CHANNEL_BACKGROUND_TASK_ID";
    private static final String NOTIFICATION_CHANNEL_OBJ_REACHED_ID = "NOTIFICATION_CHANNEL_OBJ_REACHED_ID";

    public static final String tag = "testing";
    // public boolean appVisibleOnScreen;
    SensorManager sensorManager;
    StepDao stepDao;
    RewardDao rewardDao;

    // Binder given to clients.
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        StepService getService() {
            // Return this instance of LocalService so clients can call public methods.
            return StepService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.d(tag, "onStartCommand => Creating the service");
        stepDao = StepDatabase.getInstance(this.getApplicationContext()).stepDao();
        rewardDao = RewardDatabase.getInstance(this.getApplicationContext()).rewardDao();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(tag, "onStartCommand => Service starting");

        // Create notification channels
        createNotificationChannelBackgroundTask();
        createNotificationChannelObjReached();

        // Send notification to warn user about the background activity
        // If the notification supports a direct reply action, use
        // PendingIntent.FLAG_MUTABLE instead.
        Intent notificationIntent = new Intent(this, MainUnityActivity.class);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(
                this, NOTIFICATION_CHANNEL_BACKGROUND_TASK_ID)
                    .setContentTitle(getText(R.string.notification_foreground_title))
                    .setContentText(getText(R.string.notification_foreground_message))
                    .setSmallIcon(R.drawable.ic_foot)
                    .setContentIntent(pendingIntent)
                    // .setTicker(getText(R.string.ticker_text))
                    .build();
        initSensorManager();
        // Notification ID cannot be 0.
        startForeground(ONGOING_NOTIFICATION_ID, notification);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(tag, "StepService => onBind");
        return binder;
    }

    @Override
    public void onDestroy() {
        Log.d(tag, "StepService => onDestroy => Service destroyed");
        Toast.makeText(this, "MApp step counter service has been killed!", Toast.LENGTH_SHORT).show();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        super.onDestroy();
    }

    private void createNotificationChannelBackgroundTask() {

        CharSequence name = getString(R.string.notification_foreground_channel_name);
        String description = getString(R.string.notification_foreground_channel_description);
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_BACKGROUND_TASK_ID, name, importance);
        channel.setDescription(description);
        channel.setShowBadge(false);
        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void createNotificationChannelObjReached() {

        CharSequence name = getString(R.string.notification_channel_obj_reached_name);
        String description = getString(R.string.notification_channel_obj_reached_description);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_OBJ_REACHED_ID, name, importance);
        channel.setDescription(description);
        channel.setShowBadge(true);
        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    void sendNotificationObjectiveReached(Reward reward) {
        // If the notification supports a direct reply action, use
        // PendingIntent.FLAG_MUTABLE instead.
        Intent notificationIntent = new Intent(this, MainUnityActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Extra stuff for notifying the activity in case the user clicked on it
        notificationIntent.setAction(Intent.ACTION_SEND);  // DON'T REMOVE. NECESSARY FOR AN OBSCURE REASON
        notificationIntent.putExtra("LAUNCHED_FROM_NOTIFICATION", reward.id);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);

        String title = getString(R.string.notification_objective_reached_title, reward.amount, reward.objective);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_OBJ_REACHED_ID)
                .setContentTitle(title)
                .setContentText(getText(R.string.notification_objective_reached_message))
                .setSmallIcon(R.drawable.ic_cup)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setAutoCancel(true); // Make this notification automatically dismissed when the user touches it.
        // .setTicker(getText(R.string.ticker_text))
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        int notificationId = reward.id;
        // notificationId is a unique int for each notification that you must define
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build());
        } else {
            Log.d(tag, "notification not authorized");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorValue = (int) sensorEvent.values[0];
        Log.d(tag, "onSensorChanged: " + sensorValue);
        StepRecord rec = stepDao.recordNewSensorValue(sensorValue);
        checkIfObjectiveIsReached(rec);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    private void initSensorManager(){
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor!=null){
            sensorManager.registerListener(this,countSensor,SensorManager.SENSOR_DELAY_UI);
        } else {
            Log.e(tag, "initSensorManager => Sensor not found");
        }
        Log.d(tag, "initSensorManager => Sensor manager initialized");
    }

    void checkIfObjectiveIsReached(StepRecord rec) {

        // Make inaccessible the rewards older than that day
        rewardDao.updateAccessibleAccordingToDay(rec.ts);

        List<Reward> rewards = rewardDao.notFlaggedObjectiveReachedRewards(rec.stepMidnight);

        for (Reward rwd: rewards){

            Log.d(tag, "objective reached");

            // Update reward's 'objectiveReached' flag
            rewardDao.rewardObjectiveHasBeenReached(rwd.id, rec.ts);

            // Send notification
            sendNotificationObjectiveReached(rwd);
        }
    }
}