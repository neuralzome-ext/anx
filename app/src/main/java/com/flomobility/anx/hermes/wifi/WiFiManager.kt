package com.flomobility.anx.hermes.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Provides implementation of
 * Wifi Connect/Disconnect.
 */
class WiFiManager @Inject constructor(@ApplicationContext var appContext: Context) {

    var wifiManager: WifiManager
    var conManager: ConnectivityManager
    var networkInfo: NetworkInfo?
    var isWifiConnected: Boolean

    companion object {
        const val TAG = "WiFiManager"
    }

    init {
        wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        conManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkInfo = conManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        isWifiConnected = networkInfo != null && networkInfo!!.isConnected
    }

    /**
     * method call on connect to wifi
     */
    fun connectToWifi(networkSSID: String?, networkPassword: String?) {
        if (networkSSID != null && networkPassword != null) {
            connect(networkSSID, networkPassword)
        } else if (networkSSID == null && networkPassword == null) {
            Log.d(TAG, "Enter Wifi credentials")
        } else {
            Log.d(TAG, "Enter Wifi credentials")
        }
    }

    private fun connect(networkSSID: String, networkPassword: String) {
        try {
            if (!wifiManager.isWifiEnabled) {
                wifiManager.isWifiEnabled = true
            }
            if (wifiManager.isWifiEnabled) {
                Log.d(TAG, "Connecting..")
            }
            removeAllNetworks()
            val conf = WifiConfiguration()
            conf.SSID = String.format("\"%s\"", networkSSID)
            conf.preSharedKey = String.format("\"%s\"", networkPassword)
            val netId = wifiManager.addNetwork(conf)
            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()
        } catch (e: Exception) {
            Log.d(TAG, "Exception" + e)
        }

    }

    /*
    method call on disconnect
     */
    fun disconnectWifi() {
        try {
            if (!wifiManager.isWifiEnabled) {
                Log.d(TAG, "Wifi not enabled")
            }
            val networkId = wifiManager.connectionInfo.networkId
            wifiManager.removeNetwork(networkId)
            removeAllNetworks()
            wifiManager.disconnect()
            wifiManager.isWifiEnabled = false
            Log.d(TAG, "Wifi Disconnected")
        } catch (e: Exception) {
            Log.d(TAG, "Exception" + e)
        }
    }

    @SuppressLint("MissingPermission")
    fun removeAllNetworks() {
        val list = wifiManager.configuredNetworks
        for (i in list) {
            wifiManager.removeNetwork(i.networkId)
            wifiManager.saveConfiguration();
        }
    }
}
