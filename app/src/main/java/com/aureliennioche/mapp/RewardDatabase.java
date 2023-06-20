package com.aureliennioche.mapp;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Reward.class}, version = 1)
public abstract class RewardDatabase extends RoomDatabase {
    public abstract RewardDao rewardDao();
    private static RewardDatabase instance;

    // Only one thread can execute this at a time
    public static synchronized RewardDatabase getInstance(Context context)
    {
        if (instance==null) {
            instance = Room.databaseBuilder(context,
                    RewardDatabase.class, "reward-database")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
