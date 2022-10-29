package com.flomobility.hermes.ui.download

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.downloader.Status.*
import com.flomobility.hermes.R
import com.flomobility.hermes.databinding.ActivityUbuntuSetupBinding
import com.flomobility.hermes.other.clear
import com.flomobility.hermes.other.viewutils.AlertDialog
import com.flomobility.hermes.ui.dashboard.DashboardActivity
import com.flomobility.hermes.ui.login.LoginActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.net.UnknownHostException
import javax.inject.Inject

@AndroidEntryPoint
class DownloadActivity : AppCompatActivity() {

    private var binding: ActivityUbuntuSetupBinding? = null
    private val bind get() = binding!!
    private lateinit var viewModel: DownloadViewModel
    private var downloadId = 0

    private val FILE_NAME = "sshSetup.zip"
    private var FILE_PATH = ""
    private val FILE_URl = "https://android-setup-files.s3.ap-south-1.amazonaws.com/sshSetup.zip"

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    companion object {
        fun navigateToDownload(context: Context) {
            context.startActivity(Intent(context, DownloadActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUbuntuSetupBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this@DownloadActivity)[DownloadViewModel::class.java]
        setContentView(binding?.root)
        FILE_PATH = filesDir.absolutePath + "/termux"
        setEventListeners()
        checkDownload()
    }

    private fun setEventListeners() {
        bind.apply {
            retry.setOnClickListener {
                errorLayout.visibility = View.GONE
                progressLayout.visibility = View.VISIBLE
                startDownload()
            }
            pauseResume.setOnClickListener {
//                Timber.d("downloadStatus ${PRDownloader.getStatus(0)}")
                val status = PRDownloader.getStatus(downloadId)
                when (status) {
                    PAUSED -> {
                        PRDownloader.resume(downloadId)
                    }
                    QUEUED -> {
                        showSnack("Queued")
                    }
                    RUNNING -> {
                        PRDownloader.pause(downloadId)
                    }
                    COMPLETED -> {
                        DashboardActivity.navigateToDashboard(this@DownloadActivity)
                    }
                    CANCELLED -> {
                        startDownload()
                    }
                    FAILED -> {
                        startDownload()
                    }
                    UNKNOWN -> {
                        startDownload()
                    }
                    null -> {
                        startDownload()
                    }
                }
//                Timber.d("downloadStatus ${PRDownloader.getStatus(downloadId)}")
//                PRDownloader.pause(downloadId)
//                PRDownloader.pause(0)
            }
            exit.setOnClickListener {
                AlertDialog.getInstance(
                    "Confirm Logout",
                    "Logging out will delete the current download progress",
                    "Logout",
                    "Cancel",
                    yesListener = {
                        PRDownloader.cancelAll()
                        sharedPreferences.clear()
                        LoginActivity.navigateToLogin(this@DownloadActivity)
                        finish()
                    }
                ).show(supportFragmentManager, AlertDialog.TAG)
/*
                AlertDialog.Builder(this@DownloadActivity, R.style.AlertDialogTheme).apply {
                    setTitle("Confirm Logout")
                    setMessage("Logging out will delete the current download progress")
                    setIcon(R.drawable.ic_warning)
                    setCancelable(false)
                    setPositiveButton("Logout") { _, _ ->
                        PRDownloader.cancelAll()
                        sharedPreferences.clear()
                        LoginActivity.navigateToLogin(this@DownloadActivity)
                        finish()
                    }
                    setNegativeButton("Cancel") { _, _ ->
                        //Do Nothing
                    }
                        .create()
                        .show()
                }
*/
            }
        }
    }

    private fun checkDownload() {
        if (File("$FILE_PATH/$FILE_NAME").exists()) {
            checkInstalled()
            return
        }
        startDownload()
        /*Timber.d(
            "download ${File(filesDir.absolutePath + "/termux").isDirectory} ${File(filesDir.absolutePath + "/termux").list().size} ${
                File(
                    filesDir.absolutePath + "/termux"
                ).list()[0]
            } "
        )

        Timber.d("downloadStatus ${PRDownloader.getStatus(downloadId)}")*/
    }

    private fun checkInstalled() {
//        if(sharedPreferences.getIsInstalled()) {
//            DashboardActivity.navigateToDashboard(this@DownloadActivity)
//            finish()
//            return
//        }
        bind.downloading1.text = "INSTALLING FILE SYSTEM"
        bind.progress.visibility = View.INVISIBLE
        bind.progressIndeterminate.visibility = View.VISIBLE
        bind.progressPercent.text = ""
        CoroutineScope(Dispatchers.Main).launch {
            delay(5000)
            runOnUiThread {
                DashboardActivity.navigateToDashboard(this@DownloadActivity)
                finish()
            }
        }
    }

    private fun startDownload() {
        try {
            if (!isNetworkAvailable(this@DownloadActivity)) {
                showSnack("Network Unavailable")
                bind.errorMsg.text = "Network Unavailable"
                bind.progressLayout.visibility = View.GONE
                bind.errorLayout.visibility = View.VISIBLE
                return
            }
            val downloader = PRDownloader.download(
                FILE_URl,
                File(FILE_PATH).absolutePath,
                FILE_NAME
            ).build()
                .setOnStartOrResumeListener {
                    bind.progress.progressDrawable = ContextCompat.getDrawable(
                        this@DownloadActivity,
                        R.drawable.circular_progress_bar
                    )
                    bind.progress.setProgress(bind.progress.progress + 1, true)
                    bind.pauseResume.text = "PAUSE"
                    Timber.d("download Started")
                }.setOnPauseListener {
                    bind.progress.progressDrawable = ContextCompat.getDrawable(
                        this@DownloadActivity,
                        R.drawable.circular_paused_progress_bar
                    )
                    bind.progress.setProgress(bind.progress.progress - 1, true)
                    Timber.d("download Paused")
                    bind.pauseResume.text = "RESUME"
                    showSnack("Ubuntu download Paused")
                }.setOnCancelListener {
//                    bind.progress.visibility = View.GONE
                    Timber.d("download Cancelled")
                    showSnack("Ubuntu download Canceled")
                    bind.errorMsg.text = "Download Cancelled"
                    bind.progressLayout.visibility = View.GONE
                    bind.errorLayout.visibility = View.VISIBLE
                }.setOnProgressListener {
                    val progress = ((it.currentBytes * 100) / it.totalBytes).toInt()
//                    Timber.d("download $progress%")
                    bind.progress.setProgress(progress, true)
                    bind.progressPercent.text = "$progress%"
                    Timber.d("download $progress%")
                }
            downloadId = downloader.start(object : OnDownloadListener {
                override fun onDownloadComplete() {
                    Timber.d("download complete")
                    checkDownload()
//                    DashboardActivity.navigateToDashboard(this@DownloadActivity)
                }

                override fun onError(error: com.downloader.Error?) {
                    Timber.e("download Error ${error?.connectionException?.message} ${error?.connectionException?.javaClass}")
                    if (error?.connectionException?.javaClass == UnknownHostException::class.java)
                        bind.errorMsg.text = "Server not reachable"
                    else
                        bind.errorMsg.text = error?.connectionException?.message
                    bind.progressLayout.visibility = View.GONE
                    bind.errorLayout.visibility = View.VISIBLE
//                        close("Some error in downloading file")
                }
            })
//            Timber.d("downloadStatusS $id ${downloader.downloadId} ${downloader.status}")
        } catch (err: Exception) {
            Timber.e("download $err ${err.cause}")
            bind.progressLayout.visibility = View.GONE
            bind.errorLayout.visibility = View.VISIBLE
        }
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

    private fun showSnack(msg: String?) {
        runOnUiThread {
            if (msg != null)
                Snackbar.make(bind.root, msg, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}