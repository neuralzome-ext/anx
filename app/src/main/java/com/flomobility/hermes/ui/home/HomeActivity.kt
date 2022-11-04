package com.flomobility.hermes.ui.home

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.downloader.PRDownloader
import com.flomobility.flobtops.adapters.IpAdapter
import com.flomobility.hermes.R
import com.flomobility.hermes.adapter.SensorAdapter
import com.flomobility.hermes.databinding.ActivityDashboardBinding
import com.flomobility.hermes.model.SensorModel
import com.flomobility.hermes.model.SensorStatusModel
import com.flomobility.hermes.network.requests.InfoRequest
import com.flomobility.hermes.other.*
import com.flomobility.hermes.other.viewutils.AlertDialog
import com.flomobility.hermes.ui.login.LoginActivity
import com.flomobility.hermes.ui.settings.SettingsActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    companion object {
        fun navigateToDashboard(context: Context) {
            context.startActivity(Intent(context, HomeActivity::class.java))
        }
    }

    private var binding: ActivityDashboardBinding? = null
    private val bind get() = binding!!
    private lateinit var viewModel: HomeViewModel

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this@HomeActivity)[HomeViewModel::class.java]
        setContentView(binding?.root)
        if (sharedPreferences.getDeviceID() == null) {
            showSnack("Login Again")
            logout()
            return
        }
        bind.deviceId.text = "DEVICE ID: ${sharedPreferences.getDeviceID()}"
        setupRecyclers()
        subscribeToObservers()
        setEventListeners()
        viewModel.sendInfoRequest(InfoRequest(sharedPreferences.getDeviceID()!!))
    }

    private fun setEventListeners() {
        bind.apply {
            exit.setOnClickListener {
                AlertDialog.getInstance(
                    "Confirm Logout",
                    "",
                    "Logout",
                    "Cancel",
                    yesListener = {
                        PRDownloader.cancelAll()
                        sharedPreferences.clear()
                        LoginActivity.navigateToLogin(this@HomeActivity)
                        finish()
                    }
                ).show(supportFragmentManager, AlertDialog.TAG)
            }
            settings.setOnClickListener {
                SettingsActivity.navigateToSetting(this@HomeActivity)
                finish()
            }
        }
    }

    private fun subscribeToObservers() {
        viewModel.info.observe(this@HomeActivity) {
            when (it.getContentIfNotHandled()) {
                is Resource.Loading -> {

                }
                is Resource.Success -> {
                    if (it.peekContent().data?.access == false || isExpired(it.peekContent().data?.expiry)) {
                        showSnack("Your access has been revoked")
                        logout()
                        return@observe
                    }
                    sharedPreferences.putDeviceExpiry(it.peekContent().data?.expiry)

                }
                is Resource.Error -> {
                    when (it.peekContent().errorData?.code) {
                        null -> {
                            var error = it.peekContent().message
                            if (error?.contains("Failed to connect to") == true)
                                error = "Failed to connect to server"
                            showSnack(error)
                            return@observe
                        }
                        400 -> {
                            showSnack("Server Unreachable!! (400)")
                        }
                        401 -> {
                            showSnack("Login Again")
                            logout()
                        }
                        else -> {
                            val error = it.peekContent().errorData?.message
                            showSnack(error)
                        }
                    }
                }
                null -> TODO()
            }
        }
    }

    private fun logout() {
        sharedPreferences.clear()
        LoginActivity.navigateToLogin(this@HomeActivity)
        finish()
    }

    private fun setupRecyclers() {
        bind.ipRecycler.layoutManager = LinearLayoutManager(this@HomeActivity)
        bind.ipRecycler.adapter = IpAdapter(this@HomeActivity, getIPAddressList(true))
        bind.sensorRecycler.layoutManager = GridLayoutManager(this@HomeActivity, 4)
        bind.sensorRecycler.adapter = SensorAdapter(
            this@HomeActivity,
            arrayListOf(
                SensorModel(
                    R.drawable.ic_imu, "IMU", arrayListOf(
                        SensorStatusModel(true)
                    )
                ),
                SensorModel(
                    R.drawable.ic_gps, "GNSS", arrayListOf(
                        SensorStatusModel(true)
                    )
                ),
                SensorModel(
                    R.drawable.ic_video, "CAMERA", arrayListOf(
                        SensorStatusModel(true),
                        SensorStatusModel(true),
                        SensorStatusModel(true)
                    )
                ),
                SensorModel(R.drawable.ic_mic, "MIC", isAvailable = false),
                SensorModel(
                    R.drawable.ic_usb_serial, "USB SERIAL", arrayListOf(
                        SensorStatusModel(true),
                        SensorStatusModel(true),
                        SensorStatusModel(true)
                    )
                ),
                SensorModel(
                    R.drawable.ic_speaker, "SPEAKER", arrayListOf(
                        SensorStatusModel(true)
                    )
                ),
                SensorModel(R.drawable.ic_bluetooth, "CLASSIC BT", isAvailable = false),
                SensorModel(R.drawable.ic_bluetooth, "BLE", isAvailable = false),
                SensorModel(
                    R.drawable.ic_phone, "PHONE", arrayListOf(
                        SensorStatusModel(true)
                    )
                )
            )
        )
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
