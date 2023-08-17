package com.aureliennioche.mapp.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.aureliennioche.mapp.database.Status;

@Dao
public interface StatusDao {

    @Query("SELECT * FROM status LIMIT 1")
    Status getStatus();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Status status);

    @Update
    void update(Status status);

    @Query("DELETE FROM status")
    void nukeTable();
}
