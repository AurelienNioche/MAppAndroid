package com.aureliennioche.mapp.config;

import org.joda.time.DateTimeZone;

import java.util.concurrent.TimeUnit;

public class Config {
    public static final int keepDataNoLongerThanXdays = 90;
    public static final DateTimeZone tz = DateTimeZone.forID("Europe/London");
    // Macbook pro at home: "ws://192.168.0.14:8080/ws"
    // wss://c2fe-130-209-153-108.ngrok-free.app/ws
    // wss://samoa.dcs.gla.ac.uk/mapp/ws
    public static final String websocketUrl="wss://dd2a-2001-630-40-70e0-00-6f32.ngrok-free.app/ws"; //TODO: Be sure to set that correctly for production
    public static String appVersion = "2023.08.08";
    public static long delaySendRetry = TimeUnit.SECONDS.toMillis(5);
    public static long delayServerReconnection = TimeUnit.SECONDS.toMillis(5); // TODO This should be 30 for production
    public static boolean updateServer = true; //TODO: This should be TRUE for production
    public static boolean initWithStepRecords = true; //TODO: Check that for production
    public static boolean initWithStatus = true; //TODO: Check that for production
    public static final boolean eraseProfileTableAfterUpdate = true; //TODO: Be sure to set that correctly for production
    public static final boolean eraseChallengeTableAfterUpdate = true; //TODO: Be sure to set that correctly for production
    public static final boolean eraseStepTableAfterUpdate = true; //TODO: Be sure to set that correctly for production
    public static final boolean eraseStatusTableAfterUpdate = true; //TODO: Be sure to set that correctly for production
    public static final boolean eraseInteractionTableAfterUpdate = true; //TODO: Be sure to set that correctly for production
    public static long serverUpdateRepeatInterval = TimeUnit.MINUTES.toMillis(5);
    public static long serverUpdateRepeatIntervalWhenAppOpened = TimeUnit.SECONDS.toMillis(5);
}
