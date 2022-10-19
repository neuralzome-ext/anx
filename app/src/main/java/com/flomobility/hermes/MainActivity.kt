package com.flomobility.hermes

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.content.Intent
import android.annotation.SuppressLint
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.flomobility.hermes.daemon.EndlessService
import com.flomobility.hermes.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import com.flomobility.hermes.other.Constants
import com.flomobility.hermes.other.getIPAddressList
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.tasks.Task


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationRequest: LocationRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.sampleText.text = "IP Addrs : ${getIPAddressList(true)}"

        setOnEventListeners()

        sendCommandToService(Constants.ACTION_START_OR_RESUME_SERVICE, EndlessService::class.java)

        if (!checkPermissions()) {
            requestPermissions()
        } else {
            createLocationRequest()
        }
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

    /**
     * Returns the current state of the permissions needed.
     */
    private fun checkPermissions(): Boolean {
        return PackageManager.PERMISSION_GRANTED === ActivityCompat.checkSelfPermission(
            this,
            ACCESS_FINE_LOCATION
        )
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isEmpty()) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                createLocationRequest()
            } else {
                // Permission denied.
                Snackbar.make(
                    binding.root,
                    "You have permission denied so please allow",
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction("Settings") { // Build intent that displays the App settings screen.
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri: Uri = Uri.fromParts(
                            "package",
                            BuildConfig.APPLICATION_ID, null
                        )
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }
                    .show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
