package com.aureliennioche.mapp;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class Profile {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo()
    public String username;

    @ColumnInfo(defaultValue = "-1")
    public int dailyObjective;
}
