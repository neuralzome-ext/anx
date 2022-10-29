package com.flomobility.hermes.di

import android.content.Context
import android.content.SharedPreferences
import android.hardware.usb.UsbManager
import androidx.core.app.NotificationCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.flomobility.hermes.R
import com.flomobility.hermes.network.FloApiService
import com.flomobility.hermes.other.Constants
import com.flomobility.hermes.other.Constants.NOTIFICATION_CHANNEL_ID
import com.flomobility.hermes.other.GsonUtils
import com.flomobility.hermes.repositories.FloRepository
import com.flomobility.hermes.repositories.FloRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
        .setContentText("No active session.")

    @Singleton
    @Provides
    fun providesGson() = GsonUtils.getGson()

    @Singleton
    @Provides
    fun providesUsbManager(@ApplicationContext app: Context) =
        app.getSystemService(Context.USB_SERVICE) as UsbManager

    @Singleton
    @Provides
    fun providesFloApiService(): FloApiService {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FloApiService::class.java)
    }

    @Singleton
    @Provides
    fun providesFloRepository(
        floApi: FloApiService,
        sharedPreferences: SharedPreferences
    ): FloRepository = FloRepositoryImpl(floApi, sharedPreferences)

    val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    @Singleton
    @Provides
    fun providesSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences = EncryptedSharedPreferences.create(
        "user",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
//    context.getSharedPreferences("user", AppCompatActivity.MODE_PRIVATE)

}