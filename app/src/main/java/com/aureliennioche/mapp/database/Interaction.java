package com.aureliennioche.mapp.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "interaction")
public class Interaction {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo()
    public long ts;  // Unix timestamp in milliseconds
    @ColumnInfo()
    public String event;
}
