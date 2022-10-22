package com.termux.hermes.daemon

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.termux.R
import com.termux.hermes.assets.AssetManager
import com.termux.hermes.comms.SessionManager
import com.termux.hermes.comms.SocketManager
import com.termux.hermes.other.Constants
import com.termux.hermes.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.termux.hermes.other.Constants.ACTION_STOP_SERVICE
import com.termux.hermes.other.Constants.NOTIFICATION_CHANNEL_ID
import com.termux.hermes.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.termux.hermes.other.Constants.NOTIFICATION_ID
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class EndlessService : LifecycleService() {

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    private lateinit var currentNotificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var socketManager: SocketManager

    @Inject
    lateinit var assetManager: AssetManager

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    startEndlessService()
                    Timber.d("Stated service")
                }
                ACTION_STOP_SERVICE -> {
                    killService()
                    Timber.d("Stopped service")
                }
                else -> { /*NO-OP*/
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startEndlessService() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }
        val pendingIntent =
            PendingIntent.getService(
                this,
                1,
                Intent(this, EndlessService::class.java).apply {
                    action = ACTION_STOP_SERVICE
                },
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        currentNotificationBuilder =
            baseNotificationBuilder.addAction(R.drawable.star, "Exit", pendingIntent)
        startForeground(NOTIFICATION_ID, currentNotificationBuilder.build())

        // init
        socketManager.init()
        socketManager.doOnSubscribed { subscribed ->
            Handler(Looper.getMainLooper()).postDelayed({
                currentNotificationBuilder.setContentText(
                    if (subscribed) {
                        "Active session <-> ${sessionManager.connectedDeviceIp}"
                    } else {
                        "No active session."
                    }
                )
                notificationManager.notify(NOTIFICATION_ID, currentNotificationBuilder.build())
            }, 10L)
        }

        assetManager.init()
    }

    private fun killService() {
        socketManager.destroy()
        assetManager.stopAllAssets()
        stopForeground(true)
        stopSelf()
        exitProcess(0)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

}
