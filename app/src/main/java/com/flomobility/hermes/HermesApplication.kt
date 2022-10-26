package com.flomobility.hermes

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class HermesApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        HermesApplication.appContext = applicationContext
        Timber.plant(Timber.DebugTree())
    }

    companion object {
        lateinit  var appContext: Context
    }

}