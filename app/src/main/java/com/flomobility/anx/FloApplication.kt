package com.flomobility.anx

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class FloApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

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
