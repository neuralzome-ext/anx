package com.flomobility.hermes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.flomobility.hermes.daemon.EndlessService
import com.flomobility.hermes.databinding.ActivityMainBinding
import com.flomobility.hermes.other.Constants
import com.flomobility.hermes.other.getIPAddressList
import com.flomobility.hermes.phone.Device
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
        sendCommandToService(Constants.ACTION_START_OR_RESUME_SERVICE, EndlessService::class.java)
        /*requestPermissionLauncher.launch(
               arrayOf(
                   Manifest.permission.READ_SMS,
                   Manifest.permission.READ_PHONE_NUMBERS,
                   Manifest.permission.READ_PHONE_STATE,

                   )
           )*/

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
}