package com.aureliennioche.mapp;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(tableName = "challenge")
public class Challenge {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo()
    public long tsBegin;  // Unix timestamp in milliseconds
    @ColumnInfo()
    public long tsEnd;  // Unix timestamp in milliseconds
    @ColumnInfo()
    public long tsOfferBegin;  // Unix timestamp in milliseconds
    @ColumnInfo()
    public long tsOfferEnd;  // Unix timestamp in milliseconds
    @ColumnInfo()
    public int objective;
    @ColumnInfo()
    public int stepCount;
    @ColumnInfo()
    public double amount;
    @ColumnInfo()
    public boolean accepted;
    @ColumnInfo()
    public long acceptedTs;
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
    public String androidTag;
}
