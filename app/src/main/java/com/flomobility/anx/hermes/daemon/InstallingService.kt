package com.flomobility.anx.hermes.daemon

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.downloader.Priority
import com.flomobility.anx.R
import com.flomobility.anx.app.PluginResultsService
import com.flomobility.anx.app.TerminalCommandExecutor
import com.flomobility.anx.hermes.other.Constants
import com.flomobility.anx.hermes.other.getIsInstalled
import com.flomobility.anx.hermes.other.provideDispatcher
import com.flomobility.anx.hermes.other.setIsInstalled
import com.flomobility.anx.hermes.ui.download.DownloadActivity
import com.flomobility.anx.hermes.ui.download.DownloadManager
import com.flomobility.anx.shared.logger.Logger
import com.flomobility.anx.shared.terminal.TerminalConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.net.UnknownHostException
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class InstallingService : LifecycleService() {

    @Inject
    lateinit var downloadManager: DownloadManager

    private var isRunning = false

    private val LOG_TAG = "InstallingService"

    private var downloadId = 0

    private var downloadProgress = 0L
    private var oldDownloadProgress = 0L

    companion object {
        val events = Channel<Events>()
        val eventsFlow = events.receiveAsFlow()

        var state: Events = Events.NotStarted
    }

    sealed class Events {
        object NotStarted : Events()
        object CreateInstallScript : Events()
        object Downloading : Events()
        data class DownloadProgress(val progress: Int) : Events()
        data class DownloadFailed(val error: String) : Events()
        object DownloadComplete : Events()
        object Installing : Events()
        class InstallingFailed(val code: Int) : Events()
        object InstallingSuccess : Events()
    }

    private lateinit var currentNotificationBuilder: NotificationCompat.Builder

    inner class LocalBinder : Binder() {
        fun getService(): InstallingService? {
            return this@InstallingService
        }
    }

    private val mBinder: IBinder = LocalBinder()

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private val dispatcher = provideDispatcher(nThreads = 2)

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
                            onInstallationDone(exitCode)
                        }
                    }
                }
            }
        }
    }

    private fun onInstallationDone(exitCode: Int) {
        currentNotificationBuilder = if (exitCode == 0) {
            sharedPreferences.setIsInstalled(true)
            Timber.e("Installation Complete")
            sendEvent(Events.InstallingSuccess)
            NotificationCompat.Builder(
                this@InstallingService.applicationContext,
                Constants.INSTALLER_NOTIFICATION_CHANNEL_ID
            )
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.ic_flo)
                .setContentText("Installation Complete")
                .setContentTitle("File system installed successfully!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
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
        } else {
            Timber.e("Installation Failed")
            NotificationCompat.Builder(
                this@InstallingService.applicationContext,
                Constants.INSTALLER_NOTIFICATION_CHANNEL_ID
            )
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.ic_flo)
                .setContentText("Installation Error")
                .setContentTitle("Error installing file system. Code - $exitCode")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
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
            Constants.INSTALLER_SERVICE_NOTIFICATION_ID,
            currentNotificationBuilder.build()
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                Constants.ACTION_START_OR_RESUME_SERVICE -> {
                    if (!isRunning) {
                        startEndlessService()
                        downloadManager.installFunc = {
                            checkInstalled()
                        }
                        downloadManager.retryFunc = {
                            startDownload()
                        }
                        Timber.d("Started service")
                    }
                }
                Constants.ACTION_STOP_SERVICE -> {
                    killService()
                    Timber.d("Stopped service")
                }
                Constants.ACTION_STOP_AND_EXIT -> {
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

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Logger.logVerbose(LOG_TAG, "onBind")
        return mBinder
    }

    private fun startEndlessService() {
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(PluginResultsService.RESULT_BROADCAST_INTENT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        FILE_PATH = filesDir.absolutePath

        currentNotificationBuilder =
            NotificationCompat.Builder(
                this.applicationContext,
                Constants.INSTALLER_NOTIFICATION_CHANNEL_ID
            )
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_flo)
                .setContentTitle("File system installer")
                .setContentText("Checking...")
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
        startForeground(
            Constants.INSTALLER_SERVICE_NOTIFICATION_ID,
            currentNotificationBuilder.build()
        )

        startDownload()

        isRunning = true
    }

    private fun killService() {
        isRunning = false
        PRDownloader.cancelAll()
        actionStopService()
        if(this::currentNotificationBuilder.isInitialized) {
            currentNotificationBuilder.clearActions()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            Constants.INSTALLER_NOTIFICATION_CHANNEL_ID,
            Constants.INSTALLER_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    private fun startDownload() {
        try {
            if (File("$FILE_PATH/$FILE_NAME").exists()) {
                checkInstalled()
                return
            }
            if (!isNetworkAvailable(this)) {
                sendEvent(Events.DownloadFailed(error = "Network Unavailable"))
                return
            }
            val downloader = PRDownloader.download(
                FILE_URl,
                File(FILE_PATH).absolutePath,
                FILE_NAME
            )
                .build()
                .setOnStartOrResumeListener {
                    sendEvent(Events.Downloading)
                    currentNotificationBuilder
                        .setContentTitle("File system installer")
                        .setContentText("Downloading file system")
                        .setProgress(100, 0, true)
                    notificationManager.notify(
                        Constants.INSTALLER_SERVICE_NOTIFICATION_ID,
                        currentNotificationBuilder.build()
                    )
                    startDownloadProgressPublisher()
                }.setOnPauseListener { /*NO-OP*/ }
                .setOnCancelListener { /*NO-OP*/ }
                .setOnProgressListener {
                    downloadProgress = ((it.currentBytes * 100) / it.totalBytes)
                    Timber.d("Prog : $downloadProgress")
                }
            downloadId = downloader.start(object : OnDownloadListener {
                override fun onDownloadComplete() {
                    Timber.d("download complete")
                    sendEvent(Events.DownloadComplete)
                    currentNotificationBuilder
                        .setContentTitle("File system installer")
                        .setContentText("Downloading done")
                        .setProgress(0, 0, false)
                    notificationManager.notify(
                        Constants.INSTALLER_SERVICE_NOTIFICATION_ID,
                        currentNotificationBuilder.build()
                    )

                    checkInstalled()
                }

                override fun onError(error: com.downloader.Error?) {
                    Timber.e("download Error ${error?.connectionException?.message} ${error?.connectionException?.javaClass}")
                    if (error?.connectionException?.javaClass == UnknownHostException::class.java)
                        sendEvent(Events.DownloadFailed("Server not reachable"))
                    else
                        sendEvent(
                            Events.DownloadFailed(
                                error?.connectionException?.message ?: Constants.UNKNOWN_ERROR_MSG
                            )
                        )
                    currentNotificationBuilder = NotificationCompat.Builder(
                        this@InstallingService.applicationContext,
                        Constants.INSTALLER_NOTIFICATION_CHANNEL_ID
                    )
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setOngoing(false)
                        .setAutoCancel(true)
                        .setSmallIcon(R.drawable.ic_flo)
                        .setContentTitle("File system installer")
                        .setContentText("Downloading failed.")
                    notificationManager.notify(
                        Constants.INSTALLER_SERVICE_NOTIFICATION_ID,
                        currentNotificationBuilder.build()
                    )
                }
            })
        } catch (err: Exception) {
            Timber.e("download $err ${err.cause}")
            sendEvent(Events.DownloadFailed("Download failed with an unknown error"))
        }
    }

    private fun startDownloadProgressPublisher() {
        Thread {
            while (state is Events.DownloadProgress || state is Events.Downloading) {
                if (downloadProgress != oldDownloadProgress) {
                    oldDownloadProgress = downloadProgress
                    sendEvent(Events.DownloadProgress(downloadProgress.toInt()))
                    currentNotificationBuilder
                        .setContentTitle("File system installer")
                        .setContentText("Downloading file system")
                        .setProgress(100, downloadProgress.toInt(), false)
                    notificationManager.notify(
                        Constants.INSTALLER_SERVICE_NOTIFICATION_ID,
                        currentNotificationBuilder.build()
                    )
                }
            }
        }.start()
    }

    private fun checkInstalled() {
        if (sharedPreferences.getIsInstalled()) {
            sendEvent(Events.InstallingSuccess)
            return
        }
        lifecycleScope.launch {
            // Install here
            val terminalCommandExecutor =
                TerminalCommandExecutor.getInstance(this@InstallingService)
            terminalCommandExecutor.startCommandExecutor(object :
                TerminalCommandExecutor.ITerminalCommandExecutor {
                override fun onEndlessServiceConnected() {
                    sendEvent(Events.Installing)
                    currentNotificationBuilder =
                        NotificationCompat.Builder(
                            this@InstallingService,
                            Constants.INSTALLER_NOTIFICATION_CHANNEL_ID
                        )
                            .setAutoCancel(false)
                            .setOngoing(true)
                            .setSmallIcon(R.drawable.ic_flo)
                            .setContentTitle("File system installer")
                            .setContentText("Installing")
                            .setContentIntent(
                                PendingIntent.getActivity(
                                    this@InstallingService,
                                    0,
                                    Intent(this@InstallingService, DownloadActivity::class.java),
                                    0
                                )
                            )
                            .setProgress(100, 0, true)
                    notificationManager.notify(
                        Constants.INSTALLER_SERVICE_NOTIFICATION_ID,
                        currentNotificationBuilder.build()
                    )
                    terminalCommandExecutor.executeCommand(
                        this@InstallingService,
                        "bash",
                        arrayOf("install.sh"),
                        DownloadActivity.INSTALL_FS_EXECUTION_CODE
                    )
                }

                override fun onEndlessServiceDisconnected() {
                    sendEvent(Events.InstallingFailed(8000))
                    currentNotificationBuilder =
                        NotificationCompat.Builder(
                            this@InstallingService,
                            Constants.INSTALLER_NOTIFICATION_CHANNEL_ID
                        )
                            .setAutoCancel(false)
                            .setSmallIcon(R.drawable.ic_flo)
                            .setContentTitle("File system installer")
                            .setContentText("Installation Failed")
                            .setContentIntent(
                                PendingIntent.getActivity(
                                    this@InstallingService,
                                    0,
                                    Intent(this@InstallingService, DownloadActivity::class.java),
                                    0
                                )
                            )
                    notificationManager.notify(
                        Constants.INSTALLER_SERVICE_NOTIFICATION_ID,
                        currentNotificationBuilder.build()
                    )
                }
            })
        }
    }

    private fun sendEvent(event: Events) {
        lifecycleScope.launch {
            events.send(event)
            state = event
        }
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

    private val FILE_NAME = "ubuntu-latest.tar.gz"
    private var FILE_PATH = ""
    private val FILE_URl = "https://flo-linux-fs.s3.ap-south-1.amazonaws.com/ubuntu-latest.tar.gz"
}
