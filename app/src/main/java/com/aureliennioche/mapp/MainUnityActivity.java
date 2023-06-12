package com.aureliennioche.mapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unity3d.player.UnityPlayerActivity;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainUnityActivity extends UnityPlayerActivity {
    public static final String tag = "testing";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String CASH_OUT = "cashOut";
    private static final String REVEAL_NEXT_REWARD = "revealNextReward";
    public static MainUnityActivity instance = null;  // From "Unity As A Library" demo
    StepDao stepDao;
    RewardDao rewardDao;
    ProfileDao profileDao;
    StatusDao statusDao;

    // --------------------------------------------------------------------------------------------
    // INTERFACE WITH UNITY
    // --------------------------------------------------------------------------------------------

    @SuppressWarnings("unused")
    public void initSet(
            String username,
            double chestAmount,
            int dailyObjective,
            String rewardList
    ) throws JsonProcessingException {

        // Set up profile
        if (profileDao.getRowCount() > 0) {
            Log.e(tag, "Profile was already existing");
        }
        Profile p = new Profile();
        p.username = username;
        profileDao.insert(p);

        Status s = new Status();
        s.chestAmount = chestAmount;
        s.dailyObjective = dailyObjective;
        // TODO: SET DATE ------------------------
        statusDao.insert(s);

        // Set up rewards
        List<Reward> rewards = mapper.readValue(rewardList, new TypeReference<List<Reward>>(){});
        rewardDao.insertRewardsIfNotExisting(rewards);

        // TODO: Just for display, REMOVE AFTER DEBUG ------------------------
        List<Reward> rewardsInTable = rewardDao.getAll();
        rewardsInTable.forEach(reward -> {
            Log.d(tag, "In table: reward id " + String.valueOf(reward.id));
        });
    }

    @SuppressWarnings("unused")
    public List<String> syncServer(
            long lastRecordTimestampMillisecond,
            List<Integer> syncRewardsId,
            List<String> syncRewardsServerTag)
            throws JsonProcessingException {


        // TODO: (OPTIONAL FOR NOW) delete older records, as they are already on the server
        List<StepRecord> newRecord = stepDao.getRecordsNewerThan(lastRecordTimestampMillisecond);
        String newRecordJson =  mapper.writeValueAsString(newRecord);

        rewardDao.updateServerTags(syncRewardsId, syncRewardsServerTag);

        List<Reward>  rewards = rewardDao.getUnSyncRewards();
        String unSyncRewards = mapper.writeValueAsString(rewards);

        Status status = statusDao.getStatus();
        String statusJson = mapper.writeValueAsString(status);

        List<String> r = new ArrayList<>();
        r.add(profileDao.getUsername());
        r.add(newRecordJson);
        r.add(unSyncRewards);
        r.add(statusJson);
        return r;
    }

    @SuppressWarnings("unused")
    public List<String> getStatusAndCurrentReward() throws JsonProcessingException {

        // TODO: CHECK PROPER UDPATES TO DO

        Reward  reward = rewardDao.getCurrentReward();
        String rewardJson = mapper.writeValueAsString(reward);

        Status status = statusDao.getStatus();

        // TODO: CHECK PROPER UDPATES TO DO
        String statusJson = mapper.writeValueAsString(status);
        List<String> r = new ArrayList<>();
        r.add(statusJson);
        r.add(rewardJson);
        return r;
    }

    @SuppressWarnings("unused")
    public boolean isProfileSet() {
        return profileDao.getRowCount() > 0;
    }

    @SuppressWarnings("unused")
    public String getUsername() {
        return profileDao.getUsername();
    }

    @SuppressWarnings("unused")
    public List<String> userTookAction(String action) throws JsonProcessingException {
        if (Objects.equals(action, CASH_OUT)) {
            Reward rwd = rewardDao.getCurrentReward();
            rewardDao.rewardHasBeenCashedOut(rwd.id);
        } else if (Objects.equals(action, REVEAL_NEXT_REWARD)) {
            Log.d(tag, "reveal next reward");
        } else {
            Log.e(tag, "action not recognized");
        }
        return getStatusAndCurrentReward();
    }

    @SuppressWarnings("unused")
    public List<String> userRevealedNextReward() throws JsonProcessingException {
        // TODO CHANGE STUFF HERE

        return getStatusAndCurrentReward();
    }

    // -------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(tag, "Creating MainUnityActivity");
        if ( savedInstanceState != null)
        {
            int val = savedInstanceState.getInt("LAUNCHED_FROM_NOTIFICATION");
            Log.d(tag, "val "+ val);
        } else {
            Log.d(tag, "didn't find the extras");
        }

        // Interface to the databases
        stepDao = StepDatabase.getInstance(this.getApplicationContext()).stepDao();
        rewardDao = RewardDatabase.getInstance(this.getApplicationContext()).rewardDao();
        profileDao = ProfileDatabase.getInstance(this.getApplicationContext()).profileDao();
        statusDao = StatusDatabase.getInstance(this.getApplicationContext()).statusDao();

        // For starting Unity
        Intent intentUnityPlayer = getIntent();
        handleIntent(intentUnityPlayer);

        instance = this;
    }

    @Override
    protected void onDestroy() {

        Log.d(tag, "UnityActivity => on destroy");
        Intent intent = new Intent("MAIN_UNITY_ACTIVITY_CALLBACK");
        intent.putExtra("CALLBACK", "onDestroy");
        sendBroadcast(intent);

        instance = null;

        super.onDestroy();
    }

    @Override
    protected void onStop() {
        Log.d(tag, "UnityActivity => on stop");
        Intent intent = new Intent("MAIN_UNITY_ACTIVITY_CALLBACK");
        intent.putExtra("CALLBACK", "onStop");
        sendBroadcast(intent);
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(tag, "UnityActivity => on pause");
        Intent intent = new Intent("MAIN_UNITY_ACTIVITY_CALLBACK");
        intent.putExtra("CALLBACK", "onPause");
        sendBroadcast(intent);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(tag, "UnityActivity => on resume");
        Intent intent = new Intent("MAIN_UNITY_ACTIVITY_CALLBACK");
        intent.putExtra("CALLBACK", "onResume");
        sendBroadcast(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
        setIntent(intent);
    }

    void handleIntent(Intent intent) {
        if(intent == null || intent.getExtras() == null) return;

        Log.d(tag, "handleIntent");
        Log.d(tag, intent.getAction());
        Log.d(tag, String.valueOf(intent.getExtras()));

        if (intent.getExtras().containsKey("LAUNCHED_FROM_NOTIFICATION")) {
            Log.d(tag, "val "+ intent.getExtras().getInt("LAUNCHED_FROM_NOTIFICATION"));
        }

        if(intent.getExtras().containsKey("doQuit"))
            if(mUnityPlayer != null) {
                finish();
            }
    }
}
