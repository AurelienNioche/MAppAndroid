package com.aureliennioche.mapp.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.aureliennioche.mapp.config.Config;
import com.aureliennioche.mapp.database.Challenge;
import com.aureliennioche.mapp.database.Step;

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateChallenges(List<Challenge> challenges);

    @Query("SELECT * FROM Challenge ORDER BY tsBegin, objective LIMIT 1")
    Challenge getFirstChallenge();

    @Query("SELECT * FROM Challenge WHERE serverTag != androidTag")
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

    @Query("SELECT * FROM Challenge WHERE tsBegin >= :dayBegins AND tsBegin < :dayEnds AND objectiveReached = 0 AND objective <= :stepNumber ORDER BY objective")
    List<Challenge> notFlaggedObjectiveReachedChallenges(int stepNumber, long dayBegins, long dayEnds);

    @Query("SELECT * FROM Challenge WHERE objectiveReached = 1 AND cashedOut = 0 ORDER BY tsOfferBegin")
    List<Challenge> challengesThatNeedCashOut();

    @Query("SELECT * FROM Challenge WHERE tsBegin >= :dayBegins AND tsBegin < :dayEnds ORDER BY tsOfferBegin")
    List<Challenge> dayChallenges(long dayBegins, long dayEnds);

    @Query("UPDATE Challenge SET cashedOut = 1, cashedOutTs = :ts, androidTag = :uuid WHERE id = :challengeId")
    void challengeHasBeenCashedOut(int challengeId, long ts, String uuid);

    default void challengeHasBeenCashedOut(Challenge challenge) {
        long cashedOutTs = System.currentTimeMillis();;
        challengeHasBeenCashedOut(challenge.id, cashedOutTs, generateStringTag());
    }

    @Query("UPDATE Challenge SET accepted = :accepted, acceptedTs = :acceptedTs, androidTag = :uuid WHERE id = :challengeId")
    void challengeHasBeenAccepted(int challengeId, boolean accepted, long acceptedTs, String uuid);

    default void challengeHasBeenAccepted(Challenge challenge) {
        challengeHasBeenAccepted(challenge.id, true, System.currentTimeMillis(), generateStringTag());
    }

    @Query("UPDATE Challenge SET objectiveReached = 1, objectiveReachedTs = :ts, androidTag = :uuid WHERE id = :challengeId")
    void objectiveHasBeenReached(int challengeId, long ts, String uuid);

    default void objectiveHasBeenReached(Challenge challenge, Step step) {
        objectiveHasBeenReached(challenge.id, step.ts, generateStringTag());
    }

    @Query("UPDATE Challenge SET stepCount = :stepCount,  androidTag = :uuid WHERE id = :challengeId")
    void updateStepCount(int challengeId, int stepCount, String uuid);

    default void updateStepCount(Challenge challenge, int stepCount) {
        updateStepCount(challenge.id, stepCount, generateStringTag());
    }

    @Query("DELETE FROM Challenge")
    void nukeTable();

    @Query("SELECT MIN(tsBegin) FROM Challenge")
    long minTs();

    @Query("SELECT MAX(tsBegin) FROM Challenge")
    long maxTs();

    default long getTsExpBegins() {
        long ts = minTs();
        DateTime dt = new DateTime(ts, Config.tz);
        return dt.withTimeAtStartOfDay().getMillis();
    }

    default long getTsExpEnds() {
        long ts = maxTs();
        DateTime dt = new DateTime(ts, Config.tz);
        DateTime midnight = dt.withTimeAtStartOfDay();
        DateTime nextMidnight = midnight.plusDays(1);
        return nextMidnight.getMillis();
    }

//    default List<Challenge> notFlaggedObjectiveReachedChallenges(Step step) {
//        long dayBegins = new DateTime(step.ts, Config.tz).withTimeAtStartOfDay().getMillis();
//        long dayEnds = dayBegins + TimeUnit.DAYS.toMillis(1);
//        return notFlaggedObjectiveReachedChallenges(step.stepMidnight, dayBegins, dayEnds);
//    }

//    @Query("SELECT * FROM Challenge WHERE id = :id")
//    Challenge getChallenge(int id);

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
