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
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class StepService extends Service implements SensorEventListener {
    private static final int ONGOING_NOTIFICATION_ID = 999999;
    private static final String NOTIFICATION_CHANNEL_BACKGROUND_TASK_ID = "NOTIFICATION_CHANNEL_BACKGROUND_TASK_ID";
    private static final String NOTIFICATION_CHANNEL_OBJ_REACHED_ID = "NOTIFICATION_CHANNEL_OBJ_REACHED_ID";
    public static final int MIN_DELAY_BETWEEN_TWO_RECORDS_MINUTES = 6;
    public static final int KEEP_DATA_NO_LONGER_THAN_X_MONTH = 3;
    public static final String tag = "testing";
    public boolean appVisibleOnScreen;
    SensorManager sensorManager;
    StepDao stepDao;
    RewardDao rewardDao;
    int notificationId = 1;

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
        Notification notification = createNotificationBackgroundTask();
        initSensorManager();
        // Notification ID cannot be 0.
        startForeground(ONGOING_NOTIFICATION_ID, notification);

        // sendNotificationObjectiveReached(2.00, 1300);

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
        channel.setShowBadge(false);
        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    Notification createNotificationBackgroundTask() {
        // If the notification supports a direct reply action, use
        // PendingIntent.FLAG_MUTABLE instead.
        Intent notificationIntent = new Intent(this, MainUnityActivity.class);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, NOTIFICATION_CHANNEL_BACKGROUND_TASK_ID)
                .setContentTitle(getText(R.string.notification_foreground_title))
                .setContentText(getText(R.string.notification_foreground_message))
                .setSmallIcon(R.drawable.ic_foot)
                .setContentIntent(pendingIntent)
                // .setTicker(getText(R.string.ticker_text))
                .build();
    }

    void sendNotificationObjectiveReached(double amount, int objective) {
        // If the notification supports a direct reply action, use
        // PendingIntent.FLAG_MUTABLE instead.
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);

        String title = getString(R.string.notification_objective_reached_title, amount, objective);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_OBJ_REACHED_ID)
                .setContentTitle(title)
                .setContentText(getText(R.string.notification_objective_reached_message))
                .setSmallIcon(R.drawable.ic_foot)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // Make this notification automatically dismissed when the user touches it.
        // .setTicker(getText(R.string.ticker_text))
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        } else {
            notificationManager.notify(notificationId, builder.build());
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorValue = (int) sensorEvent.values[0];
        Log.d(tag, "onSensorChanged: " + sensorValue);
        StepRecord rec = recordNewSensorValue(sensorValue);
        boolean objectiveReached = checkIfObjectiveIsReached(rec);
        if (objectiveReached) {
            Reward rwd = rewardDao.currentReward();
            sendNotificationObjectiveReached(rwd.amount, rwd.objective);
        }
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

    StepRecord recordNewSensorValue(int sensorValue)
    {
        Log.d(tag, "recordNewSensorValue => Let's record new stuff");

        long timestamp = System.currentTimeMillis();
        long lastBootTimestamp = timestamp - SystemClock.elapsedRealtime();

        DateTime dt = new DateTime(timestamp, DateTimeZone.getDefault());
        DateTime midnight = dt.withTimeAtStartOfDay();
        long midnightTimestamp = midnight.getMillis();

        int stepNumberSinceMidnight = 0; // Default if no recording, or no recording that day

        List<StepRecord> records = stepDao.getLastRecord();
        // If there is some record
        if (records.size() > 0)
        {
            StepRecord ref = records.get(0);
            // If there is some record /for today/
            if (ref.ts > midnightTimestamp)
            {
                // If phone has been reboot in the between (leave some error margin),
                // Then take the meter reading as the number of steps done since the last log
                // So, steps since midnight is step since midnight from last log plus the meter reading
                if (lastBootTimestamp > ref.tsLastBoot + 500) { // 500: error margin
                    stepNumberSinceMidnight = ref.stepMidnight + sensorValue;
                }
                // If they were no boot, just consider the progression,
                // and add it to the previous log
                else
                {
                    stepNumberSinceMidnight = ref.stepMidnight
                            + (sensorValue - ref.stepLastBoot);
                }
            }
        }

        // Create new record
        StepRecord rec = new StepRecord(
                timestamp,
                lastBootTimestamp,
                sensorValue,
                stepNumberSinceMidnight
        );

        // Record new entry
        stepDao.insertStepRecord(rec);

        // Delete
        long bound = midnight.minusMonths(KEEP_DATA_NO_LONGER_THAN_X_MONTH).getMillis();
        stepDao.deleteRecordsOlderThan(bound);

        // Delete older ones within a 6 min range (we assume we don't need data more than every 6 minutes)
        long lowerBound = midnight.minusMinutes(MIN_DELAY_BETWEEN_TWO_RECORDS_MINUTES).getMillis();
        lowerBound = Math.max(midnightTimestamp, lowerBound); // Bound the bound to midnight that dat
        stepDao.deleteRecordsOnInterval(lowerBound, timestamp); // Upper bound is the timestamp of that recording

        return rec;
    }

    boolean checkIfObjectiveIsReached(StepRecord rec) {


        boolean objReached = false;

        // Check if we changed of day
        DateTime recDt = new DateTime(rec.ts, DateTimeZone.getDefault());
        DateTime midnightDt = recDt.withTimeAtStartOfDay();
        long midnightTs = midnightDt.getMillis();

        long midnightTomorrowTs =  midnightTs + TimeUnit.DAYS.toMillis(1);

        List<Reward> rewards = rewardDao.accessibleRewards();

        Reward currentReward = null;
        // We will loop as we might have to
        for (Reward reward: rewards){
            if (reward.ts < midnightTs) {
                reward.accessible = false;
                rewardDao.updateReward(reward);
            } else if (reward.ts < midnightTomorrowTs) {
                currentReward = reward;
            }
        }
        if (currentReward != null && ! currentReward.objective_reached) {

            if (currentReward.objective <= rec.stepMidnight) {
                Log.d(tag, "objective reached");
                currentReward.objective_reached = true;
                currentReward.objective_reached_ts = rec.ts;
                rewardDao.updateReward(currentReward);
                objReached = true;
            }
        }
        return objReached;
    }

    public void Tamere() {
        Log.d(tag, "ta mere du service");
    }
}