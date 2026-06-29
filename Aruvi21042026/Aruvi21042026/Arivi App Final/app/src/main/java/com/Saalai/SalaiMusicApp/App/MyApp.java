package com.Saalai.SalaiMusicApp.App;

import android.app.Application;

import com.Saalai.SalaiMusicApp.PlayerManager;

public class MyApp extends Application {

    @Override
    public void onTerminate() {
        super.onTerminate();

        PlayerManager.stopNotification(); // 🔥 Stop notification

    }

}
