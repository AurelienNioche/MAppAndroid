package com.aureliennioche.mapp;

import static com.aureliennioche.mapp.Status.EXPERIMENT_NOT_STARTED;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

class WebSocketClient extends WebSocketListener {

    private final String TAG = "testing";

    private static WebSocketClient instance;

    static final String tag = "testing";

    private static final ObjectMapper mapper = new ObjectMapper();

    private WebSocket ws;

    // Interfaces to the database
    MAppDatabase db;
    StepDao stepDao;
    RewardDao rewardDao;
    ProfileDao profileDao;
    StatusDao statusDao;
    InteractionDao interactionDao;

    StepService stepService;

    long serverLastRecordTimestampMillisecond = -1;
    long serverLastInteractionTimestampMillisecond =-1;

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
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(UploadWorker.class, 15, TimeUnit.MINUTES)
                // Constraints
                .build();
        WorkManager workManager = WorkManager.getInstance(this.stepService.getBaseContext());
        workManager.enqueueUniquePeriodicWork("sync-server", ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    public void start(StepService stepService) {

        this.stepService = stepService;

        db = MAppDatabase.getInstance(stepService.getApplicationContext());
        stepDao = db.stepDao();
        rewardDao = db.rewardDao();
        profileDao = db.profileDao();
        statusDao = db.statusDao();
        interactionDao = db.interactionDao();

        setupBroadcasterReceiver();
        startWebSocket();
    }

    public void startWebSocket() {
        Log.d(tag, "Starting web socket");

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(0,  TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder()
                .url(ConfigAndroid.websocketUrl)
                .build();
        client.newWebSocket(request, this);

        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
        client.dispatcher().executorService().shutdown();
    }

    @Override public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
        Log.d(TAG, "websocket open!");
        // webSocket.send("Hello...");
        // webSocket.send("...World!");
        // webSocket.send(ByteString.decodeHex("deadbeef"));
        // webSocket.close(1000, "Goodbye, World!");
        ws = webSocket;

        broadcastConnection();
        backgroundSync();
    }

    @Override public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        Log.d(TAG, "MESSAGE: " + text);
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
            } else {
                Log.d(tag, "Server response not parsed!!!!!");
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void onMessage(@NonNull WebSocket webSocket, ByteString bytes) {
        Log.d(TAG, "MESSAGE bytes: " + bytes.hex());
    }

    @Override public void onClosing(WebSocket webSocket, int code, @NonNull String reason) {
        webSocket.close(1000, null);
        Log.d(TAG, "CLOSE: " + code + " " + reason);
        ws = null;
        broadcastConnection();
    }

    @Override public void onFailure(@NonNull WebSocket webSocket, Throwable t, Response response) {
        Log.d(TAG, "FAILURE: " + response + ", " + t);
        ws = null;

        t.printStackTrace();

        broadcastConnection();

        Handler handler = new Handler(Looper.getMainLooper());
        // Define the code block to be executed
        handler.postDelayed(() -> {
            Log.d(tag, "Trying to reconnect server");
            startWebSocket();
        }, ConfigAndroid.delayServerReconnection);
    }

    public void close() {
        Log.d(tag, "I have been ordered to close");
        if (ws != null) {
            ws.close(1000, null);
        }
    }

    public void syncServer() {
        Log.d(tag, "I'll try to sync the server");

        if (!profileDao.isProfileSet()) {
            Log.d(tag, "Profile not set yet, I'll just skip");
            return;
        }

        String recordsJson;
        String unSyncRewards;
        String statusJson;
        String interactionsJson;

        if (isOpen()) {

            List<StepRecord> newRecord = stepDao.getRecordsNewerThan(serverLastRecordTimestampMillisecond);
            List<Interaction> newInteractions = interactionDao.getInteractionsNewerThan(serverLastInteractionTimestampMillisecond);
            List<Reward>  rewards = rewardDao.getUnSyncRewards();

            try {
                recordsJson =  mapper.writeValueAsString(newRecord);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            try {
                unSyncRewards = mapper.writeValueAsString(rewards);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            try {
                interactionsJson =  mapper.writeValueAsString(newInteractions);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            String username = profileDao.getUsername();

            Status status = statusDao.getStatus();
            try {
                statusJson = mapper.writeValueAsString(status);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            ExerciseRequest er =  new ExerciseRequest();
            er.appVersion = ConfigAndroid.appVersion;
            er.username = username;
            er.interactions = interactionsJson;
            er.records = recordsJson;
            er.status = statusJson;
            er.unSyncRewards = unSyncRewards;

            try {
                String requestJson =  mapper.writeValueAsString(er);
                ws.send(requestJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        } else {
            Log.d(tag, "Websocket not connected, I'll skip");
        }
    }

    void handleExerciseResponse(
            ExerciseResponse er)
            throws JsonProcessingException {

        serverLastRecordTimestampMillisecond = er.lastActivityTimestampMillisecond;
        serverLastInteractionTimestampMillisecond = er.lastInteractionTimestampMillisecond;

        List<Integer> syncRewardsId = mapper.readValue(er.syncRewardsId, new TypeReference<List<Integer>>() {});
        List<String> syncRewardsServerTag = mapper.readValue(er.syncRewardsTag, new TypeReference<List<String>>() {});

        rewardDao.updateServerTags(syncRewardsId, syncRewardsServerTag);
    }

    public void handleLoginResponse(
            LoginResponse lr
    ) throws JsonProcessingException {

        if (lr.ok) {
            String rewardListJson = lr.rewardList;
            String username = lr.username;
            double chestAmount = lr.chestAmount;
            int dailyObjective = lr.dailyObjective;

            Status s = new Status();

            // Set up profile
            if (profileDao.getRowCount() > 0) {
                Log.d(tag, "THIS SHOULD NOT HAPPEN");
                s.error = "Profile already exists";
            }

            Profile p = new Profile();
            p.username = username;
            profileDao.insert(p);

            // Set up rewards
            List<Reward> rewards = mapper.readValue(rewardListJson, new TypeReference<List<Reward>>(){});
            String tag =  rewardDao.generateStringTag();
            rewards.forEach(item -> {item.serverTag = tag; item.localTag = tag;});

            rewardDao.insertRewardsIfNotExisting(rewards);

            Log.d(tag, "Rewards saved:");
            rewardDao.getAll().forEach(item -> Log.d(tag, "reward id " + item.id + "tag " + item.serverTag));

            Reward r = rewardDao.getFirstReward();

            s.state = EXPERIMENT_NOT_STARTED;
            s.chestAmount = chestAmount;
            s.dailyObjective = dailyObjective;
            s = statusDao.setRewardAttributes(s, r);
            s.stepNumber = stepDao.getStepNumberSinceMidnightThatDay(r.ts);

            Log.d(tag, "Status at the INITIALIZATION " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(s));
            statusDao.insert(s);
        }

        broadcastLoginInfo(lr);
    }

    public void setupBroadcasterReceiver() {
        IntentFilter filter = new IntentFilter("WEBSOCKET_SEND");

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(tag, "WebSocketClient => Received a broadcast for sending a message");
                String msg = intent.getStringExtra("message");
                send(msg);
            }
        };
        stepService.registerReceiver(receiver, filter);

        filter = new IntentFilter("WEBSOCKET_CONNECTION_INFO");
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //do something based on the intent's action
                Log.d(tag, "WebSocketClient => Received broadcast");
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
                    Log.d(tag, "I will try back");
                    // Repeat this the same runnable code block again another 2 seconds
                    // 'this' is referencing the Runnable object
                    handler.postDelayed(this, ConfigAndroid.delaySendRetry);
                }
            }
        };
        // Start the initial runnable task by posting through the handler
        handler.post(runnableCode);
    }

    void broadcastConnection() {
        Log.d(tag, "Send broadcast for connection");
        boolean webSocketOpen = ws != null;
        Intent broadcastIntent = new Intent("MAIN_UNITY_ACTIVITY_CONNECTION_INFO");
        ConnectionInfo ci = new ConnectionInfo();
        ci.connected = webSocketOpen;
        String ciJson;
        try {
            ciJson = mapper.writeValueAsString(ci);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        broadcastIntent.putExtra("connectionInfoJson", ciJson);
        Log.d(tag, "Connection info: " + ciJson);
        stepService.sendBroadcast(broadcastIntent);
    }

    void broadcastLoginInfo(LoginResponse loginResponse) throws JsonProcessingException {
        Log.d(tag, "Send broadcast for login");
        LoginInfo li = new LoginInfo();
        li.loginOk = loginResponse.ok;
        String liJson = mapper.writeValueAsString(li);
        Intent broadcastIntent = new Intent("MAIN_UNITY_ACTIVITY_LOGIN_INFO");
        broadcastIntent.putExtra("loginInfoJson", liJson);
        stepService.sendBroadcast(broadcastIntent);
    }
}
