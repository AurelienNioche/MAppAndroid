package com.aureliennioche.mapp;

public class LoginResponse {
    public String subject;
    public boolean ok;
    public int dailyObjective = -1;
    public double chestAmount = -1;
    public String rewardList = "<to be filled by the server>";  // rewardListInJsonFormat
    public String stepRecordList = "<to be filled by the server>";  // rewardListInJsonFormat
    public String status = "<to be filled by the server>";
    public String username = "<to be filled by the server>";    // To avoid to have to pass out too much arguments back to Android
}
