package com.flomobility.anx.hermes.ui.wifi

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.flomobility.anx.databinding.ActivityWifiBinding
import com.flomobility.anx.hermes.wifi.WiFiManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WifiActivity : AppCompatActivity(), View.OnClickListener {

    private var binding: ActivityWifiBinding? = null
    private val bind get() = binding!!
    var networkSSID: String? = null
    var networkPassword: String? = null

    @Inject
    lateinit var wiFiManager: WiFiManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        onClickEvents()
    }


    private fun onClickEvents() {
        bind.connectWifiButton.setOnClickListener(this)
        bind.disoconnectButton.setOnClickListener(this)
    }


    override fun onClick(view: View) {
        if (view.id == bind?.connectWifiButton?.id) {
            networkSSID = bind.editTextSSID.text.toString()
            networkPassword = bind.editTextPassword.text.toString()
            wiFiManager!!.connectToWifi(networkSSID, networkPassword)
        }
        if (view.id == bind?.disoconnectButton?.id) {
            wiFiManager!!.disconnectWifi()
        }
    }
}
