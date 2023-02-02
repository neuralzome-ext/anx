package com.flomobility.anx.hermes.daemon

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.widget.Toast

class WiFiManager internal constructor(var appContext: Context) {
    private val wifiManager: WifiManager
    var conManager: ConnectivityManager
    var networkInfo: NetworkInfo?
    var isWifiConnected: Boolean

    init {
        wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        conManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkInfo = conManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        isWifiConnected = networkInfo != null && networkInfo!!.isConnected
    }

    //method called on connect to wifi
    fun connectToWifi(networkSSID: String?, networkPassword: String?) {
        if (networkSSID != null && networkPassword != null) {
            connect(networkSSID, networkPassword)
        } else if (networkSSID == null && networkPassword == null) {
            Toast.makeText(appContext, "Enter Wifi credentials", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(appContext, "Enter Wifi credentials", Toast.LENGTH_LONG).show()
        }
    }

    private fun connect(networkSSID: String, networkPassword: String) {
        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
        }
        if (wifiManager.isWifiEnabled) {
            Toast.makeText(appContext, "Connecting..", Toast.LENGTH_SHORT).show()
        }
        val conf = WifiConfiguration()
        conf.SSID = String.format("\"%s\"", networkSSID)
        conf.preSharedKey = String.format("\"%s\"", networkPassword)
        val netId = wifiManager.addNetwork(conf)
        wifiManager.disconnect()
        wifiManager.enableNetwork(netId, true)
        wifiManager.reconnect()
    }

    //method called on disconnect
    fun disconnectWifi() {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(appContext, "Wifi not enabled", Toast.LENGTH_LONG).show()
        }
        val networkId = wifiManager.connectionInfo.networkId
        wifiManager.removeNetwork(networkId)
        wifiManager.saveConfiguration()
        wifiManager.disconnect()
        wifiManager.isWifiEnabled = false
        Toast.makeText(appContext, "Wifi Disconnected", Toast.LENGTH_LONG).show()
    }
}
