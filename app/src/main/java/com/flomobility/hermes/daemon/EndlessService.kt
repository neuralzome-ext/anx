package com.flomobility.hermes.daemon

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.flomobility.hermes.comms.SocketManager
import com.flomobility.hermes.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.flomobility.hermes.other.Constants.ACTION_STOP_SERVICE
import com.flomobility.hermes.other.Constants.NOTIFICATION_CHANNEL_ID
import com.flomobility.hermes.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.flomobility.hermes.other.Constants.NOTIFICATION_ID
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class EndlessService: LifecycleService() {

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var socketManager: SocketManager

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
                else -> { /*NO-OP*/ }
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
        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

        // init
        socketManager.init()
    }

    private fun killService() {

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