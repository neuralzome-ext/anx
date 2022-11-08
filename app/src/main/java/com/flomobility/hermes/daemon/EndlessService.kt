package com.flomobility.hermes.daemon

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
import com.flomobility.hermes.R
import com.flomobility.hermes.assets.AssetManager
import com.flomobility.hermes.comms.SessionManager
import com.flomobility.hermes.comms.SocketManager
import com.flomobility.hermes.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.flomobility.hermes.other.Constants.ACTION_STOP_AND_EXIT
import com.flomobility.hermes.other.Constants.ACTION_STOP_SERVICE
import com.flomobility.hermes.other.Constants.NOTIFICATION_CHANNEL_ID
import com.flomobility.hermes.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.flomobility.hermes.other.Constants.NOTIFICATION_ID
import com.flomobility.hermes.other.ThreadStatus
import com.flomobility.hermes.usb.UsbPortManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class EndlessService : LifecycleService() {

    private var isRunning = false

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    private lateinit var currentNotificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var socketManager: SocketManager

    @Inject
    lateinit var usbPortManager: UsbPortManager

    @Inject
    lateinit var assetManager: AssetManager

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if(!isRunning) {
                        startEndlessService()
                        Timber.d("Started service")
                    }
                }
                ACTION_STOP_SERVICE -> {
                    killService()
                    Timber.d("Stopped service")
                }
                ACTION_STOP_AND_EXIT -> {
                    killService()
                    Timber.d("Stopped service")
                    exitProcess(0)
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
                    action = ACTION_STOP_AND_EXIT
                },
                if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            )
        currentNotificationBuilder = baseNotificationBuilder
        currentNotificationBuilder = currentNotificationBuilder.addAction(R.drawable.ic_stop, "Exit", pendingIntent)
        startForeground(NOTIFICATION_ID, currentNotificationBuilder.build())

        // init
        if (socketManager.threadStatus != ThreadStatus.ACTIVE) {
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
        }
        if (usbPortManager.threadStatus != ThreadStatus.ACTIVE) usbPortManager.init()
        if (assetManager.threadStatus != ThreadStatus.ACTIVE) assetManager.init()
        isRunning = true
    }

    private fun killService() {
        socketManager.destroy()
        assetManager.stopAllAssets()
        stopForeground(true)
        stopSelf()
        currentNotificationBuilder.clearActions()
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