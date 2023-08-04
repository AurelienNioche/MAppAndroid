package com.aureliennioche.mapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import org.joda.time.DateTime;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Dao
public interface ChallengeDao {

    @Query("SELECT * FROM Challenge")
    List<Challenge> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Challenge challenge);

    @Query("SELECT * FROM Challenge WHERE id = :id")
    Challenge getChallenge(int id);

    @Query("SELECT * FROM Challenge ORDER BY tsBegin, stepGoal LIMIT 1")
    Challenge getFirstChallenge();

    @Query("SELECT * FROM Challenge WHERE serverTag != localTag")
    List<Challenge> getUnsyncedChallenges();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertChallengesIfNotExisting(List<Challenge> challenges);

    @Query("UPDATE Challenge SET serverTag = :serverTag WHERE id == :id")
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

    @Query("SELECT * FROM Challenge WHERE tsBegin >= :dayBegins AND tsBegin < :dayEnds AND objectiveReached = 0 AND stepGoal <= :stepNumber ORDER BY stepGoal")
    List<Challenge> notFlaggedObjectiveReachedChallenges(int stepNumber, long dayBegins, long dayEnds);

    default List<Challenge> notFlaggedObjectiveReachedChallenges(Step step) {
        long dayBegins = new DateTime(step.ts, MainActivity.tz).withTimeAtStartOfDay().getMillis();
        long dayEnds = dayBegins + TimeUnit.DAYS.toMillis(1);
        return notFlaggedObjectiveReachedChallenges(step.stepMidnight, dayBegins, dayEnds);
    }

    @Query("SELECT * FROM Challenge WHERE objectiveReached = 1 AND cashedOut = 0 ORDER BY tsAcceptBegin")
    List<Challenge> challengesThatNeedCashOut();

    @Query("SELECT * FROM Challenge WHERE tsBegin >= :dayBegins AND tsBegin < :dayEnds ORDER BY tsAcceptBegin")
    List<Challenge> dayChallenges(long dayBegins, long dayEnds);

    @Query("UPDATE Challenge SET cashedOut = 1, cashedOutTs = :ts, localTag = :uuid WHERE id = :rewardId")
    void challengeHasBeenCashedOut(int rewardId, long ts, String uuid);

    default void challengeHasBeenCashedOut(Challenge challenge) {
        long cashedOutTs = System.currentTimeMillis();;
        challengeHasBeenCashedOut(challenge.id, cashedOutTs, generateStringTag());
    }

    @Query("UPDATE Challenge SET acceptedTs = :ts, localTag = :uuid WHERE id = :rewardId")
    void challengeHasBeenAccepted(int rewardId, long ts, String uuid);

    default void challengeHasBeenAccepted(Challenge challenge) {
        challengeHasBeenAccepted(challenge.id, System.currentTimeMillis(), generateStringTag());
    }

    @Query("UPDATE Challenge SET objectiveReached = 1, objectiveReachedTs = :ts, localTag = :uuid WHERE id = :rewardId")
    void challengeObjectiveHasBeenReached(int rewardId, long ts, String uuid);

    default void challengeObjectiveHasBeenReached(Challenge challenge, Step step) {
        challengeObjectiveHasBeenReached(challenge.id, step.ts, generateStringTag());
    }

    @Query("DELETE FROM Challenge")
    void nukeTable();

    @Query("SELECT MIN(tsBegin) FROM Challenge")
    long minTs();

    @Query("SELECT MAX(tsBegin) FROM Challenge")
    long maxTs();

    default long getTsExpBegins() {
        long ts = minTs();
        DateTime dt = new DateTime(ts, MainActivity.tz);
        return dt.withTimeAtStartOfDay().getMillis();
    }

    default long getTsExpEnds() {
        long ts = maxTs();
        DateTime dt = new DateTime(ts, MainActivity.tz);
        DateTime midnight = dt.withTimeAtStartOfDay();
        DateTime nextMidnight = midnight.plusDays(1);
        return nextMidnight.getMillis();
    }

//    @Query("SELECT COUNT(id) FROM reward WHERE ts >= :dayBegins AND ts < :dayEnds AND objective > :refObjective")
//    int countAccessibleRewardWithHigherObjective(int refObjective, long dayBegins, long dayEnds);

//    @Query("SELECT COUNT(id) FROM reward")
//    int getRowCount();

//    default boolean isLastRewardOfTheDay(Reward reward) {
//        long dayBegins = new DateTime(reward.ts, MainActivity.tz).withTimeAtStartOfDay().getMillis();
//        long dayEnds = dayBegins + TimeUnit.DAYS.toMillis(1);
//        return countAccessibleRewardWithHigherObjective(reward.objective, dayBegins, dayEnds) == 0;
//    }
}
