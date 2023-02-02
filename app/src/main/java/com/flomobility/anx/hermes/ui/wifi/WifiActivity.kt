package com.flomobility.anx.hermes.ui.wifi

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.flomobility.anx.databinding.ActivityHomeBinding
import com.flomobility.anx.databinding.ActivityWifiBinding
import com.flomobility.anx.hermes.daemon.WiFiManager

class WifiActivity : AppCompatActivity(), View.OnClickListener {

    private var binding: ActivityWifiBinding? = null
    private val bind get() = binding!!
    var networkSSID: String? = null
    var networkPassword: String? = null
    var wiFiManager: WiFiManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        if (wiFiManager == null) {
            wiFiManager = WiFiManager(applicationContext)
        }

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
