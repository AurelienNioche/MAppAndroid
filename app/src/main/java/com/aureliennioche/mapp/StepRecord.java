package com.aureliennioche.mapp;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class StepRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo()
    public long ts;  // Timestamp of the recording in millisecond universal time

    @ColumnInfo()
    public long tsLastBoot; // Timestamp of the last boot

    @ColumnInfo()
    public int stepLastBoot; // Number of step since the last boot

    @ColumnInfo() //  Number of step since midnight (not hyper duper precise)
    public int stepMidnight;
}
