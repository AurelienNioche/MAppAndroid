package com.aureliennioche.mapp;

import androidx.room.Dao;
import androidx.room.Ignore;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;


@Dao
public interface RewardDao {

    @Query("SELECT * FROM reward")
    List<Reward> getAll();

    @Insert
    void insert(Reward reward);

    @Query("SELECT COUNT(id) FROM reward")
    int getRowCount();

    @Query("SELECT * FROM reward WHERE serverTag != localTag")
    List<Reward> getUnSyncRewards();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertRewardsIfNotExisting(List<Reward> rewards);

    @Update
    void unsafeUpdate(Reward reward);  // We should call updateReward instead

    @Query("UPDATE reward SET serverTag = :serverTag WHERE id == :id")
    void updateTag(int id, String serverTag);

    @Transaction
     default void update(List<Integer> idList, List<String> serverTag) {
        for (int i = 0; i < idList.size(); i++) {
            updateTag(idList.get(i), serverTag.get(i));
        }
    }

    @Query("SELECT * FROM reward WHERE accessible = 1 ORDER BY ts, objective")
    List<Reward> accessibleRewards();

    default String generateStringTag() {
        byte[] array = new byte[7]; // length is bounded by 7
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }

    default void updateReward(Reward reward) {
        reward.localTag = generateStringTag();
        unsafeUpdate(reward);
    }

    default Reward currentReward() {
        return accessibleRewards().get(0);
    }
}
