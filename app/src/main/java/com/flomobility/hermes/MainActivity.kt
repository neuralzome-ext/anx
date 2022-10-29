package com.flomobility.hermes

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.flomobility.hermes.daemon.EndlessService
import com.flomobility.hermes.databinding.ActivityMainBinding
import com.flomobility.hermes.other.Constants
import com.flomobility.hermes.other.getIPAddressList
import com.flomobility.hermes.phone.Device
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationRequest: LocationRequest

    @Inject
    lateinit var device: Device

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.sampleText.text = "IP Addrs : ${getIPAddressList(true)}"
        setOnEventListeners()
        device.checkIsRooted()
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
        sendCommandToService(Constants.ACTION_START_OR_RESUME_SERVICE, EndlessService::class.java)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1234) {
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    Snackbar.make(
                        this@MainActivity,
                        binding.rootLyt,
                        "Grant all permissions and restart app",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return
                }
            }
            requestPermissions()
            checkPermissions()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun setOnEventListeners() {
        binding.rootLyt.setOnLongClickListener {
            // TODO : toggle debug mode
            return@setOnLongClickListener true
        }
    }

    /**
     * A native method that is implemented by the 'hermes' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        const val REQUEST_PERMISSIONS_REQUEST_CODE = 1001
        const val REQUEST_CHECK_SETTINGS = 1002

        private const val TAG = "MainActivity"
        // Used to load the 'hermes' library on application startup.
        init {
            System.loadLibrary("hermes")
        }
    }

    /**
     * Sends the action string to the specified service
     * @param action The action string which the service will refer to, to execute a set of tasks
     * @param serviceClass The ServiceClass to which the action string is to be sent
     * @see Constants.ACTION_START_OR_RESUME_SERVICE
     * @see Constants.ACTION_PAUSE_SERVICE
     * @see Constants.ACTION_STOP_SERVICE
     * */
    private fun sendCommandToService(action: String, serviceClass: Class<*>) {
        Intent(this, serviceClass).also {
            it.action = action
            startService(it)
        }
    }

    private fun requestPermissions() {
        val shouldProvideRationale: Boolean = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            ACCESS_FINE_LOCATION
        )
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Snackbar.make(
                binding.root,
               "Your Permission is not enable so please enable",
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction("Ok", object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        // Request permission
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf<String>(ACCESS_FINE_LOCATION),
                            REQUEST_PERMISSIONS_REQUEST_CODE
                        )
                    }
                })
                .show()
        } else {
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(
                this@MainActivity, arrayOf<String>(ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(this@MainActivity,
                        REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            REQUEST_CHECK_SETTINGS ->{
                when(resultCode){
                    Activity.RESULT_OK->{

                    }
                }
            }
        }
    }
}
