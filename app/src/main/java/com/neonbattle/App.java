package com.m7s.battle;

import android.app.Application;

public class App extends Application {
    @Override public void onCreate() {
        super.onCreate();
        Ui.init(this);
        Api.init(this);
        Img.init(this);
        Push.createChannel(this);
        Push.ensureInit(this); // uses the Firebase settings saved from the server on the last start
    }
}
