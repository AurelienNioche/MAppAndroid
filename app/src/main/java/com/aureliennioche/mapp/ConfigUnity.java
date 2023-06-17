package com.aureliennioche.mapp;

public class ConfigUnity {
    @SuppressWarnings("unused")
    // Macbook pro at home: "ws://192.168.0.101:8000/ws"
    // Uni: "wss://samoa.dcs.gla.ac.uk/mapp/ws"
    // "ws://192.168.0.101:8000/ws"
    // wss://samoa.dcs.gla.ac.uk/mapp/ws
    public String websocketUrl="ws://192.168.0.101:8000/ws";  //TODO: Be sure to set that correctly for production
    @SuppressWarnings("unused")
    public double websocketTimeReconnection = 4;
    @SuppressWarnings("unused")
    public double androidUpdateFrequencySeconds = 1;
    @SuppressWarnings("unused")
    public double serverUpdateFrequencySeconds = 10;
    @SuppressWarnings("unused")
    public double loadingViewDelayBeforeNextView = 0.5;
    @SuppressWarnings("unused")
    public String loginInputFieldDefaultUsername = "123test"; //TODO: Be sure to put it to `""` for production
    @SuppressWarnings("unused")
    public boolean askServerToResetUser = true;  //TODO: Be sure to put it to `false` for production
    @SuppressWarnings("unused")
    public boolean offlineMode = false;  //TODO: Be sure to put it to `false` for production

    //TODO: Check as well the Android settings
}
