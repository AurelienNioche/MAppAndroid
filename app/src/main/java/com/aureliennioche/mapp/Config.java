package com.aureliennioche.mapp;

import androidx.room.Entity;

public class Config {
    @SuppressWarnings("unused")
    // Macbook pro at home: "ws://192.168.0.101:8000/ws"
    // Uni:
    public String websocketUrl="ws://192.168.0.101:8000/ws";  //TODO: Be sure to set that correctly for production
    @SuppressWarnings("unused")
    public double websocketTimeReconnection = 2;
    @SuppressWarnings("unused")
    public double androidUpdateFrequencySeconds = 1;
    @SuppressWarnings("unused")
    public double serverUpdateFrequencySeconds = 10;
    @SuppressWarnings("unused")
    public double loadingViewDelayBeforeNextView = 1.2;
    @SuppressWarnings("unused")
    public int loginInputFieldMinLength = 6;
    @SuppressWarnings("unused")
    public String loginInputFieldDefaultUsername = "123test"; //TODO: Be sure to put it to `""` for production
    @SuppressWarnings("unused")
    public boolean askServerToResetUser=true;  //TODO: Be sure to put it to `false` for production
    @SuppressWarnings("unused")
    public boolean offlineMode=false;  //TODO: Be sure to put it to `false` for production
}
