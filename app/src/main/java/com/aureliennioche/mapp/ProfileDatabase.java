package com.aureliennioche.mapp;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Profile.class}, version = 1)
public abstract class ProfileDatabase extends RoomDatabase {
    public abstract ProfileDao profileDao();

    private static ProfileDatabase instance;

    // Only one thread can execute this at a time
    public static synchronized ProfileDatabase getInstance(Context context)
    {
        if (instance==null) {
            instance = Room.databaseBuilder(context,
                    ProfileDatabase.class, "profile-database")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
