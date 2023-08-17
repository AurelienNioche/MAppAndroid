package com.aureliennioche.mapp.websocket;

import com.aureliennioche.mapp.config.Config;

public class LoginRequest {
    public String subject = "login";
    public String username = "<needs to be defined by the user>";
    public boolean resetUser = Config.askServerToResetUser;
    public String appVersion = Config.appVersion;
}
