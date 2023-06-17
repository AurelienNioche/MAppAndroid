package com.aureliennioche.mapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;


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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Status status);

    @Update
    void update(Status status);

    @Query("DELETE FROM status")
    void nukeTable();

    default Status setRewardAttributes(Status s, Reward r) {

        DateTime dt = new DateTime(r.ts, DateTimeZone.getDefault());
        s.rewardId = r.id;
        s.objective = r.objective;
        s.startingAt = r.startingAt;
        s.amount = r.amount;
        s.dayOfTheWeek = dt.dayOfWeek().getAsText();
        s.dayOfTheMonth = dt.dayOfMonth().getAsText();
        s.month = dt.monthOfYear().getAsText();
        return s;
    }

}
