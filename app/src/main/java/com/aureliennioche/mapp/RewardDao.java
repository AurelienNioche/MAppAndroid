package com.aureliennioche.mapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Dao
public interface RewardDao {

    @Query("SELECT * FROM reward")
    List<Reward> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Reward reward);

    @Query("SELECT * FROM reward WHERE id = :id")
    Reward getReward(int id);

    @Query("SELECT * FROM reward ORDER BY ts, objective LIMIT 1")
    Reward getFirstReward();

    @Query("SELECT * FROM reward WHERE serverTag != localTag")
    List<Reward> getUnSyncRewards();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertRewardsIfNotExisting(List<Reward> rewards);

    @Query("UPDATE reward SET serverTag = :serverTag WHERE id == :id")
    void updateServerTag(int id, String serverTag);

    @Transaction
     default void updateServerTags(List<Integer> idList, List<String> serverTag) {
        for (int i = 0; i < idList.size(); i++) {
            updateServerTag(idList.get(i), serverTag.get(i));
        }
    }

    default String generateStringTag() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    @Query("SELECT * FROM reward WHERE ts >= :dayBegins AND ts < :dayEnds AND objectiveReached = 0 AND objective <= :stepNumber ORDER BY objective")
    List<Reward> notFlaggedObjectiveReachedRewards(int stepNumber, long dayBegins, long dayEnds);

    default List<Reward> notFlaggedObjectiveReachedRewards(StepRecord step) {
        long dayBegins = new DateTime(step.ts, DateTimeZone.getDefault()).withTimeAtStartOfDay().getMillis();
        long dayEnds = dayBegins + TimeUnit.DAYS.toMillis(1);
        return notFlaggedObjectiveReachedRewards(step.stepMidnight, dayBegins, dayEnds);
    }

    @Query("SELECT * FROM reward WHERE objectiveReached = 1 AND cashedOut = 0 ORDER BY ts, objective")
    List<Reward> rewardsThatNeedCashOut();

    @Query("SELECT * FROM reward WHERE objectiveReached = 0 AND ts >= :dayBegins AND ts < :dayEnds ORDER BY objective")
    List<Reward> nextPossibleReward(long dayBegins, long dayEnds);

    @Query("UPDATE reward SET cashedOut = 1, cashedOutTs = :ts, localTag = :uuid WHERE id = :rewardId")
    void rewardHasBeenCashedOut(int rewardId, long ts, String uuid);

    default void rewardHasBeenCashedOut(int rewardId) {
        long cashedOutTs = System.currentTimeMillis();;
        rewardHasBeenCashedOut(rewardId, cashedOutTs, generateStringTag());
    }

    @Query("UPDATE reward SET objectiveReached = 1, objectiveReachedTs = :ts, localTag = :uuid WHERE id = :rewardId")
    void rewardObjectiveHasBeenReached(int rewardId, long ts, String uuid);

    default void rewardObjectiveHasBeenReached(Reward reward, StepRecord step) {
        rewardObjectiveHasBeenReached(reward.id, step.ts, generateStringTag());
    };

    @Query("SELECT COUNT(id) FROM reward WHERE ts >= :dayBegins AND ts < :dayEnds AND objective > :refObjective")
    int countAccessibleRewardWithHigherObjective(int refObjective, long dayBegins, long dayEnds);

    default boolean isLastRewardOfTheDay(Reward reward) {
        long dayBegins = new DateTime(reward.ts, DateTimeZone.getDefault()).withTimeAtStartOfDay().getMillis();
        long dayEnds = dayBegins + TimeUnit.DAYS.toMillis(1);
        return countAccessibleRewardWithHigherObjective(reward.objective, dayBegins, dayEnds) == 0;
    }

    @Query("DELETE FROM reward")
    void nukeTable();

    @Query("SELECT MIN(ts) FROM reward")
    long minTs();

    @Query("SELECT MAX(ts) FROM reward")
    long maxTs();

    default long getTsExpBegins() {
        long ts = minTs();
        DateTime dt = new DateTime(ts, DateTimeZone.getDefault());
        return dt.withTimeAtStartOfDay().getMillis();
    }

    default long getTsExpEnds() {
        long ts = maxTs();
        DateTime dt = new DateTime(ts, DateTimeZone.getDefault());
        DateTime midnight = dt.withTimeAtStartOfDay();
        DateTime nextMidnight = midnight.plusDays(1);
        return nextMidnight.getMillis();
    }

//    @Query("SELECT COUNT(id) FROM reward")
//    int getRowCount();
}
