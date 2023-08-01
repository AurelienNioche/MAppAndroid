package com.aureliennioche.mapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Locale;

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

    default Status setRewardAttributes(Status s, Challenge r) {

        DateTime dt = new DateTime(r.tsBegin, DateTimeZone.getDefault());
        s.rewardId = r.id;
        s.objective = r.stepGoal;
        s.startingAt = r.startingAt;
        s.amount = r.amount;
        s.dayOfTheWeek = dt.dayOfWeek().getAsText(Locale.ENGLISH);
        s.dayOfTheMonth = dt.dayOfMonth().getAsText(Locale.ENGLISH);
        s.month = dt.monthOfYear().getAsText(Locale.ENGLISH);
        return s;
    }
}
