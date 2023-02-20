package com.flomobility.anx.hermes.ui.download

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.downloader.PRDownloader
//import com.flomobility.anx.app.TerminalInstaller
import com.flomobility.anx.databinding.ActivityUbuntuSetupBinding
import com.flomobility.anx.hermes.daemon.InstallingService
import com.flomobility.anx.hermes.other.*
import com.flomobility.anx.hermes.other.viewutils.AlertDialog
import com.flomobility.anx.hermes.ui.home.HomeActivity
import com.flomobility.anx.hermes.ui.login.LoginActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject
import kotlin.system.exitProcess


@SuppressLint("NewApi")
@AndroidEntryPoint
class DownloadActivity : AppCompatActivity() {

    private var binding: ActivityUbuntuSetupBinding? = null
    private val bind get() = binding!!

    private var isInstalling = false

    private val FILE_NAME = "ubuntu-latest.tar.gz"
    private var FILE_PATH = ""

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

echo "Making startubuntu.sh executable please wait..."
chmod +x ${'$'}START
echo "Successfully made start.sh executable"

echo "done"
"""

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var downloadManager: DownloadManager

    companion object {
        fun navigateToDownload(context: Context) {
            context.startActivity(
                Intent(context, DownloadActivity::class.java)//,
//                ActivityOptions.makeSceneTransitionAnimation(context as Activity).toBundle()
            )
        }

        const val INSTALL_FS_EXECUTION_CODE = 10001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUbuntuSetupBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        if(sharedPreferences.getIsInstalled()) {
            onInstallSuccess()
            return
        }
        FILE_PATH = filesDir.absolutePath
        createInstallScript()
        setEventListeners()
        handleEvent(InstallingService.state)
        subscribeToObservers()
        sendCommandToService(
            Constants.ACTION_START_OR_RESUME_SERVICE,
            InstallingService::class.java
        )
    }

    private fun subscribeToObservers() {
        lifecycleScope.launch(Dispatchers.Main) {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                InstallingService.eventsFlow.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }

    private fun handleEvent(event: InstallingService.Events) {
        when (event) {
            InstallingService.Events.CreateInstallScript -> {
                createInstallScript()
            }
            InstallingService.Events.Downloading -> {
                Timber.d("Downloading file system.")
            }
            InstallingService.Events.DownloadComplete -> {
                Timber.d("Download of filesystem done. Proceeding to Install")
            }
            is InstallingService.Events.DownloadFailed -> {
                val error = event.error
                bind.errorMsg.text = error
                bind.progressLayout.visibility = View.GONE
                bind.errorLayout.visibility = View.VISIBLE
            }
            is InstallingService.Events.DownloadProgress -> {
                val progress = event.progress
                Timber.d("Progress : $progress")
                bind.progress.setProgress(progress, true)
                bind.progressPercent.text = "$progress%"
            }
            InstallingService.Events.Installing -> {
                bind.tvInfoText.text = "INSTALLING FILE SYSTEM"
                bind.progress.visibility = View.INVISIBLE
                bind.progressIndeterminate.visibility = View.VISIBLE
                bind.progressPercent.text = ""
            }
            is InstallingService.Events.InstallingFailed -> {
                onInstallFailed(event.code)
                sendCommandToService(Constants.ACTION_STOP_SERVICE, InstallingService::class.java)
            }
            InstallingService.Events.InstallingSuccess -> {
                onInstallSuccess()
            }
            InstallingService.Events.NotStarted -> Unit

        }
    }

    private fun createInstallScript() {
        /*TerminalInstaller.setupBootstrapIfNeeded(this@DownloadActivity, Runnable {
            try {
                val dirPath = "/data/data/com.flomobility.anx/files/home/"
                val installScriptFile = "install.sh"
                val cmd = INSTALL_CMD.replace("<path>", "$FILE_PATH/$FILE_NAME")
                try {
                    if (!File(dirPath).exists()) {
                        File(dirPath).mkdirs()
                    }
                    val file = File("$dirPath/$installScriptFile")
                    if (!file.exists())
                        file.createNewFile()

                    val fo: OutputStream = FileOutputStream(file)
                    val bytes: ByteArray = cmd.toByteArray(Charsets.UTF_8)
                    fo.write(bytes)
                    fo.flush()
                    fo.close()
                } catch (e: IOException) {
                    Timber.e(e)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        })*/
    }

    private fun setEventListeners() {
        // register broadcast manager
        bind.apply {
            retry.setOnClickListener {
                errorLayout.visibility = View.GONE
                progressLayout.visibility = View.VISIBLE
                // Retry download
                downloadManager.retry()
            }
            exit.setOnClickListener {
                if(!isHeadLessBuildType()) {
                    AlertDialog.getInstance(
                        "Confirm Logout",
                        "Logging out will delete the current download progress",
                        "Logout",
                        "Cancel",
                        yesListener = {
                            sendCommandToService(
                                Constants.ACTION_STOP_SERVICE,
                                InstallingService::class.java
                            )
                            sharedPreferences.clear()
                            LoginActivity.navigateToLogin(this@DownloadActivity)
                            finish()
                        }
                    ).show(supportFragmentManager, AlertDialog.TAG)
                }
            }
        }
    }

    private fun sendCommandToService(action: String, serviceClass: Class<*>) {
        handleExceptions {
            Intent(this, serviceClass).also {
                it.action = action
                startService(it)
            }
        }
    }

    private fun onInstallSuccess() {
        lifecycleScope.launch {
            sharedPreferences.setIsInstalled(true)
            bind.progress.visibility = View.INVISIBLE
            bind.progressIndeterminate.visibility = View.VISIBLE
            bind.progressPercent.text = ""
            bind.tvInfoText2.isVisible = false
            bind.progressIndeterminate.visibility = View.GONE
            bind.tvInfoText.text = "YOU'RE ALL SET!"
            bind.checkAnim.visibility = View.VISIBLE
            delay(2000)
            sendCommandToService(Constants.ACTION_STOP_SERVICE, InstallingService::class.java)
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

}
