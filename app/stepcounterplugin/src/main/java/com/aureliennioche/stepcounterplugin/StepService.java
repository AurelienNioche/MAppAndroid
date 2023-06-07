package com.aureliennioche.stepcounterplugin;

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
import android.util.Log;
import android.widget.Toast;

public class StepService extends Service implements SensorEventListener {
    private static final int ONGOING_NOTIFICATION_ID = 1234;
    private static final String CHANNEL_ID = "channel_id";

    SensorManager sensorManager;
    String tag = this.getClass().getSimpleName();

    StepDao stepDao;

    @Override
    public void onCreate() {
        stepDao = StepDatabase.getInstance(this.getApplicationContext()).stepDao();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(tag, "Service starting");

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
        Log.d(tag, "Service destroyed");
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show();

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


        // TODO: ERASE LAST 5 MIN RECORDS (EXCEPT BEFORE MIDNIGHT) BEFORE ADDING NEW ONE

        // TODO: UNCOMMENT THIS
        // recordNewSensorValue(sensorValue);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    private void initSensorManager(){
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor!=null){
            sensorManager.registerListener(this,countSensor,SensorManager.SENSOR_DELAY_UI);
        } else {
            Log.e(tag, "Sensor not found");
        }
        Log.d(tag, "Sensor manager initialized");
    }
//
//    void recordNewSensorValue(int sensorValue) {
//
//        // Erase everything first (we might want to do something else later on)
//        // Log.d(tag, "Du passé faisons table rase");
//        // stepDao.nukeTable();
//
//        DateTimeZone timeZone = TIMEZONE;
//        DateTime now = DateTime.now(timeZone).withTimeAtStartOfDay();
//
//        Long ref = now.getMillis();
//        List<StepRecord> stepRecords = stepDao.GetRecordsNewerThan(ref);
//        int value;
//        if (stepRecords.size() > 0 ) {
//            StepRecord firstStepRecord = stepRecords.get(0);
//            value = sensorValue - firstStepRecord.stepNumber;
//        }
//        else
//        {
//            value = 0;
//        }
//
//        // TODO: NOT RECORD EVERY TIME
//
//        Log.d(tag, "Let's record new stuff");
//        Long ts = now.getMillis();
//        stepDao.insertStepRecord(new StepRecord(ts, value));
//
//        // TODO: REMOVE LOG
//         // Log what is already in the database (we might want to do something else later on)
//        logRecords();
//    }
}