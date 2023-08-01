package com.aureliennioche.mapp;

import static com.aureliennioche.mapp.Status.EXPERIMENT_NOT_STARTED;
import static com.aureliennioche.mapp.Status.EXPERIMENT_JUST_STARTED;
import static com.aureliennioche.mapp.Status.ONGOING_CHALLENGE;
import static com.aureliennioche.mapp.Status.WAITING_FOR_CHALLENGE_TO_START;
import static com.aureliennioche.mapp.Status.WAITING_FOR_NEXT_CHALLENGE_PROPOSAL;
import static com.aureliennioche.mapp.Status.WAITING_FOR_USER_TO_ACCEPT;
import static com.aureliennioche.mapp.Status.WAITING_FOR_USER_TO_CASH_OUT;
import static com.aureliennioche.mapp.Status.EXPERIMENT_ENDED_AND_ALL_CASH_OUT;

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

        Challenge challenge = challengeDao.getChallenge(status.rewardId);
        // Log.d(tag, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reward));

        // First, look if dates of the experiment are gone
        long tsExpBegins = challengeDao.getTsExpBegins();
        long tsExpEnds = challengeDao.getTsExpEnds();
        long tsNow = System.currentTimeMillis();
        long dayBegins = new DateTime(tsNow, MainActivity.tz).withTimeAtStartOfDay().getMillis();
        long dayEnds = dayBegins + TimeUnit.DAYS.toMillis(1);

        boolean rewardWasYesterdayOrBefore = challenge.tsBegin < dayBegins;
        boolean experimentStarted = tsNow >= tsExpBegins;
        boolean experimentEnded = tsNow >= tsExpEnds;

        boolean dayChangedAndNeedToMoveOn = false;

        switch (status.state) {
            case EXPERIMENT_NOT_STARTED:
                if (experimentEnded) {
                    // The user miss all the experience
                    // We don't really expect that to happen
                    status.state = EXPERIMENT_ENDED_AND_ALL_CASH_OUT;
                } else if (experimentStarted) {
                    List<Challenge> toCashOut = challengeDao.challengesThatNeedCashOut();
                    if (toCashOut.size() > 0) {
                        status.state = WAITING_FOR_USER_TO_CASH_OUT;
                        challenge = toCashOut.get(0);
                    } else {
                        List<Challenge> possibleChallenges = challengeDao.nextPossibleChallenge(dayBegins, dayEnds);
                        if (possibleChallenges.size() > 0) {
                            challenge = possibleChallenges.get(0);
                        } else {
                            Log.d(tag, "MainUnityActivity => THIS SHOULD NOT HAPPEN!!!!! Maybe reset the server?");
                            status.error = "Error! I couldn't find the reward to show!";
                        }
                    }
                } else {
                    // User still needs to wait
                    Log.d(tag, "MainUnityActivity => Experiment not started yet");
                }
                break;

            case WAITING_FOR_NEXT_CHALLENGE_PROPOSAL:

                // Only thing to handle is the change of day
                if (rewardWasYesterdayOrBefore) {
                    // We change of day - change of status below
                    dayChangedAndNeedToMoveOn = true;
                } else {
                    // We are still in the same day
                    if (tsNow > challenge.tsAcceptBegin) {
                        if (tsNow < challenge.tsAcceptEnd) {
                            // We are in the same day and the reward is in the past
                            // We need to show the reward
                            status.state = WAITING_FOR_USER_TO_ACCEPT;
                        } else {
                            // The user missed the acceptance window, we need to propose another challenge
                            if (status.currentChallenge < ConfigAndroid.maxChallengesPerDay - 1) {
                                status.currentChallenge++;
                            }
                            // Otherwise just wait for the next day

                        }
                    } else {
                        // We are in the same day and the reward is in the future
                        // Nothing to do
                        Log.d(tag, "MainUnityActivity => Still waiting for next challenge proposal");
                    }

                }

            case WAITING_FOR_USER_TO_CASH_OUT:

                List<Challenge> toCashOut = challengeDao.challengesThatNeedCashOut();
                boolean needToMoveOn;
                if (toCashOut.size() == 0) {
                    Log.d(tag, "MainUnityActivity => THIS SHOULD NOT HAPPEN. I'LL STILL UPDATE TO NEXT STAGE");
                    status.error = "Error! Waiting for cash out but nothing to cash out!";
                    needToMoveOn = false;
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
                    needToMoveOn = true;
                } else {
                    Log.d(tag, "MainUnityActivity => Waiting for user to cash out");
                    needToMoveOn = false;
                }

                if (needToMoveOn) {
                    toCashOut = challengeDao.challengesThatNeedCashOut();
                    if (toCashOut.size() > 0) {
                        challenge = toCashOut.get(0);
                        status.state = WAITING_FOR_USER_TO_CASH_OUT;
                    } else if (experimentEnded) {
                        status.state = EXPERIMENT_ENDED_AND_ALL_CASH_OUT;
                    } else {
                        List<Challenge> possibleChallenges = challengeDao.nextPossibleChallenge(dayBegins, dayEnds);
                        if (possibleChallenges.size() > 0) {
                            challenge = possibleChallenges.get(0);
                        }
                        status.state = WAITING_FOR_CHALLENGE_TO_START;
                    }
                }
                break;

            case WAITING_FOR_USER_TO_ACCEPT:
                boolean buttonAction = Objects.equals(userAction, USER_ACTION_REVEAL_NEXT_REWARD);
                if (buttonAction) {
                    Log.d(tag, "MainUnityActivity => User wants to reveal a new reward");
                    toCashOut = challengeDao.challengesThatNeedCashOut();
                    if (toCashOut.size() > 0) {
                        challenge = toCashOut.get(0);
                        challengeDao.challengeHasBeenAccepted(challenge);
                        status.state = WAITING_FOR_USER_TO_CASH_OUT;
                    } else if (experimentEnded) {
                        status.state = EXPERIMENT_ENDED_AND_ALL_CASH_OUT;
                    } else {
                        List<Challenge> possibleChallenges = challengeDao.nextPossibleChallenge(dayBegins, dayEnds);
                        if (possibleChallenges.size() > 0) {
                            challenge = possibleChallenges.get(0);
                            challengeDao.challengeHasBeenAccepted(challenge);
                            status.state = ONGOING_CHALLENGE;
                        } else {
                            // This might never happen - probably can remove it later on
                            Log.d(tag, "MainUnityActivity => THIS SHOULD NOT HAPPEN!");
                            // status.state = LAST_REWARD_OF_THE_DAY_AND_ALL_CASH_OUT;
                            status.error = "Error! No newer reward found!";
                        }
                    }
                } else if (challengeDao.challengesThatNeedCashOut().size() == 0) {
                    if (experimentEnded) {
                        status.state = EXPERIMENT_ENDED_AND_ALL_CASH_OUT;
                    } else if (rewardWasYesterdayOrBefore) {
                        // Handle change of day
                        dayChangedAndNeedToMoveOn = true;
                    } else {
                        Log.d(tag, "MainUnityActivity => Waiting for user to reveal new reward");
                    }
                } else {
                    Log.d(tag, "MainUnityActivity => Waiting for user to reveal new reward");
                }
                break;

            case ONGOING_OBJECTIVE:
                // Check that the objective has not been reached
                if (challenge.objectiveReached) {
                    status.state = WAITING_FOR_USER_TO_CASH_OUT;
                } else if (rewardWasYesterdayOrBefore) {
                    // We change of day - change of status below
                    dayChangedAndNeedToMoveOn = true;
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

        if (dayChangedAndNeedToMoveOn) {
            List<Challenge> toCashOut = challengeDao.challengesThatNeedCashOut();
            if (toCashOut.size() > 0) {
                challenge = toCashOut.get(0);
                status.state = WAITING_FOR_USER_TO_CASH_OUT;
            } else if (experimentEnded) {
                status.state = EXPERIMENT_ENDED_AND_ALL_CASH_OUT;
            } else {
                List<Challenge> nextPossibleChallenges = challengeDao.nextPossibleChallenge(dayBegins, dayEnds);
                if (nextPossibleChallenges.size() > 0) {
                    challenge = nextPossibleChallenges.get(0);
                    status.state = WAITING_FOR_USER_TO_REVEAL_NEW_REWARD;
                } else {
                    status.error = "Error! The day changed but I didn't find any reward to reveal!";
                }
            }
        }

        status.stepNumber = stepDao.getStepNumberSinceMidnightThatDay(challenge.tsBegin);
        status = statusDao.setRewardAttributes(status, challenge);

        statusDao.update(status);

        Log.d(tag, "Status AFTER updating" + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(status));
        return mapper.writeValueAsString(status);
    }

    // ---------------------------------------------------------------------------

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

    // --------------------------------------

    // -------------------------------------------------------------------------------------

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
