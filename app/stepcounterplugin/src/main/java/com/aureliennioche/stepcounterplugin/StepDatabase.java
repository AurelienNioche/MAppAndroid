package com.aureliennioche.stepcounterplugin;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {StepRecord.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class StepDatabase extends RoomDatabase {
    public abstract StepDao stepDao();

    private static StepDatabase instance;

    // Only one thread can execute this at a time
    public static synchronized StepDatabase getInstance(Context context)
    {
        if (instance==null) {
            instance = Room.databaseBuilder(context,
                    StepDatabase.class, "step-database")
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}
