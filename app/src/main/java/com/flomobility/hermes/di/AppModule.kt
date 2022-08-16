package com.flomobility.hermes.di

import android.content.Context
import androidx.core.app.NotificationCompat
import com.flomobility.hermes.R
import com.flomobility.hermes.other.Constants.NOTIFICATION_CHANNEL_ID
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideBaseNotificationBuilder(
        @ApplicationContext app: Context
    ) = NotificationCompat.Builder(app, NOTIFICATION_CHANNEL_ID)
        .setAutoCancel(false)
        .setOngoing(true)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Hermes service")
        .setContentText("")

    @Singleton
    @Provides
    fun providesGson() = GsonBuilder()
        .setPrettyPrinting()
        .create()

}