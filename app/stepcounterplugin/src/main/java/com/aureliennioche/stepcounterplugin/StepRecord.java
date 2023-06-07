package com.aureliennioche.stepcounterplugin;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class StepRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo()
    public long timestamp;

    @ColumnInfo()
    public long lastBootTimestamp;

    @ColumnInfo() // name = "step_number"
    public int stepNumberSinceLastBoot;

    @ColumnInfo() // name = "step_number"
    public int stepNumberSinceMidnight;

    public StepRecord(int id,
                      long timestamp,
                      long lastBootTimestamp,
                      int stepNumberSinceLastBoot,
                      int stepNumberSinceMidnight
                      ) {
        this.id = id;
        this.timestamp = timestamp;
        this.lastBootTimestamp = lastBootTimestamp;
        this.stepNumberSinceLastBoot = stepNumberSinceLastBoot;
        this.stepNumberSinceMidnight = stepNumberSinceMidnight;
    }

    @Ignore
    public StepRecord(long timestamp,
                      long lastBootTimestamp,
                      int stepNumberSinceLastBoot,
                      int stepNumberSinceMidnight) {
        this.timestamp = timestamp;
        this.lastBootTimestamp = lastBootTimestamp;
        this.stepNumberSinceLastBoot = stepNumberSinceLastBoot;
        this.stepNumberSinceMidnight = stepNumberSinceMidnight;
    }
}
