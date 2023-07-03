package com.aureliennioche.mapp;

public class ConfigAndroid {
    static final int keepDataNoLongerThanXdays = 90;
    static final boolean eraseTablesAfterInstallExceptSteps = true;
    static final String timezoneId = "Europe/London";

    // Macbook pro at home: "ws://192.168.0.14:8080/ws"
    // wss://samoa.dcs.gla.ac.uk/mapp/ws
    public static final String websocketUrl="ws://192.168.0.14:8080/ws";  //TODO: Be sure to set that correctly for production

    public static boolean askServerToResetUser = true;  //TODO: Be sure to put it to `false` for production
    //TODO: Check as well the Unity settings
    public static String appVersion = "2023.06.24";  // It is actually 2023.06.28 but we won't restart everyone
}
