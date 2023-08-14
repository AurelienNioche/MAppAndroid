package com.aureliennioche.mapp;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(tableName = "status")
public class Status {

    @Ignore
    public static final String EXPERIMENT_NOT_STARTED = "experimentNotStarted";
    @Ignore
    public static final String WAITING_FOR_NEXT_CHALLENGE_PROPOSAL = "waitingForNextChallengeProposal";
    @Ignore
    public static final String WAITING_FOR_USER_TO_ACCEPT = "waitingForUserToAccept";
    @Ignore
    public static final String WAITING_FOR_CHALLENGE_TO_START = "waitingForChallengeToStart";
    @Ignore
    public static final String ONGOING_CHALLENGE = "ongoingChallenge";
    @Ignore
    public static final String WAITING_FOR_USER_TO_CASH_OUT = "waitingForUserToCashOut";
    @Ignore
    public static final String EXPERIMENT_ENDED_AND_ALL_CASH_OUT = "experimentEndedAndAllCashOut";

    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo
    public String state = EXPERIMENT_NOT_STARTED;
    @ColumnInfo
    public double chestAmount = -1;
    @ColumnInfo
    public int stepDay = -1;
    @ColumnInfo
    public String dayOfTheWeek = "dayOfTheWeek";
    @ColumnInfo
    public String dayOfTheMonth = "dayOfTheMonth";
    @ColumnInfo
    public String month = "month";
    @ColumnInfo
    public String error = "";
    @ColumnInfo
    public int currentChallenge = 0;
    @ColumnInfo
    public long ts;
    @ColumnInfo
    public long tsAtStartOfDay;
    @Ignore
    List<Challenge> challenges = new ArrayList<>();
}
