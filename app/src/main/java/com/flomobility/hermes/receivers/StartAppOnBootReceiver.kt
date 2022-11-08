package com.flomobility.hermes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import com.flomobility.hermes.di.AppModule
import com.flomobility.hermes.other.getIsOnBoot
import com.flomobility.hermes.ui.splash.SplashActivity


class StartAppOnBootReceiver() : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val sharedPreferences = EncryptedSharedPreferences.create(
            "user",
            AppModule.masterKeyAlias,
            context ?: return,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        if (sharedPreferences.getIsOnBoot())
            if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
                val i = Intent(context, SplashActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
//            runAsRoot(cmd = "am start com.flomobility.hermes/com.flomobility.hermes.MainActivity")
            }
    }
}