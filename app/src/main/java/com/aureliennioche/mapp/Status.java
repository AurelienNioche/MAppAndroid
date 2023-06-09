package com.aureliennioche.mapp;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "status")
public class Status {

    @Ignore
    public static final String WAITING_FOR_USER_TO_CASH_OUT = "waitingForUserToCashOut";
    @Ignore
    public static final String WAITING_FOR_USER_TO_REVEAL_NEW_REWARD = "waitingForUserToRevealNewReward";
    @Ignore
    public static final String EXPERIMENT_NOT_STARTED = "experimentNotStarted";
    @Ignore
    public static final String EXPERIMENT_ENDED_AND_ALL_CASH_OUT = "experimentEndedAndAllCashOut";
    @Ignore
    public static final String LAST_REWARD_OF_THE_DAY_AND_ALL_CASH_OUT = "lastRewardOfTheDayAndAllCashOut";
    @Ignore
    public static final String ONGOING_OBJECTIVE = "onGoingObjective";
    @Ignore
    public static final String EXPERIMENT_JUST_STARTED = "experimentJustStarted";

    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo
    public String state = EXPERIMENT_NOT_STARTED;
    @ColumnInfo
    public int dailyObjective = -1;
    @ColumnInfo
    public double chestAmount = -1;
    @ColumnInfo
    public String dayOfTheWeek = "dayOfTheWeek";
    @ColumnInfo
    public String dayOfTheMonth = "dayOfTheMonth";
    @ColumnInfo
    public String month = "month";
    @ColumnInfo
    public int stepNumber = -1;
    @ColumnInfo
    public int rewardId = -1;
    @ColumnInfo
    public int objective = -1;
    @ColumnInfo
    public int startingAt = -1;
    @ColumnInfo
    public double amount = -1;
    @ColumnInfo
    public String error = "";
}
