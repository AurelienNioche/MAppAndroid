package com.aureliennioche.mapp;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Status.class}, version = 2)
public abstract class StatusDatabase extends RoomDatabase {
    public abstract StatusDao statusDao();

    private static StatusDatabase instance;

    // Only one thread can execute this at a time
    public static synchronized StatusDatabase getInstance(Context context)
    {
        if (instance==null) {
            instance = Room.databaseBuilder(context,
                    StatusDatabase.class, "status-database")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
