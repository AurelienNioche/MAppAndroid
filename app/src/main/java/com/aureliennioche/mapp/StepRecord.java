package com.aureliennioche.mapp;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
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

    public StepRecord(int id,
                      long ts,
                      long tsLastBoot,
                      int stepLastBoot,
                      int stepMidnight
                      ) {
        this.id = id;
        this.ts = ts;
        this.tsLastBoot = tsLastBoot;
        this.stepLastBoot = stepLastBoot;
        this.stepMidnight = stepMidnight;
    }

    @Ignore
    public StepRecord(long timestamp,
                      long tsLastBoot,
                      int stepNumberSinceLastBoot,
                      int stepMidnight) {
        this.ts = timestamp;
        this.tsLastBoot = tsLastBoot;
        this.stepLastBoot = stepNumberSinceLastBoot;
        this.stepMidnight = stepMidnight;
    }
}
