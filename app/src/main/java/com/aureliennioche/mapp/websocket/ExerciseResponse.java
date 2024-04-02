package com.aureliennioche.mapp.websocket;

public class ExerciseResponse {
    @SuppressWarnings("unused")
    public String subject = "update";
    public long lastActivityTimestampMillisecond = -1;
    public long lastInteractionTimestampMillisecond = -1;
    public String syncedChallengesId = "<to be filled by the server>"; //JSON
    public String syncedChallengesTag = "<to be filled by the server>"; //JSON
    public String updatedChallenges = "<to be filled by the server>";  // JSON
}
