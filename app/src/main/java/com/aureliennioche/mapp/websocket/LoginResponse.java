package com.aureliennioche.mapp.websocket;

import com.aureliennioche.mapp.config.Config;

public class LoginResponse {
    @SuppressWarnings("unused")
    public String subject;
    public boolean ok;
    public String challengeList = "<to be filled by the server>";  // JSON
    public String stepList = "<to be filled by the server>";  // JSON
    public String status = "<to be filled by the server>";
    public String username = "<to be filled by the server>";    // To avoid to have to pass out too much arguments back to Android
    public String appVersion = Config.appVersion;
}
