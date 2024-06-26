package com.aureliennioche.mapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.aureliennioche.mapp.dao.ChallengeDao;
import com.aureliennioche.mapp.dao.InteractionDao;
import com.aureliennioche.mapp.dao.ProfileDao;
import com.aureliennioche.mapp.dao.StatusDao;
import com.aureliennioche.mapp.dao.StepDao;

@Database(entities = {
        Status.class,
        Challenge.class,
        Profile.class,
        Interaction.class,
        Step.class,
}, version = 1)
public abstract class MAppDatabase extends RoomDatabase {
    public abstract StatusDao statusDao();
    public abstract ProfileDao profileDao();
    public abstract StepDao stepDao();
    public abstract ChallengeDao challengeDao();
    public abstract InteractionDao interactionDao();

    private static MAppDatabase instance;

    // Only one thread can execute this at a time
    public static synchronized MAppDatabase getInstance(Context context)
    {
        if (instance==null) {
            // Log.d("testing", "CREATING DATABASE");
            instance = Room.databaseBuilder(context,
                    MAppDatabase.class, "mapp-database")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
