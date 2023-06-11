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

import java.util.List;

public class MainUnityActivity extends UnityPlayerActivity {
    public static final String tag = "testing";
    private static final ObjectMapper mapper = new ObjectMapper();
    public static MainUnityActivity instance = null;  // From "Unity As A Library" demo
    StepDao stepDao;
    RewardDao rewardDao;
    ProfileDao profileDao;

    // UTILS ---------------------------------------------------------------------------------

    // --------------------------------------------------------------------------------------------
    // INTERFACE WITH UNITY
    // --------------------------------------------------------------------------------------------

    public void initSet(
            String rewardList,
            int dailyObjective,
            double chestAmount,
            String username) throws JsonProcessingException {

        // Set up profile
        if (profileDao.getRowCount() > 0) {
            Log.e(tag, "Profile was already existing");
        }
        Profile p = new Profile();
        p.username = username;
        p.dailyObjective = dailyObjective;
        p.chestAmount = chestAmount;
        profileDao.insert(p);

        // Set up rewards
        setRewards(rewardList);
    }

    void setRewards(String jsonData) throws JsonProcessingException {
        List<Reward> rewards = mapper.readValue(jsonData, new TypeReference<List<Reward>>(){});
        rewardDao.insertRewardsIfNotExisting(rewards);

        // TODO: REMOVE AFTER DEBUG ------------------------
        List<Reward> rewardsInTable = rewardDao.getAll();
        rewardsInTable.forEach(reward -> {
            Log.d(tag, "In table: reward id " + String.valueOf(reward.id));
        });
        // ------------------------------------------------
    }

    public int rewardCount() {
        return rewardDao.getRowCount();
    }

    public String getUnSyncRewards() throws JsonProcessingException {
        List<Reward>  rewards = rewardDao.getUnSyncRewards();
        return mapper.writeValueAsString(rewards);
    }

    public void updateServerTags(List<Integer> idList, List<String> serverTagList) {
        rewardDao.updateServerTags(idList, serverTagList);
    }

    public void updateRewardFromJson(String jsonData) throws JsonProcessingException {
        Reward reward = mapper.readValue(jsonData, Reward.class);
        rewardDao.updateReward(reward);
    }

    // ----------------------------

    public boolean isProfileSet() {
        return profileDao.getRowCount() > 0;
    }

    public String getUsername() {
        return profileDao.getUsername();
    }

    public double getChestAmount() {
        return profileDao.getChestAmount();
    }

    public int getDailyObjective() {return profileDao.getDailyObjective();}

    public void setChestAmount(double chestAmount) {
        if (profileDao.getRowCount() < 1) {
            Log.e(tag, "Nothing to edit");
        }
        Profile p = profileDao.getProfile();
        p.chestAmount = chestAmount;
        profileDao.update(p);
    }

    public void updateRewardCashedOut(int rewardId) {
        rewardDao.rewardHasBeenCashedOut(rewardId);
    }

//    public void setUsername(String username) {
//        if (profileDao.getRowCount() > 0) {
//            Profile profile = profileDao.getProfile();
//            profile.username = username;
//            profileDao.update(profile);
//        } else {
//            Profile profile = new Profile();
//            profileDao.insert(profile);
//        }
//    }
//
//    public boolean isDailyObjectiveSet() {
//        return profileDao.getDailyObjective() >= 0;
//    }
//
//    public void setDailyObjective(int dailyObjective) {
//        if (profileDao.getRowCount() > 0) {
//            Profile profile = profileDao.getProfile();
//            profile.dailyObjective = dailyObjective;
//            profileDao.update(profile);
//        } else {
//            Log.e(tag, "want to set daily objective but there isn't");
//        }
//    }

    // --------------------------------

    public int getStepNumberSinceMidnightThatDay(long timestamp) {
        // Log.d(tag, "hello");
        // long timestamp = System.currentTimeMillis();

        DateTime dt = new DateTime(timestamp, DateTimeZone.getDefault());
        DateTime midnight = dt.withTimeAtStartOfDay();
        DateTime nextMidnight = midnight.plusDays(1);

        long midnightTimestamp = midnight.getMillis();
        long nextMidnightTimestamp = nextMidnight.getMillis();

        // Log.d(tag, "timezone ID:" + dt.getZone().getID());
        List<StepRecord> records = stepDao.getLastRecordOnInterval(
                midnightTimestamp,
                nextMidnightTimestamp);
        int stepNumber = 0;
        if (records.size() > 0) {
            stepNumber = records.get(0).stepMidnight;
        }
        // Log.d(tag, "step number: " + stepNumber);
        return stepNumber;
    }

    public String getRecordNewerThanJsonFormat(long timestamp) throws JsonProcessingException {

        // TODO: (OPTIONAL FOR NOW) delete older records, as they are already on the server
        List<StepRecord> list = stepDao.getRecordsNewerThan(timestamp);
        return mapper.writeValueAsString(list);
    }

    // ------------------------------------------------------------------------------------
    // Interface with service

//    public void updateReward(Reward reward) throws JsonProcessingException {
//        reward.localTag = generateStringTag();
//        rewardDao.updateReward(reward);
//    }

    // -------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Interface to the databases
        stepDao = StepDatabase.getInstance(this.getApplicationContext()).stepDao();
        rewardDao = RewardDatabase.getInstance(this.getApplicationContext()).rewardDao();
        profileDao = ProfileDatabase.getInstance(this.getApplicationContext()).profileDao();

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

        if(intent.getExtras().containsKey("doQuit"))
            if(mUnityPlayer != null) {
                finish();
            }
    }
}
