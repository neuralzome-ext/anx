package com.flomobility.hermes.ui.splash

import android.Manifest
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.flomobility.hermes.MainActivity
import com.flomobility.hermes.databinding.ActivitySplashBinding
import com.flomobility.hermes.other.checkToken
import com.flomobility.hermes.other.getIsInstalled
import com.flomobility.hermes.other.viewutils.AlertDialog
import com.flomobility.hermes.ui.download.DownloadActivity
import com.flomobility.hermes.ui.home.HomeActivity
import com.flomobility.hermes.ui.login.LoginActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
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

    private val FILE_NAME = "sshSetup.zip"
    private var FILE_PATH = ""

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        FILE_PATH = filesDir.absolutePath + "/termux"
        checkPermissions()
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
        createLocationRequest()
        checkConditions()
//        sendCommandToService(Constants.ACTION_START_OR_RESUME_SERVICE, EndlessService::class.java)
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
            requestPermissions()
            checkPermissions()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun requestPermissions() {
        val shouldProvideRationale: Boolean = ActivityCompat.shouldShowRequestPermissionRationale(
            this@SplashActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Snackbar.make(
                bind.root,
                "Your Permission is not enable so please enable",
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction("Ok") { // Request permission
                    ActivityCompat.requestPermissions(
                        this@SplashActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        MainActivity.REQUEST_PERMISSIONS_REQUEST_CODE
                    )
                }
                .show()
        } else {
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(
                this@SplashActivity, arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                MainActivity.REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        this@SplashActivity,
                        MainActivity.REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun checkConditions() {
        lifecycleScope.launch {
            delay(2000)
            when (true) {
                !sharedPreferences.checkToken() -> LoginActivity.navigateToLogin(this@SplashActivity)
                File("$FILE_PATH/$FILE_NAME").exists() && sharedPreferences.getIsInstalled() -> HomeActivity.navigateToHome(
                    this@SplashActivity
                )
                else -> DownloadActivity.navigateToDownload(
                    this@SplashActivity
                )
            }
            finish()
        }
    }
}