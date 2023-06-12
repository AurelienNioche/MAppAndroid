package com.aureliennioche.mapp;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Status {

    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo()
    public String username;
    @ColumnInfo(defaultValue = "-1")
    public int dailyObjective;
    @ColumnInfo
    public double chestAmount;
    @ColumnInfo
    public double timestamp = -1;
    @ColumnInfo
    public String dayOfWeek = "day";
    @ColumnInfo
    public String month = "month";
    @ColumnInfo
    public String dayNumber = "dayNumber";
    @ColumnInfo
    public int objective = -1;
    @ColumnInfo
    public boolean waitingUserToDisplayReward = true;
    @ColumnInfo
    public int stepNumber = -1;
    @ColumnInfo
    public boolean experiment_started = true;
    @ColumnInfo
    public boolean experiment_ended = false;
}
