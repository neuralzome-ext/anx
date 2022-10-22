package com.termux.hermes.di

import android.content.Context
import android.hardware.usb.UsbManager
import androidx.core.app.NotificationCompat
import com.termux.R
import com.termux.hermes.other.Constants.NOTIFICATION_CHANNEL_ID
import com.termux.hermes.other.GsonUtils
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
        .setSmallIcon(R.drawable.ic_foreground)
        .setContentTitle("Hermes service")
        .setContentText("No active session.")

    @Singleton
    @Provides
    fun providesGson() = GsonUtils.getGson()

    @Singleton
    @Provides
    fun providesUsbManager(@ApplicationContext app: Context) =
        app.getSystemService(Context.USB_SERVICE) as UsbManager

}
