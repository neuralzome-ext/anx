package com.flomobility.anx

import android.app.Application
import android.content.Context
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import com.flomobility.anx.shared.crash.TerminalCrashUtils
import com.flomobility.anx.shared.logger.Logger
import com.flomobility.anx.shared.settings.preferences.FloAppSharedPreferences
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class FloApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        val config = PRDownloaderConfig.newBuilder()
            .setDatabaseEnabled(true)
            .setReadTimeout(30000)
            .setConnectTimeout(30000)
            .build()
        PRDownloader.initialize(applicationContext,config)

        // Timber logging
        Timber.plant(Timber.DebugTree())

        // Set crash handler for the app
        TerminalCrashUtils.setCrashHandler(this)

        // Set log level for the app (Terminal
        setLogLevel()
    }

    private fun setLogLevel() {
        // Load the log level from shared preferences and set it to the {@link Logger.CURRENT_LOG_LEVEL}
        val preferences = FloAppSharedPreferences.build(applicationContext) ?: return
        preferences.setLogLevel(null, preferences.logLevel)
        Logger.logDebug("Starting Application")
    }

    object BuildType {
        const val DEV = "dev"
        const val STAGING = "staging"
        const val RELEASE = "release"
    }

    companion object {
        lateinit  var appContext: Context
    }

}
