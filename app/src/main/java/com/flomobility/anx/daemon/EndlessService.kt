package com.flomobility.anx.daemon

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.flomobility.anx.EnvUtils
import com.flomobility.anx.R
import com.flomobility.anx.activity.MainActivity
import com.flomobility.anx.comms.DeviceRpcHandler
import com.flomobility.anx.other.Constants
import com.flomobility.anx.other.Constants.ACTION_STOP_AND_EXIT
import com.flomobility.anx.other.Constants.NOTIFICATION_CHANNEL_ID
import com.flomobility.anx.other.Constants.NOTIFICATION_CHANNEL_NAME
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class EndlessService : Service() {

    @Inject
    lateinit var deviceRpcHandler: DeviceRpcHandler

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    private lateinit var currentNotificationBuilder: NotificationCompat.Builder

    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                Constants.ACTION_START_SERVICE -> {
                   if(!isRunning) {
                       startAnxService()
                       Timber.d("Started service")
                   }
                }
                Constants.ACTION_STOP_SERVICE -> {
                    stopAnxService()
                    Timber.d("Stopped service")
                }
                Constants.ACTION_STOP_AND_EXIT -> {
                    Timber.d("Stopped service")
                    EnvUtils.execService(baseContext, "stop", "-u")
                    stopAnxService()
                    exitProcess(0)
                }
                else -> { /*NO-OP*/ }
            }
        }
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun startAnxService() {
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
        currentNotificationBuilder = currentNotificationBuilder
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    0
                )
            )
        currentNotificationBuilder =
            currentNotificationBuilder.addAction(R.drawable.ic_stop, "Exit", pendingIntent)
//        startForeground(NOTIFICATION_ID, currentNotificationBuilder.build())
        isRunning = true

        deviceRpcHandler.init(10002)
    }

    private fun stopAnxService() {

        isRunning = false

        if (this::currentNotificationBuilder.isInitialized) {
            currentNotificationBuilder.clearActions()
        }
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
