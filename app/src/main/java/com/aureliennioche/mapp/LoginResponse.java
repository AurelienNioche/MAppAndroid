package com.aureliennioche.mapp;

public class LoginResponse {
    @SuppressWarnings("unused")
    public String subject;
    public boolean ok;
    public String challengeList = "<to be filled by the server>";  // rewardListInJsonFormat
    public String stepList = "<to be filled by the server>";  // rewardListInJsonFormat
    public String status = "<to be filled by the server>";
    public String username = "<to be filled by the server>";    // To avoid to have to pass out too much arguments back to Android
}
