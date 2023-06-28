package com.aureliennioche.mapp;

import static com.aureliennioche.mapp.Status.EXPERIMENT_ENDED_AND_ALL_CASH_OUT;
import static com.aureliennioche.mapp.Status.EXPERIMENT_JUST_STARTED;
import static com.aureliennioche.mapp.Status.EXPERIMENT_NOT_STARTED;
import static com.aureliennioche.mapp.Status.LAST_REWARD_OF_THE_DAY_AND_ALL_CASH_OUT;
import static com.aureliennioche.mapp.Status.ONGOING_OBJECTIVE;
import static com.aureliennioche.mapp.Status.WAITING_FOR_USER_TO_CASH_OUT;
import static com.aureliennioche.mapp.Status.WAITING_FOR_USER_TO_REVEAL_NEW_REWARD;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.core.app.NotificationManagerCompat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    RewardDao rewardDao;
    ProfileDao profileDao;
    StatusDao statusDao;
    InteractionDao interactionDao;

    // ------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(tag, "Creating MainUnityActivity");

        // Interfaces to the database
        MAppDatabase db = MAppDatabase.getInstance(this.getApplicationContext());
        stepDao = db.stepDao();
        rewardDao = db.rewardDao();
        profileDao = db.profileDao();
        statusDao = db.statusDao();
        interactionDao = db.interactionDao();

        // Record the event
        interactionDao.newInteraction("onCreate");

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
    public void initSet(
            String username,
            double chestAmount,
            int dailyObjective,
            String rewardList
    ) throws JsonProcessingException {

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
        List<Reward> rewards = mapper.readValue(rewardList, new TypeReference<List<Reward>>(){});
        String tag =  rewardDao.generateStringTag();
        rewards.forEach(item -> {item.serverTag = tag; item.localTag = tag;});

        rewardDao.insertRewardsIfNotExisting(rewards);

        Log.d(tag, "rewards saved:");
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

    @SuppressWarnings({"unused", "UnusedReturnValue"})
    public String getStatus(String userAction) throws JsonProcessingException {
        Status status = statusDao.getStatus();

        if (!Objects.equals(status.error, "")) {
            Log.d(tag, "There is an error, I won't change anything");
            return mapper.writeValueAsString(status);
        }
        switch (userAction) {
            case USER_ACTION_CASH_OUT:
                Log.d(tag, "Just for info: User clicked cashed out");
                break;
            case USER_ACTION_REVEAL_NEXT_REWARD:
                Log.d(tag, "Just for info: User clicked next reward");
                break;
            case USER_ACTION_NONE:
                Log.d(tag, "Just for info: No user action");
                break;
            case USER_ACTION_OPEN_FROM_NOTIFICATION:
                Log.d(tag, "Just for info: Open from notification");
                break;
            default:
                Log.d(tag, "User action not recognized!!! This shouldn't happen");
                break;
        }

        Reward reward = rewardDao.getReward(status.rewardId);

        Log.d(tag, "Status BEFORE updating " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(status));
        // Log.d(tag, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reward));

        // First, look if dates of the experiment are gone
        long tsExpBegins = rewardDao.getTsExpBegins();
        long tsExpEnds = rewardDao.getTsExpEnds();
        long tsNow = System.currentTimeMillis();
        long dayBegins = new DateTime(tsNow, MainActivity.tz).withTimeAtStartOfDay().getMillis();
        long dayEnds = dayBegins + TimeUnit.DAYS.toMillis(1);

        boolean rewardWasYesterdayOrBefore = reward.ts < dayBegins;
        boolean experimentStarted = tsNow >= tsExpBegins;
        boolean experimentEnded = tsNow >= tsExpEnds;

        boolean dayChangedAndNeedToMoveOn = false;

        switch (status.state) {
            case EXPERIMENT_NOT_STARTED:
                if (experimentEnded) {
                    // The user miss all the experience
                    // We don't really expect that to happen
                    status.state = EXPERIMENT_ENDED_AND_ALL_CASH_OUT;
                }
                else if (experimentStarted) {
                    status.state = EXPERIMENT_JUST_STARTED;
                    List<Reward> toCashOut = rewardDao.rewardsThatNeedCashOut();
                    if (toCashOut.size() > 0) {
                        reward = toCashOut.get(0);
                    } else {
                        List<Reward> possibleRewards = rewardDao.nextPossibleReward(dayBegins, dayEnds);
                        if (possibleRewards.size() > 0) {
                            reward = possibleRewards.get(0);
                        } else {
                            Log.d(tag, "THIS SHOULD NOT HAPPEN!!!!! Maybe reset the server?");
                            status.error = "Error! I couldn't find the reward to show!";
                        }
                    }
                } else {
                    // User still needs to wait
                    Log.d(tag, "Experiment not started yet");
                }
                break;

            case EXPERIMENT_ENDED_AND_ALL_CASH_OUT:
                // Nothing specific to do here, that's the END state
                break;

            case WAITING_FOR_USER_TO_CASH_OUT:

                List<Reward> toCashOut = rewardDao.rewardsThatNeedCashOut();
                boolean needToMoveOn;
                if (toCashOut.size() == 0) {
                    Log.d(tag, "THIS SHOULD NOT HAPPEN. I'LL STILL UPDATE TO NEXT STAGE");
                    status.error = "Error! Waiting for cash out but nothing to cash out!";
                    needToMoveOn = false;
                } else if (Objects.equals(userAction, USER_ACTION_CASH_OUT)) {
                    Log.d(tag, "User cashed out");
                    reward = toCashOut.get(0);
                    rewardDao.rewardHasBeenCashedOut(reward);
                    status.chestAmount += reward.amount;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        View view = this.getCurrentFocus();
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                    }
                    // Cancel the notification if still there
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                    notificationManager.cancel(reward.id);
                    needToMoveOn = true;
                } else {
                    Log.d(tag, "Waiting for user to cash out");
                    needToMoveOn = false;
                }

                if (needToMoveOn) {
                    toCashOut = rewardDao.rewardsThatNeedCashOut();
                    if (toCashOut.size() > 0) {
                        reward = toCashOut.get(0);
                        status.state = WAITING_FOR_USER_TO_REVEAL_NEW_REWARD;
                    } else if (experimentEnded) {
                        status.state = EXPERIMENT_ENDED_AND_ALL_CASH_OUT;
                    } else {
                        List<Reward> possibleRewards = rewardDao.nextPossibleReward(dayBegins, dayEnds);
                        if (possibleRewards.size() > 0) {
                            reward = possibleRewards.get(0);
                            status.state = WAITING_FOR_USER_TO_REVEAL_NEW_REWARD;
                        } else {
                            status.state = LAST_REWARD_OF_THE_DAY_AND_ALL_CASH_OUT;
                        }
                    }
                }
                break;

            case EXPERIMENT_JUST_STARTED:
            case WAITING_FOR_USER_TO_REVEAL_NEW_REWARD:
                boolean buttonAction = Objects.equals(userAction, USER_ACTION_REVEAL_NEXT_REWARD);
                boolean notificationTap = Objects.equals(userAction, USER_ACTION_OPEN_FROM_NOTIFICATION);
                if (buttonAction || notificationTap) {
                    Log.d(tag, "user wants to reveal a new reward");
                    toCashOut = rewardDao.rewardsThatNeedCashOut();
                    if (toCashOut.size() > 0) {
                        reward = toCashOut.get(0);
                        rewardDao.rewardHasBeenRevealed(reward, buttonAction, notificationTap);
                        status.state = WAITING_FOR_USER_TO_CASH_OUT;
                    } else if (experimentEnded) {
                        status.state = EXPERIMENT_ENDED_AND_ALL_CASH_OUT;
                    } else {
                        List<Reward> possibleRewards = rewardDao.nextPossibleReward(dayBegins, dayEnds);
                        if (possibleRewards.size() > 0) {
                            reward = possibleRewards.get(0);
                            rewardDao.rewardHasBeenRevealed(reward, buttonAction, notificationTap);
                            status.state = ONGOING_OBJECTIVE;
                        } else {
                            // This might never happen - probably can remove it later on
                            Log.d(tag, "THIS SHOULD NOT HAPPEN!");
                            // status.state = LAST_REWARD_OF_THE_DAY_AND_ALL_CASH_OUT;
                            status.error = "Error! No newer reward found!";
                        }
                    }
                } else if (rewardDao.rewardsThatNeedCashOut().size() == 0) {
                    if (experimentEnded) {
                        status.state = EXPERIMENT_ENDED_AND_ALL_CASH_OUT;
                    } else if (rewardWasYesterdayOrBefore) {
                        // Handle change of day
                        dayChangedAndNeedToMoveOn = true;
                    } else {
                        Log.d(tag, "Waiting for user to reveal new reward");
                    }
                } else {
                    Log.d(tag, "Waiting for user to reveal new reward");
                }
                break;

            case LAST_REWARD_OF_THE_DAY_AND_ALL_CASH_OUT:
                // Only thing to handle is the change of day
                if (rewardWasYesterdayOrBefore) {
                    // We change of day - change of status below
                    dayChangedAndNeedToMoveOn = true;
                }
                break;

            case ONGOING_OBJECTIVE:
                // Check that the objective has not been reached
                if (reward.objectiveReached) {
                    status.state = WAITING_FOR_USER_TO_CASH_OUT;
                } else if (rewardWasYesterdayOrBefore) {
                    // We change of day - change of status below
                    dayChangedAndNeedToMoveOn = true;
                }
                break;

            default:
                Log.d(tag, "Case not handled");
                status.error = "I encountered an error!";
                break;
        }

        if (dayChangedAndNeedToMoveOn) {
            List<Reward> toCashOut = rewardDao.rewardsThatNeedCashOut();
            if (toCashOut.size() > 0) {
                reward = toCashOut.get(0);
                status.state = WAITING_FOR_USER_TO_REVEAL_NEW_REWARD;
            } else if (experimentEnded) {
                status.state = EXPERIMENT_ENDED_AND_ALL_CASH_OUT;
            } else {
                List<Reward> nextPossibleRewards = rewardDao.nextPossibleReward(dayBegins, dayEnds);
                if (nextPossibleRewards.size() > 0) {
                    reward = nextPossibleRewards.get(0);
                    status.state = WAITING_FOR_USER_TO_REVEAL_NEW_REWARD;
                } else {
                    status.error = "Error! The day changed but I didn't find any reward to reveal!";
                }
            }
        }

        status.stepNumber = stepDao.getStepNumberSinceMidnightThatDay(reward.ts);
        status = statusDao.setRewardAttributes(status, reward);

        statusDao.update(status);

        Log.d(tag, "Status AFTER updating" + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(status));
        return mapper.writeValueAsString(status);
    }

    @SuppressWarnings("unused")
    public boolean isProfileSet() {
        boolean val =profileDao.getRowCount() > 0;
        Log.d(tag, "Profile set = "+ val);
        return val;
    }

    // --------------------------------------

    @SuppressWarnings("unused")
    public String[] syncServer(
            long lastRecordTimestampMillisecond,
            long lastInteractionTimestampMillisecond,
            String syncRewardsIdJSON,
            String syncRewardsServerTagJSON)
            throws JsonProcessingException {

        List<Integer> syncRewardsId = mapper.readValue(syncRewardsIdJSON, new TypeReference<List<Integer>>() {});
        List<String> syncRewardsServerTag = mapper.readValue(syncRewardsServerTagJSON, new TypeReference<List<String>>() {});

        // TODO: (OPTIONAL FOR NOW) delete older records, as they are already on the server
        List<StepRecord> newRecord = stepDao.getRecordsNewerThan(lastRecordTimestampMillisecond);
        String newRecordJson =  mapper.writeValueAsString(newRecord);

        rewardDao.updateServerTags(syncRewardsId, syncRewardsServerTag);

        List<Reward>  rewards = rewardDao.getUnSyncRewards();
        String unSyncRewards = mapper.writeValueAsString(rewards);

        Status status = statusDao.getStatus();
        String statusJson = mapper.writeValueAsString(status);

        List<Interaction> newInteractions = interactionDao.getInteractionsNewerThan(lastInteractionTimestampMillisecond);
        String newInteractionsJson =  mapper.writeValueAsString(newInteractions);

        String username = profileDao.getUsername();
        return new String[]{
            username, newRecordJson, unSyncRewards, statusJson, newInteractionsJson
        };
    }

    @SuppressWarnings("unused")
    public String[] syncServer()
            throws JsonProcessingException {

        // TODO: (OPTIONAL FOR NOW) delete older records, as they are already on the server
        List<StepRecord> newRecord = stepDao.getAll();
        String newRecordJson =  mapper.writeValueAsString(newRecord);

        List<Reward>  rewards = rewardDao.getUnSyncRewards();
        String unSyncRewards = mapper.writeValueAsString(rewards);

        Status status = statusDao.getStatus();
        String statusJson = mapper.writeValueAsString(status);

        List<Interaction> newInteractions = interactionDao.getAll();
        String newInteractionsJson =  mapper.writeValueAsString(newInteractions);

        String username = profileDao.getUsername();
        return new String[]{
                username, newRecordJson, unSyncRewards, statusJson, newInteractionsJson
        };
    }

    // -------------------------------------------------------------------------------------

    @Override
    protected void onDestroy() {

        Log.d(tag, "UnityActivity => on destroy");
//        Intent intent = new Intent("MAIN_UNITY_ACTIVITY_CALLBACK");
//        intent.putExtra("CALLBACK", "onDestroy");
//        sendBroadcast(intent);

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

        if (intent.getExtras().containsKey("LAUNCHED_FROM_NOTIFICATION")) {
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
