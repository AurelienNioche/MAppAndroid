package com.aureliennioche.mapp.step;

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

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.aureliennioche.mapp.config.Config;
import com.aureliennioche.mapp.database.MAppDatabase;
import com.aureliennioche.mapp.R;
import com.aureliennioche.mapp.activity.MainUnityActivity;
import com.aureliennioche.mapp.dao.ChallengeDao;
import com.aureliennioche.mapp.dao.StepDao;
import com.aureliennioche.mapp.database.Challenge;
import com.aureliennioche.mapp.database.Step;
import com.aureliennioche.mapp.websocket.WebSocketClient;

import org.joda.time.DateTime;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class StepService extends Service implements SensorEventListener {
    private static final int ONGOING_NOTIFICATION_ID = -1;
    private static final String NOTIFICATION_CHANNEL_BACKGROUND_TASK_ID = "NOTIFICATION_CHANNEL_BACKGROUND_TASK_ID";
    private static final String NOTIFICATION_CHANNEL_OBJ_REACHED_ID = "NOTIFICATION_CHANNEL_OBJ_REACHED_ID";

    // public boolean appVisibleOnScreen;
    SensorManager sensorManager;
    StepDao stepDao;
    ChallengeDao challengeDao;

    WebSocketClient ws;

    // Binder given to clients.
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public StepService getService() {
            // Return this instance of LocalService so clients can call public methods.
            return StepService.this;
        }
    }

    @Override
    public void onCreate() {
        // Log.d(tag, "onStartCommand => Creating the service");
        MAppDatabase db = MAppDatabase.getInstance(this.getApplicationContext());
        stepDao = db.stepDao();
        challengeDao = db.rewardDao();

        ws = WebSocketClient.getInstance();
        ws.start(this);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Log.d(tag, "onStartCommand => Service starting");

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

        ws.scheduleSync(); // For when app is open only // TODO: DEBUG THIS NEW FEATURE

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        // Toast.makeText(this, "MApp step counter service has been killed!", Toast.LENGTH_SHORT).show();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        if (ws != null) {
            ws.close();
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

    void sendNotificationObjectiveReached(Challenge challenge) {
        // If the notification supports a direct reply action, use
        // PendingIntent.FLAG_MUTABLE instead.
        Intent notificationIntent = new Intent(this, MainUnityActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Extra stuff for notifying the activity in case the user clicked on it
        notificationIntent.setAction(Intent.ACTION_SEND);  // DON'T REMOVE. NECESSARY FOR AN OBSCURE REASON
        notificationIntent.putExtra(MainUnityActivity.LAUNCHED_FROM_NOTIFICATION, challenge.id);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);

        String title = getString(R.string.notification_objective_reached_title, challenge.amount);
        String text = getString(R.string.notification_objective_reached_message);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_OBJ_REACHED_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_cashout)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setAutoCancel(true); // Make this notification automatically dismissed when the user touches it.
        // .setTicker(getText(R.string.ticker_text))
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        int notificationId = challenge.id;
        // notificationId is a unique int for each notification that you must define
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build());
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorValue = (int) sensorEvent.values[0];
        Step rec = stepDao.recordNewSensorValue(sensorValue);
        checkIfObjectiveIsReached(rec);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    private void initSensorManager(){
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor!=null){
            sensorManager.registerListener(this,countSensor,SensorManager.SENSOR_DELAY_UI);
        }
    }

    void checkIfObjectiveIsReached(Step rec) {

        long tsNow = System.currentTimeMillis();
        long dayBegins = new DateTime(tsNow, Config.tz).withTimeAtStartOfDay().getMillis();
        long dayEnds = dayBegins + TimeUnit.DAYS.toMillis(1);

        List<Challenge> challenges = challengeDao.dayChallenges(dayBegins, dayEnds);

        for (Challenge c: challenges) {

            if (c.objectiveReached || !c.accepted || !(rec.ts > c.tsBegin && rec.ts < c.tsEnd)) {
                // Log.d(tag, "objective already reached");
                continue;
            }

            int nStepBefore = 0;
            List<Step> stepBeforeChallenge  = stepDao.getLastRecordOnInterval(dayBegins, c.tsBegin);
            if (stepBeforeChallenge.size() > 0) {
                nStepBefore = stepBeforeChallenge.get(0).stepMidnight;
            }

            int nStep = rec.stepMidnight - nStepBefore;

            challengeDao.updateStepCount(c, nStep);

            if (nStep < c.objective) {
                // Log.d(tag, "not enough steps");
                continue;
            }

            // Update reward's 'objectiveReached' flag
            challengeDao.objectiveHasBeenReached(c, rec);

            // Send notification
            sendNotificationObjectiveReached(c);
        }
    }
}