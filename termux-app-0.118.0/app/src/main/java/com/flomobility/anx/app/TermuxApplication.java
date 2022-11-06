package com.flomobility.anx.app;

import android.app.Application;

import com.flomobility.anx.shared.crash.TermuxCrashUtils;
import com.flomobility.anx.shared.settings.preferences.TermuxAppSharedPreferences;
import com.flomobility.anx.shared.logger.Logger;

import dagger.hilt.android.HiltAndroidApp;
import timber.log.Timber;

@HiltAndroidApp
public class TermuxApplication extends Application {
    public void onCreate() {
        super.onCreate();

        // Set crash handler for the app
        TermuxCrashUtils.setCrashHandler(this);

        // Set log level for the app
        setLogLevel();

        Timber.plant(new Timber.DebugTree());

    }

    private void setLogLevel() {
        // Load the log level from shared preferences and set it to the {@link Logger.CURRENT_LOG_LEVEL}
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(getApplicationContext());
        if (preferences == null) return;
        preferences.setLogLevel(null, preferences.getLogLevel());
        Logger.logDebug("Starting Application");
    }
}

