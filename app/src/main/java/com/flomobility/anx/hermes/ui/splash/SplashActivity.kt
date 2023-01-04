package com.flomobility.anx.hermes.ui.splash

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.flomobility.anx.databinding.ActivitySplashBinding
import com.flomobility.anx.hermes.other.*
import com.flomobility.anx.hermes.other.viewutils.AlertDialog
import com.flomobility.anx.hermes.phone.Device
import com.flomobility.anx.hermes.phone.PhoneManager
import com.flomobility.anx.hermes.ui.download.DownloadActivity
import com.flomobility.anx.hermes.ui.home.HomeActivity
import com.flomobility.anx.hermes.ui.license.LicenseActivity
import com.flomobility.anx.hermes.ui.login.LoginActivity
import com.google.android.gms.location.LocationRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
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

    @Inject
    lateinit var phoneManager: PhoneManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        FILE_PATH = filesDir.absolutePath
        device.checkIsRooted()
    }

    private fun checkPermissions(): Boolean {
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
            return false
        }
        Timber.d("All permissions granted")
        return true
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
                            // open settings to grant permissions
                            startActivity(Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", packageName, null)
                            })
                        },
                        noListener = {
                            finishAffinity()
                        }
                    ).show(supportFragmentManager, AlertDialog.TAG)
                    return
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun requestPermission() {
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
    }

    override fun onStart() {
        super.onStart()
        if (!checkPermissions()) {
            requestPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkPermissions()) {
            checkConditions()
        }
    }

    private fun checkConditions() {
        lifecycleScope.launch {
            delay(2000)
            /**
             * If Headless build than move to Download and then home screen
             */
            if (isHeadLessBuildType()) {
                setupHeadless()
                when {
                    sharedPreferences.getIsInstalled() -> {
                        HomeActivity.navigateToHome(
                            this@SplashActivity
                        )
                    }
                    else -> DownloadActivity.navigateToDownload(
                        this@SplashActivity
                    )
                }
            } else {
                when {
                    !sharedPreferences.getAcceptLicense() -> {
                        LicenseActivity.navigateToLicense(this@SplashActivity)
                    }
                    !sharedPreferences.checkToken() -> LoginActivity.navigateToLogin(this@SplashActivity)
                    /*File("$FILE_PATH/${Constants.FILES_SYSTEM_FILE_NAME}").exists() && */
                    sharedPreferences.getIsInstalled() -> {
                        HomeActivity.navigateToHome(
                            this@SplashActivity
                        )
                    }
                    else -> DownloadActivity.navigateToDownload(
                        this@SplashActivity
                    )
                }
            }
            finish()
        }
    }

    private fun setupHeadless() {
        sharedPreferences.putDeviceID(phoneManager.getIdentity())
        sharedPreferences.setIsOnBoot(true)
    }

}
