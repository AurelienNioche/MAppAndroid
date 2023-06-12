package com.aureliennioche.mapp;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Profile {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo()
    public String username;
}
