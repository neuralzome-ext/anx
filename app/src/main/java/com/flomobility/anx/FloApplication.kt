package com.flomobility.anx

import android.app.Application
import android.content.Context
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class FloApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        try {
            val config = PRDownloaderConfig.newBuilder()
                .setDatabaseEnabled(true)
                .setReadTimeout(30000)
                .setConnectTimeout(30000)
                .build()
            PRDownloader.initialize(applicationContext,config)
        } catch (e: Exception) {
            Timber.e(e)
        }

        // Timber logging
        Timber.plant(Timber.DebugTree())

    }

    object BuildType {
        const val DEV = "dev"
        const val STAGING = "staging"
        const val RELEASE = "release"
        const val HEADLESS = "headless"
    }

    companion object {
        lateinit  var appContext: Context
    }

}
