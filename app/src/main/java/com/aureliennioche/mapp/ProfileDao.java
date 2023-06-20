package com.aureliennioche.mapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProfileDao {

    @Query("SELECT * FROM profile LIMIT 1")
    Profile getProfile();

    @Query("SELECT COUNT(id) FROM profile")
    int getRowCount();

    @Query("SELECT username FROM profile LIMIT 1")
    String getUsername();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Profile profile);

    @Update
    void update(Profile profile);

    @Query("DELETE FROM profile")
    void nukeTable();

    @Query("SELECT * FROM profile")
    List<Profile> getAll();
}
