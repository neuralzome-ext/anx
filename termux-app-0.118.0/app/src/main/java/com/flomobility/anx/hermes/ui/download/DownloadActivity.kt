package com.flomobility.anx.hermes.ui.download

import android.annotation.SuppressLint
import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.downloader.Status.*
import com.flomobility.anx.R
import com.flomobility.anx.app.PluginResultsService
import com.flomobility.anx.app.TermuxCommandExecutor
import com.flomobility.anx.app.TermuxCommandExecutor.ITermuxCommandExecutor
import com.flomobility.anx.app.TermuxInstaller
import com.flomobility.anx.databinding.ActivityUbuntuSetupBinding
import com.flomobility.anx.hermes.other.clear
import com.flomobility.anx.hermes.other.setIsInstalled
import com.flomobility.anx.hermes.other.viewutils.AlertDialog
import com.flomobility.anx.hermes.ui.home.HomeActivity
import com.flomobility.anx.hermes.ui.login.LoginActivity
import com.flomobility.anx.shared.termux.TermuxConstants
import com.flomobility.anx.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_ACTIVITY
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.UnknownHostException
import javax.inject.Inject
import kotlin.system.exitProcess


@SuppressLint("NewApi")
@AndroidEntryPoint
class DownloadActivity : AppCompatActivity() {

    private var binding: ActivityUbuntuSetupBinding? = null
    private val bind get() = binding!!
    private lateinit var viewModel: DownloadViewModel
    private var downloadId = 0

    private val FILE_NAME = "ubuntu-latest.tar.gz"
    private var FILE_PATH = ""
    private val FILE_URl = "https://flo-linux-fs.s3.ap-south-1.amazonaws.com/ubuntu-latest.tar.gz"

    val INSTALL_CMD = """#!/data/data/com.flomobility.anx/files/usr/bin/bash
FS="ubuntu20.04-fs"
echo "Decompressing the ${'$'}{FS}, please wait..."
proot --link2symlink tar --recursive-unlink --preserve-permissions -vzxf <path> --exclude='dev'||:
echo "The ubuntu rootfs have been successfully decompressed!"

START=start.sh
echo "Creating the start script, please wait..."
cat > ${'$'}START <<- EOM
#!/data/data/com.flomobility.anx/files/usr/bin/bash
unset LD_PRELOAD
command="proot"
command+=" --link2symlink"
command+=" -0"
command+=" -r ubuntu-fs"
command+=" -b /dev"
command+=" -b /proc"
command+=" -b /sys"
command+=" -b /:/host-rootfs"
command+=" -b ubuntu-fs/tmp:/dev/shm"
command+=" -b /sdcard"
command+=" -b /storage"
command+=" -b /system"
command+=" -b /vendor"
command+=" -b /mnt"
command+=" -b ubuntu-fs/proc/stat:/proc/stat"
command+=" -b ubuntu-fs/proc/loadavg:/proc/loadavg"
command+=" -b ubuntu-fs/proc/uptime:/proc/uptime"
command+=" -b ubuntu-fs/proc/version:/proc/version"
command+=" -b ubuntu-fs/proc/vmstat:/proc/vmstat"
command+=" -w /root"
command+=" /usr/bin/env -i"
command+=" HOME=/root"
command+=" PATH=/usr/local/sbin:/usr/local/bin:/bin:/usr/bin:/sbin:/usr/sbin:/usr/games:/usr/local/games"
command+=" TERM=\${'$'}TERM"
command+=" LANG=C.UTF-8"
command+=" /bin/bash --login"
com="\${'$'}@"
if [ -z "\${'$'}1" ];then
    exec \${'$'}command
else
    \${'$'}command -c "\${'$'}com"
fi
EOM
echo "The start script has been successfully created!"

echo "Fixing shebang of ${'$'}{START}, please wait..."
termux-fix-shebang ${'$'}bin
echo "Successfully fixed shebang of startubuntu.sh!"

echo "Making startubuntu.sh executable please wait..."
chmod +x ${'$'}START
echo "Successfully made start.sh executable"

echo "done"
"""

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    companion object {
        fun navigateToDownload(context: Context) {
            context.startActivity(
                Intent(context, DownloadActivity::class.java)//,
//                ActivityOptions.makeSceneTransitionAnimation(context as Activity).toBundle()
            )
        }

        private const val INSTALL_FS_EXECUTION_CODE = 10001
    }

    // broadcast receiver
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                val executionCode =
                    intent.getIntExtra(PluginResultsService.RESULT_BROADCAST_EXECUTION_CODE_KEY, -1)
                val result = intent.getBundleExtra(PluginResultsService.RESULT_BROADCAST_RESULT_KEY)
                when (executionCode) {
                    INSTALL_FS_EXECUTION_CODE -> {
                        result?.let {
                            val exitCode =
                                result.getInt(TermuxConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE)
                            if (exitCode == 0) {
                                onInstallSuccess()
                                return
                            }
                            onInstallFailed(exitCode)
                            Timber.e("Installation Failed")
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUbuntuSetupBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this@DownloadActivity)[DownloadViewModel::class.java]
        setContentView(binding?.root)
        FILE_PATH = filesDir.absolutePath
        Timber.d("File path : $FILE_PATH")

        createInstallScript()
        setEventListeners()
        checkDownload()
    }

    private fun createInstallScript() {
        val dirPath = "/data/data/com.flomobility.anx/files/home/install.sh"
        val cmd = INSTALL_CMD.replace("<path>", "$FILE_PATH/$FILE_NAME")
        try {
            val file = File(dirPath)
            file.createNewFile()
            val fo: OutputStream = FileOutputStream(file)
            val bytes: ByteArray = cmd.toByteArray(Charsets.UTF_8)
            fo.write(bytes)
            fo.flush()
            fo.close()
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    private fun setEventListeners() {
        // register broadcast manager
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
                        checkDownload()
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
            }
        }
    }

    private fun checkDownload() {
        if (File("$FILE_PATH/$FILE_NAME").exists()) {
            checkInstalled()
            return
        }
        startDownload()
    }

    private fun checkInstalled() {
        bind.downloading1.text = "INSTALLING FILE SYSTEM"
        bind.progress.visibility = View.INVISIBLE
        bind.progressIndeterminate.visibility = View.VISIBLE
        bind.progressPercent.text = ""
        lifecycleScope.launch {
            // Install here

            TermuxInstaller.setupBootstrapIfNeeded(this@DownloadActivity, Runnable {
                try {
                    val termuxCommandExecutor =
                        TermuxCommandExecutor.getInstance(this@DownloadActivity)
                    termuxCommandExecutor.startTermuxCommandExecutor(object :
                        ITermuxCommandExecutor {
                        override fun onTermuxServiceConnected() {
                            termuxCommandExecutor.executeTermuxCommand(
                                this@DownloadActivity,
                                "bash",
                                arrayOf("install.sh"),
                                INSTALL_FS_EXECUTION_CODE
                            )
                        }

                        override fun onTermuxServiceDisconnected() {}
                    })

                } catch (e: Exception) {
                    Timber.e(e)
                }
            })
        }
    }

    private fun onInstallSuccess() {
        lifecycleScope.launch {
            sharedPreferences.setIsInstalled(true)
            bind.progressIndeterminate.visibility = View.GONE
            bind.downloading1.text = "YOU'RE ALL SET!"
            bind.checkAnim.visibility = View.VISIBLE
            delay(2000)
            HomeActivity.navigateToHome(this@DownloadActivity)
            finish()
        }
    }

    private fun onInstallFailed(code: Int) {
        AlertDialog.getInstance(
            "Installation Failed",
            "Process exited with code $code",
            "Exit",
            yesListener = {
                exitProcess(0)
            }
        ).show(supportFragmentManager, AlertDialog.TAG)
    }

    private fun startDownload() {
        try {
            if (File("$FILE_PATH/$FILE_NAME").exists()) {
                checkInstalled()
                return
            }
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
                    showSnack("File system download Canceled")
                    bind.errorMsg.text = "Download Cancelled"
                    bind.progressLayout.visibility = View.GONE
                    bind.errorLayout.visibility = View.VISIBLE
                }.setOnProgressListener {
                    val progress = ((it.currentBytes * 100) / it.totalBytes).toInt()
//                    Timber.d("download $progress%")
                    bind.progress.setProgress(progress, true)
                    bind.progressPercent.text = "$progress%"
//                    Timber.d("download $progress%")
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

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(PluginResultsService.RESULT_BROADCAST_INTENT))
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
