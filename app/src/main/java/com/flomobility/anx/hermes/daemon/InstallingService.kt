package com.flomobility.anx.hermes.daemon

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.flomobility.anx.R
import com.flomobility.anx.app.PluginResultsService
import com.flomobility.anx.hermes.other.Constants
import com.flomobility.anx.hermes.other.Event
import com.flomobility.anx.hermes.other.setIsInstalled
import com.flomobility.anx.hermes.ui.download.DownloadActivity
import com.flomobility.anx.hermes.ui.download.DownloadViewModel
import com.flomobility.anx.shared.logger.Logger
import com.flomobility.anx.shared.terminal.TerminalConstants
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class InstallingService : LifecycleService() {

    private var isRunning = false

    private val LOG_TAG = "InstallingService"

    private val _installStatus = MutableLiveData<Event<DownloadViewModel.InstallStatus>>()
    val installStatus: LiveData<Event<DownloadViewModel.InstallStatus>> = _installStatus

//    @Inject
//    lateinit var installerNotificationBuilder: NotificationCompat.Builder

    private lateinit var currentNotificationBuilder: NotificationCompat.Builder

    inner class LocalBinder : Binder() {
        fun getService(): InstallingService? {
            return this@InstallingService
        }
    }

    private val mBinder: IBinder = LocalBinder()

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                val executionCode =
                    intent.getIntExtra(PluginResultsService.RESULT_BROADCAST_EXECUTION_CODE_KEY, -1)
                val result = intent.getBundleExtra(PluginResultsService.RESULT_BROADCAST_RESULT_KEY)
                when (executionCode) {
                    DownloadActivity.INSTALL_FS_EXECUTION_CODE -> {
                        result?.let {
                            val exitCode =
                                result.getInt(TerminalConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE)
                            val notificationManager =
                                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            currentNotificationBuilder = if (exitCode == 0) {
                                sharedPreferences.setIsInstalled(true)
                                Timber.e("Installation Complete")
                                currentNotificationBuilder.setProgress(100, 100, false)
                                    .setContentText("Installation Complete")
                                    .setContentIntent(
                                        PendingIntent.getActivity(
                                            this@InstallingService,
                                            0,
                                            Intent(
                                                this@InstallingService,
                                                DownloadActivity::class.java
                                            ),
                                            0
                                        )
                                    )

//                                viewModel.setInstallStatus(DownloadViewModel.InstallStatus.Success)
                            } else {
                                Timber.e("Installation Failed")
                                currentNotificationBuilder.setProgress(100, 0, false)
                                    .setContentText("Installation Failed")
                                    .setContentIntent(
                                        PendingIntent.getActivity(
                                            this@InstallingService,
                                            0,
                                            Intent(
                                                this@InstallingService,
                                                DownloadActivity::class.java
                                            ),
                                            0
                                        )
                                    )

                            }
                            notificationManager.notify(
                                Constants.INSTALLER_ID,
                                currentNotificationBuilder.build()
                            )
//                            viewModel.setInstallStatus(
//                                DownloadViewModel.InstallStatus.Failed(
//                                    exitCode
//                                )
//                            )
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                Constants.ACTION_START_OR_RESUME_SERVICE -> {
                    if (!isRunning) {
                        startEndlessService()
                        Timber.d("Started service")
                    }
                }
                Constants.ACTION_STOP_SERVICE -> {
                    killService()
                    actionStopService();
                    Timber.d("Stopped service")
                }
                Constants.ACTION_STOP_AND_EXIT -> {
                    killService()
                    actionStopService();
                    Timber.d("Stopped service")
                    exitProcess(0)
                }
                else -> { /*NO-OP*/
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // NotificationUtils.setupNotificationChannel(this, TermuxConstants.TERMUX_APP_NOTIFICATION_CHANNEL_ID,
        //   TermuxConstants.TERMUX_APP_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Logger.logVerbose(LOG_TAG, "onBind")
        return mBinder
    }

    private fun startEndlessService() {
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(PluginResultsService.RESULT_BROADCAST_INTENT))
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }
        val pendingIntent =
            PendingIntent.getService(
                this,
                1,
                Intent(this, InstallingService::class.java),
                if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            )

        currentNotificationBuilder =
            NotificationCompat.Builder(this.applicationContext, Constants.INSTALLER_CHANNEL_ID)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_flo)
                .setContentTitle("FS Installer")
                .setContentText("Installation in progress")
                .setContentIntent(
                    PendingIntent.getActivity(
                        this@InstallingService,
                        0,
                        Intent(this, DownloadActivity::class.java),
                        0
                    )
                )
                .setProgress(100, 0, true)
//        currentNotificationBuilder =
//            currentNotificationBuilder.addAction(R.drawable.ic_stop, "Open", pendingIntent)
        startForeground(Constants.INSTALLER_ID, currentNotificationBuilder.build())

        isRunning = true
    }

    private fun killService() {
        stopForeground(true)
        stopSelf()
        currentNotificationBuilder.clearActions()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            Constants.INSTALLER_CHANNEL_ID,
            Constants.INSTALLER_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    fun setInstallStatus(status: DownloadViewModel.InstallStatus) {
        _installStatus.postValue(Event(status))
    }

    /** Process action to stop service.  */
    private fun actionStopService() {
        requestStopService()
    }

    /** Make service leave foreground mode.  */
    private fun runStopForeground() {
        stopForeground(true)
    }

    /** Request to stop service.  */
    private fun requestStopService() {
        Logger.logDebug(LOG_TAG, "Requesting to stop service")
        runStopForeground()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.logVerbose(LOG_TAG, "onDestroy")
        runStopForeground()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

}
