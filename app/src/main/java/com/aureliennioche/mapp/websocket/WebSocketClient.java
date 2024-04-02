package com.aureliennioche.mapp.websocket;

import static com.aureliennioche.mapp.database.Status.EXPERIMENT_NOT_STARTED;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.aureliennioche.mapp.config.Config;
import com.aureliennioche.mapp.database.Interaction;
import com.aureliennioche.mapp.database.MAppDatabase;
import com.aureliennioche.mapp.step.StepService;
import com.aureliennioche.mapp.dao.ChallengeDao;
import com.aureliennioche.mapp.dao.InteractionDao;
import com.aureliennioche.mapp.dao.ProfileDao;
import com.aureliennioche.mapp.dao.StatusDao;
import com.aureliennioche.mapp.dao.StepDao;
import com.aureliennioche.mapp.database.Challenge;
import com.aureliennioche.mapp.database.Profile;
import com.aureliennioche.mapp.database.Status;
import com.aureliennioche.mapp.database.Step;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketClient extends WebSocketListener {
    public static final String BROADCAST_CONNECTION_INFO = "BROADCAST_CONNECTION_INFO";
    public static final String BROADCAST_LOGIN_INFO = "BROADCAST_LOGIN_INFO";
    public static final String LOGIN_INFO = "LOGIN_INFO";
    public static final String CONNECTION_INFO = "CONNECTION_INFO";
    public static final String BROADCAST_SEND_MESSAGE = "BROADCAST_SEND_MESSAGE";
    public static final String MESSAGE_TO_SEND = "MESSAGE_TO_SEND";
    private static WebSocketClient instance;
    static final long DEFAULT_SERVER_LAST_RECORD_TIMESTAMP_MILLISECOND = -1;
    static final long DEFAULT_SERVER_LAST_INTERACTION_TIMESTAMP_MILLISECOND = -1;
    private static final ObjectMapper mapper = new ObjectMapper();
    private WebSocket ws;

    // Interfaces to the database
    MAppDatabase db;
    StepDao stepDao;
    ChallengeDao challengeDao;
    ProfileDao profileDao;
    StatusDao statusDao;
    InteractionDao interactionDao;
    StepService stepService;
    long serverLastRecordTimestampMillisecond = DEFAULT_SERVER_LAST_RECORD_TIMESTAMP_MILLISECOND;
    long serverLastInteractionTimestampMillisecond = DEFAULT_SERVER_LAST_INTERACTION_TIMESTAMP_MILLISECOND;

    // Only one thread can execute this at a time
    public static synchronized WebSocketClient getInstance()
    {
        if (instance==null) {
            instance = new WebSocketClient();
        }
        return instance;
    }

    boolean isOpen() {
        return ws != null;
    }

    void backgroundSync() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(UploadWorker.class, Config.serverUpdateRepeatInterval, TimeUnit.MILLISECONDS)
                // Constraints
                .build();
        WorkManager workManager = WorkManager.getInstance(this.stepService.getBaseContext());
        workManager.enqueueUniquePeriodicWork("sync-server", ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    public void start(StepService stepService) {

        // Log.d("testing", "Starting websocket client");

        this.stepService = stepService;

        db = MAppDatabase.getInstance(stepService.getApplicationContext());
        stepDao = db.stepDao();
        challengeDao = db.challengeDao();
        profileDao = db.profileDao();
        statusDao = db.statusDao();
        interactionDao = db.interactionDao();

        setupBroadcasterReceiver();
        startWebSocket();
    }

    public void startWebSocket() {
        // Log.d("testing", "Starting web socket itself");

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0,  TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder()
                .url(Config.websocketUrl)
                .build();
        client.newWebSocket(request, this);

        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
        client.dispatcher().executorService().shutdown();
    }

    @Override public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
        // Log.d("testing", "websocket open!");
        ws = webSocket;
        broadcastConnection();
        backgroundSync();
    }

    @Override public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        // Log.d(TAG, "MESSAGE: " + text);
        try {
            GenericResponse gr = mapper.readValue(text, new TypeReference<GenericResponse>() {
            });
            if (Objects.equals(gr.subject, GenericResponse.SUBJECT_LOGIN)) {
                LoginResponse lr = mapper.readValue(text, new TypeReference<LoginResponse>() {});
                handleLoginResponse(lr);

            } else if (Objects.equals(gr.subject, GenericResponse.SUBJECT_EXERCISE)) {
                ExerciseResponse er = mapper.readValue(
                        text, new TypeReference<ExerciseResponse>() {});
                handleExerciseResponse(er);
            }
//            else {
//                // Log.d(TAG, "Server response not parsed!!!!!");
//            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
        // Log.d(TAG, "MESSAGE bytes: " + bytes.hex());
    }

    @Override public void onClosing(WebSocket webSocket, int code, @NonNull String reason) {
        webSocket.close(1000, null);
        // Log.d(TAG, "CLOSE: " + code + " " + reason);
        ws = null;
        broadcastConnection();
    }

    @Override public void onFailure(@NonNull WebSocket webSocket, Throwable t, Response response) {
        // Log.d(TAG, "FAILURE: " + response + ", " + t);
        ws = null;

        t.printStackTrace();

        broadcastConnection();

        Handler handler = new Handler(Looper.getMainLooper());
        // Define the code block to be executed
        // Log.d(TAG, "Trying to reconnect server");
        handler.postDelayed(this::startWebSocket, Config.delayServerReconnection);
    }

    public void close() {
        // Log.d(TAG, "I have been ordered to close");
        if (ws != null) {
            ws.close(1000, null);
        }
    }

    public void syncServer() {
        // Log.d(TAG, "I'll try to sync the server");

        if (!profileDao.isProfileSet() || !isOpen() || !Config.updateServer) {
            Log.d("testing", "Unable to syncing server now");
            return;
        }

        // Get last step records
        List<Step> newRecord;
        if (serverLastRecordTimestampMillisecond == DEFAULT_SERVER_LAST_RECORD_TIMESTAMP_MILLISECOND) {
            newRecord = new ArrayList<>();
        } else {
            newRecord = stepDao.getRecordsNewerThan(serverLastRecordTimestampMillisecond);
        }

        // Get last interaction records
        List<Interaction> newInteractions;
        if (serverLastInteractionTimestampMillisecond == DEFAULT_SERVER_LAST_INTERACTION_TIMESTAMP_MILLISECOND) {
            newInteractions = new ArrayList<>();
        } else {
            newInteractions = interactionDao.getInteractionsNewerThan(serverLastInteractionTimestampMillisecond);
        }

        // Get un-synchronized rewards;
        List<Challenge> challenges = challengeDao.getUnsyncedChallenges();

        // Get username and status
        String username = profileDao.getUsername();
        Status status = statusDao.getStatus();

        // Json-ify
        String recordsJson;
        String unSyncRewards;
        String statusJson;
        String interactionsJson;
        try {
            recordsJson =  mapper.writeValueAsString(newRecord);
            unSyncRewards = mapper.writeValueAsString(challenges);
            interactionsJson =  mapper.writeValueAsString(newInteractions);
            statusJson = mapper.writeValueAsString(status);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // Build the request object
        ExerciseRequest er =  new ExerciseRequest();
        er.appVersion = Config.appVersion;
        er.username = username;
        er.interactions = interactionsJson;
        er.steps = recordsJson;
        er.status = statusJson;
        er.unSyncedChallenges = unSyncRewards;

        try {
            String requestJson =  mapper.writeValueAsString(er);
            ws.send(requestJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    void handleExerciseResponse(
            ExerciseResponse er)
            throws JsonProcessingException {

        // Set the last record timestamp
        serverLastRecordTimestampMillisecond = er.lastActivityTimestampMillisecond;
        // Set the last interaction timestamp
        serverLastInteractionTimestampMillisecond = er.lastInteractionTimestampMillisecond;
        //
        List<Integer> syncRewardsId = mapper.readValue(er.syncedChallengesId, new TypeReference<List<Integer>>() {});
        List<String> syncRewardsServerTag = mapper.readValue(er.syncedChallengesTag, new TypeReference<List<String>>() {});

        challengeDao.updateServerTags(syncRewardsId, syncRewardsServerTag);

        String updatedChallenges = er.updatedChallenges;
        List<Challenge> challenges = mapper.readValue(updatedChallenges,
                new TypeReference<List<Challenge>>(){});
        String tag =  challengeDao.generateStringTag();
        challenges.forEach(item -> {item.serverTag = tag; item.androidTag = tag;});

        challengeDao.insertOrUpdateChallenges(challenges);
    }

    public void handleLoginResponse(
            LoginResponse lr
    ) throws JsonProcessingException {

        // Log.d("testing", "handleLoginResponse");

        if (lr.ok) {
            // Log.d("testing", "handleLoginResponse ok");
            String rewardListJson = lr.challengeList;
            String statusJson = lr.status;
            String stepRecordListJson = lr.stepList;
            String username = lr.username;

            // Setup status
            Status s;
            if (Config.initWithStatus) {
                // Log.d("testing", "handleLoginResponse initWithStatus");
                s = mapper.readValue(statusJson,
                        new TypeReference<Status>(){});
            } else {
                // Log.d(TAG, "handleLoginResponse NOT initWithStatus");
                s = new Status();
            }

            // Log.d("testing", "handleLoginResponse status: " + s);

            // Log.d("testing", "handleLoginResponse insert steps");
            // Set up record steps
            if (Config.initWithStepRecords) {
                List<Step> steps =
                        mapper.readValue(stepRecordListJson,
                                new TypeReference<List<Step>>(){});
                stepDao.insertIfNotExisting(steps);

                // Log.d("testing", "handleLoginResponse steps inserted: " + steps.size());
            }

            // Log.d("testing", "handleLoginResponse setup challenges");
            // Set up challenges
            List<Challenge> challenges = mapper.readValue(rewardListJson,
                    new TypeReference<List<Challenge>>(){});
            String tag =  challengeDao.generateStringTag();
            challenges.forEach(item -> {item.serverTag = tag; item.androidTag = tag;});

            challengeDao.insertChallengesIfNotExisting(challenges);

            // Log.d("testing", "handleLoginResponse number of challenges:" + challenges.size());

            // Log.d("testing", "Challenges saved:");
            challengeDao.getAll().forEach(item -> {
                // Log.d("testing", "id " + item.id + " tag " + item.serverTag);
                // Log.d("testing", "begin " + new DateTime(item.tsOfferBegin).toString());
            });

            // Log.d("testing", "handleLoginResponse number of challenges from db:" + challengeDao.getAll().size());

            Challenge r = challengeDao.getFirstChallenge();

            s.state = EXPERIMENT_NOT_STARTED;
            s.stepDay = stepDao.getStepNumberSinceMidnightThatDay(r.tsBegin);
            s.month = new DateTime(r.tsBegin).monthOfYear().getAsText(Locale.ENGLISH);
            s.dayOfTheMonth = new DateTime(r.tsBegin).dayOfMonth().getAsText(Locale.ENGLISH);
            s.dayOfTheWeek = new DateTime(r.tsBegin).dayOfWeek().getAsText(Locale.ENGLISH);

            // Log.d("testing", "Status AT INIT " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(s));

            // Set up profile
            if (profileDao.getRowCount() > 0) {
                // Log.d(TAG, "THIS SHOULD NOT HAPPEN");
                s.error = "Profile already exists";
            }

            Profile p = new Profile();
            p.username = username;
            profileDao.insert(p);

            // Log.d(tag, "Status at the INITIALIZATION " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(s));
            statusDao.insert(s);
        }

        // Log.d("testing", "handleLoginResponse broadcastLoginInfo");
        broadcastLoginInfo(lr);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void setupBroadcasterReceiver() {
        IntentFilter filter = new IntentFilter(BROADCAST_SEND_MESSAGE);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Log.d(TAG, "WebSocketClient => Received a broadcast for sending a message");
                String msg = intent.getStringExtra(MESSAGE_TO_SEND);
                send(msg);
            }
        };
        stepService.registerReceiver(receiver, filter);

        filter = new IntentFilter(BROADCAST_CONNECTION_INFO);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //do something based on the intent's action
                // Log.d(TAG, "WebSocketClient => Received broadcast");
                broadcastConnection();
            }
        };
        stepService.registerReceiver(receiver, filter);
    }

    public void send(String message) {
        // Create the Handler object (on the main thread by default)
        Handler handler = new Handler();
        // Define the code block to be executed
        Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                if (isOpen()) {
                    ws.send(message);
                } else {
                    // Do something here on the main thread
                    // Log.d(TAG, "I will try back");
                    // Repeat this the same runnable code block again another 2 seconds
                    // 'this' is referencing the Runnable object
                    handler.postDelayed(this, Config.delaySendRetry);
                }
            }
        };
        // Start the initial runnable task by posting through the handler
        handler.post(runnableCode);
    }

    void broadcastConnection() {
        // Log.d(TAG, "Send broadcast for connection");
        boolean webSocketOpen = ws != null;
        Intent broadcastIntent = new Intent(BROADCAST_CONNECTION_INFO);
        ConnectionInfo ci = new ConnectionInfo();
        ci.connected = webSocketOpen;
        String ciJson;
        try {
            ciJson = mapper.writeValueAsString(ci);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        broadcastIntent.putExtra(CONNECTION_INFO, ciJson);
        // Log.d(TAG, "Connection info: " + ciJson);
        stepService.sendBroadcast(broadcastIntent);
    }

    void broadcastLoginInfo(LoginResponse loginResponse) throws JsonProcessingException {
        // Log.d(TAG, "Send broadcast for login");
        LoginInfo li = new LoginInfo();
        li.loginOk = loginResponse.ok;
        String liJson = mapper.writeValueAsString(li);
        Intent broadcastIntent = new Intent(BROADCAST_LOGIN_INFO);
        broadcastIntent.putExtra(LOGIN_INFO, liJson);
        stepService.sendBroadcast(broadcastIntent);
    }

    public void scheduleSync() {

        Log.d("testing", "Launching sync");
        // Create a handler to run the sync task
        final Handler handler = new Handler();
        Runnable syncTask = new Runnable() {
            @Override
            public void run() {
                // Perform the sync operation here
                Log.d("testing", "Syncing call from scheduleSync");
                syncServer();

                // Schedule the next sync
                handler.postDelayed(this, Config.serverUpdateRepeatIntervalWhenAppOpened);
            }
        };

        // Schedule the first sync
        handler.postDelayed(syncTask, Config.serverUpdateRepeatIntervalWhenAppOpened);
    }
}
