package com.aureliennioche.mapp;

import java.util.concurrent.TimeUnit;

public class ConfigAndroid {
    static final int keepDataNoLongerThanXdays = 90;
    static final String timezoneId = "Europe/London";
    // Macbook pro at home: "ws://192.168.0.14:8080/ws"
    // wss://cf9d-130-209-150-254.ngrok-free.app/ws
    // wss://2be9-130-209-150-170.ngrok-free.app/ws
    // wss://samoa.dcs.gla.ac.uk/mapp/ws
    public static final String websocketUrl="wss://samoa.dcs.gla.ac.uk/mapp/ws"; //TODO: Be sure to set that correctly for production
    public static boolean askServerToResetUser = false; //TODO: Be sure to put it to `false` for production
    //TODO: Check as well the Unity settings
    public static String appVersion = "2023.06.24"; // It is actually 2023.07.09 but we won't restart everyone
    public static long delaySendRetry = TimeUnit.SECONDS.toMillis(5);
    public static long delayServerReconnection = TimeUnit.SECONDS.toMillis(30);
    public static boolean updateServer = true; //TODO: This should be TRUE for production
    public static boolean initWithStepRecords = true; //TODO: Check that for production
    public static boolean initWithStatus = true; //TODO: Check that for production
    static final boolean eraseProfileTableAfterUpdate = true; //TODO: Be sure to set that correctly for production
    static final boolean eraseRewardTableAfterUpdate = true; //TODO: Be sure to set that correctly for production
    static final boolean eraseStepRecordTableAfterUpdate = true; //TODO: Be sure to set that correctly for production
    static final boolean eraseStatusTableAfterUpdate = true; //TODO: Be sure to set that correctly for production
    static final boolean eraseInteractionTableAfterUpdate = true; //TODO: Be sure to set that correctly for production
    public static Object maxChallengesPerDay = 3;
}
