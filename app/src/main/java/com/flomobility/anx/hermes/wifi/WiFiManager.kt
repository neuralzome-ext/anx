package com.flomobility.anx.hermes.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.util.Log
import com.flomobility.anx.hermes.common.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides implementation of
 * Wifi Connect/Disconnect.
 */
@Singleton
class WiFiManager @Inject constructor(
    @ApplicationContext var appContext: Context
) {

    private var wifiManager: WifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var conManager: ConnectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkInfo: NetworkInfo? = conManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
    private var isWifiConnected: Boolean = networkInfo != null && networkInfo!!.isConnected

    companion object {
        const val TAG = "WiFiManager"
    }

    /**
     * method call on connect to wifi
     */
    fun connectToWifi(networkSSID: String?, networkPassword: String?): Result {
        if (networkSSID.isNullOrEmpty() || networkPassword.isNullOrEmpty()) {
            return Result(false, message = "Empty values are not allowed")
        }
        return connect(networkSSID, networkPassword)
    }

    private fun connect(networkSSID: String, networkPassword: String): Result {
        try {
            if (!wifiManager.isWifiEnabled) {
                wifiManager.isWifiEnabled = true
            }
            if (wifiManager.isWifiEnabled) {
                Timber.d(".........")
            }
            removeAllNetworks()
            val conf = WifiConfiguration()
            conf.SSID = String.format("\"%s\"", networkSSID)
            conf.preSharedKey = String.format("\"%s\"", networkPassword)
            val netId = wifiManager.addNetwork(conf)
            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            wifiManager.reconnect()
            return Result(true)
        } catch (e: Exception) {
            Timber.d("Error connecting to $networkSSID : ${e.message}")
            return Result(false, e.message.toString())
        }

    }

    /*
    method call on disconnect
     */
    fun disconnectWifi() {
        try {
            if (!wifiManager.isWifiEnabled) {
                Timber.d("Wifi not enabled")
            }
            val networkId = wifiManager.connectionInfo.networkId
            wifiManager.removeNetwork(networkId)
//            removeAllNetworks()
            wifiManager.disconnect()
            wifiManager.isWifiEnabled = false
            Timber.d("Wifi Disconnected")
        } catch (e: Exception) {
            Timber.d("Error in disconnecting ${e.message}" )
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
