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

    @Query("SELECT COUNT(id) FROM reward")
    int getRowCount();

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

    @Query("SELECT * FROM reward WHERE accessible = 1 ORDER BY ts, objective")
    List<Reward> accessibleRewards();

    @Query("SELECT * FROM reward WHERE accessible = 1 AND objectiveReached = 0 AND objective <= :stepNumber ORDER BY objective")
    List<Reward> notFlaggedObjectiveReachedRewards(int stepNumber);

    @Query("SELECT * FROM reward WHERE objectiveReached = 1 AND cashedOut = 0 ORDER BY ts, objective")
    List<Reward> rewardsThatNeedCashOut();

    @Query("UPDATE reward SET accessible = 0 WHERE ts < :midnight OR ts >= :tsEndOfDay")
    void updateAccessibleFalseAccordingToDay(long midnight, long tsEndOfDay);

    @Query("UPDATE reward SET accessible = 1 WHERE ts >= :midnight AND ts < :tsEndOfDay")
    void updateAccessibleTrueAccordingToDay(long midnight, long tsEndOfDay);

    default void updateAccessibleAccordingToDay(long timestamp) {
        DateTime dt = new DateTime(timestamp, DateTimeZone.getDefault());
        DateTime midnight = dt.withTimeAtStartOfDay();
        long midnightTimestamp = midnight.getMillis();
        DateTime nextMidnight = midnight.plusDays(1);
        long tsEndOfDay = nextMidnight.getMillis();
        updateAccessibleFalseAccordingToDay(midnightTimestamp, tsEndOfDay);
        updateAccessibleTrueAccordingToDay(midnightTimestamp, tsEndOfDay);
    }

    @Query("SELECT * FROM reward WHERE objective = (SELECT MIN(OBJECTIVE) FROM reward WHERE ACCESSIBLE = 1 AND objectiveReached = 0)")
    List<Reward> nextPossibleReward();

    @Query("UPDATE reward SET cashedOut = 1, cashedOutTs = :ts, localTag = :uuid WHERE id = :rewardId")
    void rewardHasBeenCashedOut(int rewardId, long ts, String uuid);

    default void rewardHasBeenCashedOut(int rewardId) {
        long cashedOutTs = System.currentTimeMillis();;
        rewardHasBeenCashedOut(rewardId, cashedOutTs, generateStringTag());
    }

    @Query("UPDATE reward SET objectiveReached = 1, objectiveReachedTs = :ts, localTag = :uuid WHERE id = :rewardId")
    void rewardObjectiveHasBeenReached(int rewardId, long ts, String uuid);

    default void rewardObjectiveHasBeenReached(int rewardId, long ts) {
        rewardObjectiveHasBeenReached(rewardId, ts, generateStringTag());
    };

    @Query("SELECT COUNT(id) FROM reward WHERE accessible = 1 AND objective > :refObjective")
    int countAccessibleRewardWithHigherObjective(int refObjective);

    default boolean isLastRewardOfTheDay(Reward reward) {
        return countAccessibleRewardWithHigherObjective(reward.objective) == 0;
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
}
