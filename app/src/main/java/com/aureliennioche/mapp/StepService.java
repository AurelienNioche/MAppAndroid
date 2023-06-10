package com.aureliennioche.mapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

public class StepService extends Service implements SensorEventListener {
    private static final int ONGOING_NOTIFICATION_ID = 1234;
    private static final String CHANNEL_ID = "channel_id";

    SensorManager sensorManager;
    String tag = "testing"; // this.getClass().getSimpleName();

    StepDao stepDao;

    @Override
    public void onCreate() {
        stepDao = StepDatabase.getInstance(this.getApplicationContext()).stepDao();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(tag, "onStartCommand => Service starting");

        createNotificationChannel();

        // If the notification supports a direct reply action, use
        // PendingIntent.FLAG_MUTABLE instead.
        Intent notificationIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationIntent = new Intent(this, Bridge.class);
        }
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);

        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_message))
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
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(tag, "onDestroy => Service destroyed");
        Toast.makeText(this, "MApp step counter service has been killed!", Toast.LENGTH_SHORT).show();
        sensorManager.unregisterListener(this);
        super.onDestroy();
    }

    private void createNotificationChannel() {

        CharSequence name = getString(R.string.channel_name);
        String description = getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorValue = (int) sensorEvent.values[0];
        Log.d(tag, "onSensorChanged: " + sensorValue);
        recordNewSensorValue(sensorValue);
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

    void recordNewSensorValue(int sensorValue)
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

        // Record new entry
        stepDao.insertStepRecord(new StepRecord(
                timestamp,
                lastBootTimestamp,
                sensorValue,
                stepNumberSinceMidnight
        ));

        // Delete
        long bound = midnight.minusMonths(Bridge.KEEP_DATA_NO_LONGER_THAN_X_MONTH).getMillis();
        stepDao.deleteRecordsOlderThan(bound);

        // Delete older ones within a 6 min range (we assume we don't need data more than every 6 minutes)
        long lowerBound = midnight.minusMinutes(Bridge.MIN_DELAY_BETWEEN_TWO_RECORDS_MINUTES).getMillis();
        lowerBound = Math.max(midnightTimestamp, lowerBound); // Bound the bound to midnight that dat
        stepDao.deleteRecordsOnInterval(lowerBound, timestamp); // Upper bound is the timestamp of that recording
    }
}