package com.aureliennioche.mapp.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.aureliennioche.mapp.config.Config;
import com.aureliennioche.mapp.database.Interaction;

@Dao
public interface InteractionDao {

    @SuppressWarnings("unused")
    @Query("SELECT * FROM interaction")
    List<Interaction> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Interaction interaction);

    @Query("SELECT * FROM interaction WHERE ts > :ref ORDER BY ts ASC")
    List<Interaction> getInteractionsNewerThan(long ref);

    @Query("DELETE FROM interaction WHERE ts < :ref")
    void deleteRecordsOlderThan(long ref);

    default void deleteRecordsTooOld() {
        long ref = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(Config.keepDataNoLongerThanXdays);
        deleteRecordsOlderThan(ref);
    }

    default void newInteraction(String event) {
        Interaction interaction = new Interaction();
        interaction.ts = System.currentTimeMillis();
        interaction.event = event;
        insert(interaction);
        // Log.d("testing", "recording interaction");
        deleteRecordsTooOld();
    }

    @Query("DELETE FROM interaction")
    void nukeTable();
}
