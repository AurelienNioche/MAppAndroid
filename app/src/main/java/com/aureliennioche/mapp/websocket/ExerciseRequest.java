package com.aureliennioche.mapp.websocket;

public class ExerciseRequest {
    @SuppressWarnings("unused")
    public String subject = "update";
    public String username = "<needs to be defined by the user>";
    public String records = "[]";  // JSON from Android
    public String interactions = "[]";  // JSON from Android
    public String unsyncedChallenges = "[]"; // Challenges that needs to be synced
    public String status = "{}";
    public String appVersion = "";
}
