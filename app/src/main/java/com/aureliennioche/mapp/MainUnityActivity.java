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

    long getBeginningOfTheDay(long ts) {
        return new DateTime(ts, MainActivity.tz).withTimeAtStartOfDay().getMillis();
    }

    long getBeginningOfTheDay() {
        return getBeginningOfTheDay(System.currentTimeMillis());
    }

    void giveHapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            View view = this.getCurrentFocus();
            if (view != null)
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        }
    }

    void ifExperimentNotStarted(Status status) {

        Log.d(tag, "MainUnityActivity => Experiment not started yet");
        long tsNow = System.currentTimeMillis();
        long tsExpEnds = challengeDao.getTsExpEnds();
        long tsExpBegins = challengeDao.getTsExpBegins();

        boolean experimentStarted = tsNow >= tsExpBegins;
        boolean experimentEnded = tsNow >= tsExpEnds;
        if (experimentEnded) {
            // The user miss all the experience
            // We don't really expect that to happen
            ifExperimentEnded(status);
            return;
        } else if (experimentStarted) {
            ifWaitingForNextChallengeProposal(status);
            return;
        }
        // User still needs to wait
        Log.d(tag, "MainUnityActivity => Experiment not started yet");
        status.ts = tsNow;
    }

    void ifExperimentEnded(Status status) {
        status.state = EXPERIMENT_ENDED_AND_ALL_CASH_OUT;
        status.ts = System.currentTimeMillis();
    }

    void ifWaitingForUserToCashOut(Status status, String userAction) {
        List<Challenge> toCashOut = challengeDao.challengesThatNeedCashOut();
        if (toCashOut.size() == 0) {
            status.error = "Error! Waiting for cash out but nothing to cash out!";
        } else if (Objects.equals(userAction, UserAction.CASH_OUT)) {
            Log.d(tag, "MainUnityActivity => User cashed out");
            Challenge challenge = toCashOut.get(0);
            challengeDao.challengeHasBeenCashedOut(challenge);
            status.chestAmount += challenge.amount;

            // Give some haptic feedback
            giveHapticFeedback();

            // Cancel the notification if still there
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.cancel(challenge.id);

            if (status.currentChallenge < ConfigAndroid.maxChallengesPerDay - 1) {
                status.currentChallenge++;
            }

            // Got to next stage;
            status.state = WAITING_FOR_NEXT_CHALLENGE_PROPOSAL;
            ifWaitingForNextChallengeProposal(status);
        }
    }

    void ifWaitingForNextChallengeProposal(Status status) {
        Log.d(tag, "MainUnityActivity => Waiting for next challenge proposal");
        long tsExpEnds = challengeDao.getTsExpEnds();
        boolean experimentEnded = System.currentTimeMillis() >= tsExpEnds;

        if (experimentEnded) {
            ifExperimentEnded(status);
            return;
        }

        long dayBegins = getBeginningOfTheDay();
        long dayEnds = dayBegins + TimeUnit.DAYS.toMillis(1);
        status.challenges = challengeDao.dayChallenges(dayBegins, dayEnds);
        status.currentChallenge = 0;
        long tsNow = System.currentTimeMillis();
        while (tsNow > status.challenges.get(status.currentChallenge).tsOfferBegin) {
            if (tsNow < status.challenges.get(status.currentChallenge).tsOfferEnd) {
                // We are in the same day and the reward is in the past
                // We need to show the reward
                ifWaitingForUserToAccept(status, UserAction.NONE);
                return;
            } else if (status.currentChallenge < status.challenges.size() - 1) {
                status.currentChallenge++;
            } else {
                // Otherwise the status doesn't change and we just wait for the next day
                break;
            }
        }
        status.state = WAITING_FOR_NEXT_CHALLENGE_PROPOSAL;
        status.ts = System.currentTimeMillis();
        Log.d(tag, "MainUnityActivity => Still waiting for next challenge proposal");
    }

    void ifWaitingForUserToAccept(Status status, String userAction) {
        status.state = WAITING_FOR_USER_TO_ACCEPT;

        long tsNow = System.currentTimeMillis();
        long dayBegins = new DateTime(tsNow, MainActivity.tz).withTimeAtStartOfDay().getMillis();
        long dayEnds = dayBegins + TimeUnit.DAYS.toMillis(1);
        long tsExpEnds = challengeDao.getTsExpEnds();
        boolean experimentEnded = tsNow >= tsExpEnds;

        status.challenges = challengeDao.dayChallenges(dayBegins, dayEnds);
        Challenge challenge;
        if (!experimentEnded) {
            challenge = status.challenges.get(status.currentChallenge);
        } else {
            status.error = "Unexpected error (null value for challenge)!";
            Log.d(tag, "MainUnityActivity => Challenge is null, this shouldn't happen");
            return;
        }

        boolean buttonAction = Objects.equals(userAction, UserAction.ACCEPT);
        if (tsNow > challenge.tsOfferEnd) {
            Log.d(tag, "MainUnityActivity => User missed the acceptance window");
            status.state = WAITING_FOR_NEXT_CHALLENGE_PROPOSAL;
        } else if (buttonAction) {
            Log.d(tag, "MainUnityActivity => User accepted the challenge");
            status.state = WAITING_FOR_CHALLENGE_TO_START;
        } else {
            Log.d(tag, "MainUnityActivity => Still waiting for user to accept");
        }
    }

    void ifWaitingForChallengeToStart(Status status) {
        status.state = WAITING_FOR_CHALLENGE_TO_START;

        long tsNow = System.currentTimeMillis();
        long dayBegin = getBeginningOfTheDay();
        boolean dayChanged = status.ts < dayBegin;
        if (dayChanged) {
            status.currentChallenge = 0;
        } else {
            if (status.currentChallenge < ConfigAndroid.maxChallengesPerDay - 1) {
                status.currentChallenge++;
            }
        }
        status.ts = tsNow;
        long dayBegins = new DateTime(tsNow, MainActivity.tz).withTimeAtStartOfDay().getMillis();
        long dayEnds = dayBegins + TimeUnit.DAYS.toMillis(1);
        long tsExpEnds = challengeDao.getTsExpEnds();
        boolean experimentEnded = tsNow >= tsExpEnds;

        status.challenges = challengeDao.dayChallenges(dayBegins, dayEnds);
        Challenge challenge;
        if (experimentEnded) {
            ifExperimentEnded(status);
            return;
        } else {
            challenge = status.challenges.get(status.currentChallenge);
        }

        if (tsNow < challenge.tsBegin) {
            Log.d(tag, "MainUnityActivity => Still waiting for challenge to start");
        } else if (tsNow < challenge.tsEnd) {
            status.state = ONGOING_CHALLENGE;
        } else {
            // User missed the challenge
            if (status.currentChallenge < ConfigAndroid.maxChallengesPerDay - 1) {
                status.currentChallenge++;
            }

            ifWaitingForNextChallengeProposal(status);
        }
    }

    void ifOngoingChallenge(Status status) {

        status.state = ONGOING_CHALLENGE;

        long refTs = status.ts;
        long dayBegins = new DateTime(refTs, MainActivity.tz).withTimeAtStartOfDay().getMillis();
        long dayEnds = dayBegins + TimeUnit.DAYS.toMillis(1);

        long tsNow = System.currentTimeMillis();

        status.challenges = challengeDao.dayChallenges(dayBegins, dayEnds);
        Challenge challenge = status.challenges.get(status.currentChallenge);

        // Check that the objective has not been reached
        if (challenge.objectiveReached) {
            // User won the challenge, they need to cash out, moving to another state
            ifWaitingForUserToCashOut(status, UserAction.NONE);
        } else if (challenge.tsEnd > tsNow) {
            // User missed the challenge, moving to another state
            ifWaitingForChallengeToStart(status);
        } else {
            // User is still in the challenge
            status.ts = tsNow;
        }
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

        switch (status.state) {
            case EXPERIMENT_NOT_STARTED:
                ifExperimentNotStarted(status);
                break;

            case WAITING_FOR_NEXT_CHALLENGE_PROPOSAL:
                ifWaitingForNextChallengeProposal(status);
                break;

            case WAITING_FOR_USER_TO_ACCEPT:
                ifWaitingForUserToAccept(status, userAction);
                break;

            case WAITING_FOR_CHALLENGE_TO_START:
                ifWaitingForChallengeToStart(status);
                break;

            case ONGOING_CHALLENGE:
                ifOngoingChallenge(status);
                break;

            case WAITING_FOR_USER_TO_CASH_OUT:
                ifWaitingForUserToCashOut(status, userAction);
                break;

            case EXPERIMENT_ENDED_AND_ALL_CASH_OUT:
                ifExperimentEnded(status);
                break;

            default:
                Log.d("testing", "MainUnityActivity => Case not handled");
                status.error = "I encountered an error!";
                break;
        }

        // Set the date
        DateTime now = new DateTime(status.ts, MainActivity.tz);  // Convert to milliseconds
        status.stepDay = stepDao.getStepNumberSinceMidnightThatDay(now.getMillis());
        status.dayOfTheWeek = now.dayOfWeek().getAsText(Locale.ENGLISH);
        status.dayOfTheMonth = now.dayOfMonth().getAsText(Locale.ENGLISH);
        status.month = now.monthOfYear().getAsText(Locale.ENGLISH);

        // Save the status
        statusDao.update(status);

        // Add extra information for Unity
        // Set the challenges (using `now` as the reference date)
        status.challenges = challengeDao.dayChallenges(
                now.withTimeAtStartOfDay().getMillis(),
                now.withTimeAtStartOfDay().getMillis() + TimeUnit.DAYS.toMillis(1));
        // Set the time in Unity system
        status.ts = status.ts / 1000;

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
                getStatus(UserAction.OPEN_FROM_NOTIFICATION);
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
