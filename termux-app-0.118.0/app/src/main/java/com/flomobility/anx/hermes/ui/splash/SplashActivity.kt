package com.flomobility.anx.hermes.ui.splash

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.flomobility.anx.databinding.ActivitySplashBinding
import com.flomobility.anx.hermes.other.Constants
import com.flomobility.anx.hermes.other.checkToken
import com.flomobility.anx.hermes.other.clear
import com.flomobility.anx.hermes.other.getIsInstalled
import com.flomobility.anx.hermes.other.viewutils.AlertDialog
import com.flomobility.anx.hermes.phone.Device
import com.flomobility.anx.hermes.ui.download.DownloadActivity
import com.flomobility.anx.hermes.ui.home.HomeActivity
import com.flomobility.anx.hermes.ui.login.LoginActivity
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private var binding: ActivitySplashBinding? = null
    private val bind get() = binding!!
    private lateinit var locationRequest: LocationRequest

    private var FILE_PATH = ""

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var device: Device

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
//        with(window) {
//            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
//
//            // set an exit transition
//            enterTransition = Slide(Gravity.START)
//            exitTransition = Slide(Gravity.END)
//        }
        setContentView(binding?.root)
        FILE_PATH = filesDir.absolutePath
        checkPermissions()
        device.checkIsRooted()
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_NUMBERS
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.d("All permissions not granted")
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_PHONE_NUMBERS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 1234
            )
            return
        }
        Timber.d("All permissions granted")
        checkConditions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1234) {
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    AlertDialog.getInstance(
                        "Permission Required",
                        "Allow all requested permissions to continue",
                        "Allow",
                        "Exit",
                        yesListener = {
                            checkPermissions()
                        },
                        noListener = {
                            finishAffinity()
                        }
                    ).show(supportFragmentManager, AlertDialog.TAG)
/*
                    Snackbar.make(
                        this@SplashActivity,
                        bind.root,
                        "Grant all permissions and restart app",
                        Snackbar.LENGTH_SHORT
                    ).show()
*/
                    return
                }
            }
            checkPermissions()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun checkConditions() {
        lifecycleScope.launch {
            delay(2000)
            when (true) {
                !sharedPreferences.checkToken() -> LoginActivity.navigateToLogin(this@SplashActivity)
                File("$FILE_PATH/${Constants.FILES_SYSTEM_FILE_NAME}").exists() && sharedPreferences.getIsInstalled() -> {
                    HomeActivity.navigateToHome(
                        this@SplashActivity
                    )
                }
                else -> DownloadActivity.navigateToDownload(
                    this@SplashActivity
                )
            }
            finish()
        }
    }
}
