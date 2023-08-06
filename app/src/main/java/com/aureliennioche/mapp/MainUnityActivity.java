package com.aureliennioche.mapp;

import static com.aureliennioche.mapp.Status.EXPERIMENT_NOT_STARTED;
import static com.aureliennioche.mapp.Status.ONGOING_CHALLENGE;
import static com.aureliennioche.mapp.Status.WAITING_FOR_CHALLENGE_TO_START;
import static com.aureliennioche.mapp.Status.WAITING_FOR_NEXT_CHALLENGE_PROPOSAL;
import static com.aureliennioche.mapp.Status.WAITING_FOR_USER_TO_ACCEPT;
import static com.aureliennioche.mapp.Status.WAITING_FOR_USER_TO_CASH_OUT;
import static com.aureliennioche.mapp.Status.EXPERIMENT_ENDED_AND_ALL_CASH_OUT;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.core.app.NotificationManagerCompat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import org.joda.time.DateTime;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MainUnityActivity extends UnityPlayerActivity {
    public static final String tag = "testing";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String USER_ACTION_CASH_OUT = "cashOut";
    private static final String USER_ACTION_REVEAL_NEXT_REWARD = "revealNextReward";
    private static final String USER_ACTION_OPEN_FROM_NOTIFICATION = "openFromNotification";
    private static final String USER_ACTION_NONE = "none";
    public static MainUnityActivity instance = null;  // From "Unity As A Library" demo
    StepDao stepDao;
    ChallengeDao challengeDao;
    ProfileDao profileDao;
    StatusDao statusDao;
    InteractionDao interactionDao;
    boolean loginChecked;
    boolean loginOk;

    // ------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(tag, "Creating MainUnityActivity");

        // Interfaces to the database
        MAppDatabase db = MAppDatabase.getInstance(this.getApplicationContext());
        stepDao = db.stepDao();
        challengeDao = db.rewardDao();
        profileDao = db.profileDao();
        statusDao = db.statusDao();
        interactionDao = db.interactionDao();

        // Record the event
        interactionDao.newInteraction("onCreate");

        setupBroadcasterReceiver();

        // For starting Unity
        Intent intentUnityPlayer = getIntent();
        handleIntent(intentUnityPlayer);

        instance = this;

        // finishAndRemoveTask();
    }

    // --------------------------------------------------------------------------------------------
    // INTERFACE WITH UNITY
    // --------------------------------------------------------------------------------------------

    @SuppressWarnings("unused")
    public String getConfig() throws JsonProcessingException {
        ConfigUnity configUnity = new ConfigUnity();
        Log.d(tag, mapper.writeValueAsString(configUnity));
        return mapper.writeValueAsString(configUnity);
    }

    @SuppressWarnings("unused")
    public void updateConnectionInfo() {
        Log.d(tag, "MainUnityActivity => Send broadcast for updating connection info");
        Intent broadcastIntent = new Intent("WEBSOCKET_CONNECTION_INFO");
        sendBroadcast(broadcastIntent);
    }

    @SuppressWarnings("unused")
    public void userEnteredUsername(String username) throws JsonProcessingException {
        Log.d(tag, "MainUnityActivity => Sending broadcast for login");
        loginChecked = false; // Reset flags
        loginOk = false; // Reset flags
        LoginRequest lr = new LoginRequest();
        lr.appVersion = ConfigAndroid.appVersion;
        lr.resetUser = ConfigAndroid.askServerToResetUser;
        lr.username = username;
        String lrJson = mapper.writeValueAsString(lr);

        Intent broadcastIntent = new Intent("WEBSOCKET_SEND");
        broadcastIntent.putExtra("message", lrJson);
        sendBroadcast(broadcastIntent);
    }

    @SuppressWarnings("unused")
    public boolean isProfileSet() {
        boolean val = profileDao.getRowCount() > 0;
        Log.d(tag, "MainUnityActivity => Profile set = "+ val);
        return val;
    }

    @SuppressWarnings("unused")
    public boolean isLoginChecked() {
        return loginChecked;
    }

    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public String getStatus(String userAction) throws JsonProcessingException {
        Status status = statusDao.getStatus();

        Log.d(tag, "------------------------------------------------------");
        Log.d(tag, "Status BEFORE updating " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(status));

        if (!Objects.equals(status.error, "")) {
            Log.d(tag, "MainUnityActivity => There is an error, I won't change anything");
            return mapper.writeValueAsString(status);
        }
        switch (userAction) {
            case USER_ACTION_CASH_OUT:
                Log.d(tag, "MainUnityActivity => [User action] User clicked cashed out");
                break;
            case USER_ACTION_REVEAL_NEXT_REWARD:
                Log.d(tag, "MainUnityActivity => [User action] User clicked next reward");
                break;
            case USER_ACTION_NONE:
                // Log.d(tag, "MainUnityActivity => Just for info: No user action");
                break;
            case USER_ACTION_OPEN_FROM_NOTIFICATION:
                Log.d(tag, "MainUnityActivity => [User action] Open from notification");
                break;
            default:
                Log.d(tag, "MainUnityActivity => User action not recognized!!! This shouldn't happen");
                break;
        }

        // Log.d(tag, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reward));

        // First, look if dates of the experiment are gone
        long tsExpBegins = challengeDao.getTsExpBegins();
        long tsExpEnds = challengeDao.getTsExpEnds();
        long tsNow = System.currentTimeMillis();
        long dayBegins = new DateTime(tsNow, MainActivity.tz).withTimeAtStartOfDay().getMillis();
        long dayEnds = dayBegins + TimeUnit.DAYS.toMillis(1);

        boolean experimentStarted = tsNow >= tsExpBegins;
        boolean experimentEnded = tsNow >= tsExpEnds;

        boolean changeOfDay = tsNow > dayEnds;

        // We changed of day
        if (changeOfDay) {
            if (Objects.equals(status.state, WAITING_FOR_USER_TO_CASH_OUT)) {
                // We stick to the same day
                dayBegins = new DateTime(status.time * 1000, MainActivity.tz).withTimeAtStartOfDay().getMillis();
                dayEnds = dayBegins + TimeUnit.DAYS.toMillis(1);
            } else {
                if (experimentStarted) {
                    status.state = WAITING_FOR_NEXT_CHALLENGE_PROPOSAL;
                    status.currentChallenge = 0;
                }
                status.time = tsNow / 1000;  // In seconds
            }
        }

        status.challenges = challengeDao.dayChallenges(dayBegins, dayEnds);
        Challenge challenge = null;
        if (!experimentEnded) {
            challenge = status.challenges.get(status.currentChallenge);
        }

        switch (status.state) {
            case EXPERIMENT_NOT_STARTED:
                if (experimentEnded) {
                    // The user miss all the experience
                    // We don't really expect that to happen
                    status.state = EXPERIMENT_ENDED_AND_ALL_CASH_OUT;
                } else if (experimentStarted) {
                    status.state = WAITING_FOR_NEXT_CHALLENGE_PROPOSAL;
                } else {
                    // User still needs to wait
                    Log.d(tag, "MainUnityActivity => Experiment not started yet");
                }
                break;

            case WAITING_FOR_NEXT_CHALLENGE_PROPOSAL:
                if (experimentEnded) {
                    status.state = EXPERIMENT_ENDED_AND_ALL_CASH_OUT;
                } else if (tsNow > challenge.tsAcceptBegin) {
                    if (tsNow < challenge.tsAcceptEnd) {
                        // We are in the same day and the reward is in the past
                        // We need to show the reward
                        status.state = WAITING_FOR_USER_TO_ACCEPT;
                    } else {
                        // The user missed the proposal window, we need to propose another challenge
                        if (status.currentChallenge < ConfigAndroid.maxChallengesPerDay - 1) {
                            status.currentChallenge++;
                        }
                        // Otherwise the status doesn't change and we just wait for the next day
                    }
                } else {
                    Log.d(tag, "MainUnityActivity => Still waiting for next challenge proposal");
                }

            case WAITING_FOR_USER_TO_ACCEPT:
                if (challenge == null) {
                    status.error = "Unexpected error (null value for challenge)!";
                    Log.d(tag, "MainUnityActivity => Challenge is null, this shouldn't happen");
                    break;
                }
                boolean buttonAction = Objects.equals(userAction, USER_ACTION_REVEAL_NEXT_REWARD);
                if (tsNow > challenge.tsAcceptEnd) {
                    Log.d(tag, "MainUnityActivity => User missed the acceptance window");
                    status.state = WAITING_FOR_NEXT_CHALLENGE_PROPOSAL;
                } else if (buttonAction) {
                    Log.d(tag, "MainUnityActivity => User accepted the challenge");
                    status.state = WAITING_FOR_CHALLENGE_TO_START;
                } else {
                    Log.d(tag, "MainUnityActivity => Still waiting for user to accept");
                }
                break;

            case WAITING_FOR_CHALLENGE_TO_START:
                if (challenge == null) {
                    status.error = "Unexpected error (null value for challenge)!";
                    Log.d(tag, "MainUnityActivity => Challenge is null, this shouldn't happen");
                    break;
                }
                if(tsNow < challenge.tsBegin) {
                    Log.d(tag, "MainUnityActivity => Still waiting for challenge to start");
                } else if (tsNow < challenge.tsEnd) {
                    status.state = ONGOING_CHALLENGE;
                } else {
                    // User missed the challenge
                    if (status.currentChallenge < ConfigAndroid.maxChallengesPerDay - 1) {
                        status.currentChallenge++;
                    }
                    status.state = WAITING_FOR_NEXT_CHALLENGE_PROPOSAL;
                }
                break;

            case ONGOING_CHALLENGE:
                if (challenge == null) {
                    status.error = "Unexpected error (null value for challenge)!";
                    Log.d(tag, "MainUnityActivity => Challenge is null, this shouldn't happen");
                    break;
                }
                // Check that the objective has not been reached
                if (challenge.objectiveReached) {
                    status.state = WAITING_FOR_USER_TO_CASH_OUT;
                } else if (challenge.tsEnd > tsNow) {
                    // User missed the challenge
                    if (status.currentChallenge < ConfigAndroid.maxChallengesPerDay - 1) {
                        status.currentChallenge++;
                    }
                    status.state = WAITING_FOR_NEXT_CHALLENGE_PROPOSAL;
                }
                break;

            case WAITING_FOR_USER_TO_CASH_OUT:

                List<Challenge> toCashOut = challengeDao.challengesThatNeedCashOut();
                if (toCashOut.size() == 0) {
                    status.error = "Error! Waiting for cash out but nothing to cash out!";
                } else if (Objects.equals(userAction, USER_ACTION_CASH_OUT)) {
                    Log.d(tag, "MainUnityActivity => User cashed out");
                    challenge = toCashOut.get(0);
                    challengeDao.challengeHasBeenCashedOut(challenge);
                    status.chestAmount += challenge.amount;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        View view = this.getCurrentFocus();
                        if (view != null)
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                    }
                    // Cancel the notification if still there
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                    notificationManager.cancel(challenge.id);

                    if (status.currentChallenge < ConfigAndroid.maxChallengesPerDay - 1) {
                        status.currentChallenge++;
                    }
                    status.state = WAITING_FOR_NEXT_CHALLENGE_PROPOSAL;

                    // Inception it's not a bug, it's a feature (thank you @GitHub Copilot for providing this comment)
                    return getStatus(USER_ACTION_NONE);

                } else {
                    Log.d(tag, "MainUnityActivity => Waiting for user to cash out");
                }
                break;

            case EXPERIMENT_ENDED_AND_ALL_CASH_OUT:
                // Nothing specific to do here, that's the END state
                break;

            default:
                Log.d(tag, "MainUnityActivity => Case not handled");
                status.error = "I encountered an error!";
                break;
        }

        DateTime now = new DateTime(status.time * 1000, MainActivity.tz);  // Convert to milliseconds
        status.stepDay = stepDao.getStepNumberSinceMidnightThatDay(dayBegins);
        status.dayOfTheWeek = now.dayOfWeek().getAsText(Locale.ENGLISH);
        status.dayOfTheMonth = now.dayOfMonth().getAsText(Locale.ENGLISH);
        status.month = now.monthOfYear().getAsText(Locale.ENGLISH);

        statusDao.update(status);

        Log.d(tag, "Status AFTER updating" + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(status));
        return mapper.writeValueAsString(status);
    }

    // ---------------------------------------------------------------------------

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void setupBroadcasterReceiver() {
        IntentFilter filter = new IntentFilter("MAIN_UNITY_ACTIVITY_CONNECTION_INFO");

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //do something based on the intent's action
                Log.d(tag,  "MainUnityActivity => Received broadcast for connection info");
                String loginInfoJson = intent.getStringExtra("connectionInfoJson");
                UnityPlayer.UnitySendMessage("AndroidController", "SetConnectionInfo", loginInfoJson);
            }
        };
        registerReceiver(receiver, filter);

        // --------------------------

        filter = new IntentFilter("MAIN_UNITY_ACTIVITY_LOGIN_INFO");

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //do something based on the intent's action
                Log.d(tag,  "MainUnityActivity => Received broadcast");
                String loginInfoJson = intent.getStringExtra("loginInfoJson");
                UnityPlayer.UnitySendMessage("AndroidController", "SetLoginInfo", loginInfoJson);
            }
        };
        registerReceiver(receiver, filter);
    }

    // ---------------------------------------------------------------------------

    @Override
    protected void onDestroy() {

        Log.d(tag, "UnityActivity => on destroy");

        // Record
        interactionDao.newInteraction("onDestroy");

        instance = null;

        super.onDestroy();
    }

    @Override
    protected void onStop() {
        Log.d(tag, "UnityActivity => on stop");
        Interaction interaction = new Interaction();
        interaction.ts = System.currentTimeMillis();
        interaction.event = "onStop";
        interactionDao.insert(interaction);
        interactionDao.deleteRecordsTooOld();
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(tag, "UnityActivity => on pause");

        // Record
        interactionDao.newInteraction("onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(tag, "UnityActivity => on resume");

        // Record
        interactionDao.newInteraction("onResume");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
        setIntent(intent);
    }

    void handleIntent(Intent intent) {
        if(intent == null || intent.getExtras() == null) return;

        if (intent.getExtras().containsKey("UnityActivity => LAUNCHED_FROM_NOTIFICATION")) {
            Log.d(tag, "Opened from the notification corresponding to the reward id "+ intent.getExtras().getInt("LAUNCHED_FROM_NOTIFICATION"));

            // Record
            interactionDao.newInteraction("onNotificationTap");

            try {
                getStatus(USER_ACTION_OPEN_FROM_NOTIFICATION);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        if(intent.getExtras().containsKey("doQuit"))
            if(mUnityPlayer != null) {
                finish();
            }
    }
}
