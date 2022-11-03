package com.flomobility.hermes

import android.app.Application
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class HermesApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        val config = PRDownloaderConfig.newBuilder()
            .setDatabaseEnabled(true)
            .setReadTimeout(30000)
            .setConnectTimeout(30000)
            .build()
        PRDownloader.initialize(applicationContext,config)
        Timber.plant(
            when (BuildConfig.BUILD_TYPE) {
                BuildType.DEV -> {
                    Timber.DebugTree()
                }
                else -> {
                    Timber.DebugTree()
                }
            }
        )
    }

    object BuildType {
        const val DEV = "dev"
        const val STAGING = "staging"
        const val RELEASE = "release"
    }

}