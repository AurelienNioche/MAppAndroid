package com.aureliennioche.mapp;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Reward {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo()
    public long ts;  // Unix timestamp in milliseconds

    @ColumnInfo()
    public int objective;
    @ColumnInfo
    public int startingAt;

    @ColumnInfo()
    public double amount;

    @ColumnInfo()
    public boolean objectiveReached;

    @ColumnInfo()
    public long objectiveReachedTs;

    @ColumnInfo()
    public boolean cashedOut;

    @ColumnInfo()
    public long cashedOutTs;

    @ColumnInfo()
    public String serverTag;

    @ColumnInfo
    public String localTag;
}
