package com.aureliennioche.mapp;

public class LoginRequest {
    public String subject = "login";
    public String username = "<needs to be defined by the user>";
    public boolean resetUser = ConfigAndroid.askServerToResetUser;
    public String appVersion = ConfigAndroid.appVersion;
}
