package com.aureliennioche.mapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;


@Dao
public interface StatusDao {

    @Query("SELECT * FROM status LIMIT 1")
    Status getStatus();

    @Query("SELECT COUNT(id) FROM status")
    int getRowCount();

    @Query("SELECT dailyObjective FROM status LIMIT 1")
    int getDailyObjective();

    @Query("SELECT chestAmount FROM status LIMIT 1")
    double getChestAmount();

    @Insert
    void insert(Status status);

    @Update
    void update(Status status);
}
