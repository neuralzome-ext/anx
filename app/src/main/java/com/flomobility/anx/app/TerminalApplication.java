package com.flomobility.anx.app;

import android.app.Application;

import com.flomobility.anx.shared.crash.TerminalCrashUtils;
import com.flomobility.anx.shared.settings.preferences.FloAppSharedPreferences;
import com.flomobility.anx.shared.logger.Logger;

import timber.log.Timber;

public class TerminalApplication extends Application {
    public void onCreate() {
        super.onCreate();

        // Set crash handler for the app
        TerminalCrashUtils.setCrashHandler(this);

        // Set log level for the app
        setLogLevel();

        Timber.plant(new Timber.DebugTree());

    }

    private void setLogLevel() {
        // Load the log level from shared preferences and set it to the {@link Logger.CURRENT_LOG_LEVEL}
        FloAppSharedPreferences preferences = FloAppSharedPreferences.build(getApplicationContext());
        if (preferences == null) return;
        preferences.setLogLevel(null, preferences.getLogLevel());
        Logger.logDebug("Starting Application");
    }
}

