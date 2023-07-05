package com.aureliennioche.mapp;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
        Status.class,
        Reward.class,
        Profile.class,
        Interaction.class,
        StepRecord.class,
}, version = 1)
public abstract class MAppDatabase extends RoomDatabase {
    public abstract StatusDao statusDao();
    public abstract ProfileDao profileDao();
    public abstract StepDao stepDao();
    public abstract RewardDao rewardDao();
    public abstract InteractionDao interactionDao();

    private static MAppDatabase instance;

    // Only one thread can execute this at a time
    public static synchronized MAppDatabase getInstance(Context context)
    {
        if (instance==null) {
            Log.d("testing", "CREATING DATABASE");
            instance = Room.databaseBuilder(context,
                    MAppDatabase.class, "mapp-database")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
