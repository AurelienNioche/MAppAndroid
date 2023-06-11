package com.aureliennioche.mapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;


@Dao
public interface ProfileDao {

    @Query("SELECT * FROM profile LIMIT 1")
    Profile getProfile();

    @Query("SELECT COUNT(id) FROM profile")
    int getRowCount();

    @Query("SELECT username FROM profile LIMIT 1")
    String getUsername();

    @Query("SELECT dailyObjective FROM  profile LIMIT 1")
    int getDailyObjective();

    @Insert
    void insert(Profile profile);

    @Update
    void update(Profile profile);
}
