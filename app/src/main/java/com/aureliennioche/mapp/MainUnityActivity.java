package com.aureliennioche.mapp;

import android.content.Intent;
import android.os.Bundle;

import com.unity3d.player.UnityPlayerActivity;

public class MainUnityActivity extends UnityPlayerActivity {
    public static MainUnityActivity instance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        handleIntent(intent);

        instance = this;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
        setIntent(intent);
    }

    void handleIntent(Intent intent) {
        if(intent == null || intent.getExtras() == null) return;

        if(intent.getExtras().containsKey("doQuit"))
            if(mUnityPlayer != null) {
                finish();
            }
    }
}
