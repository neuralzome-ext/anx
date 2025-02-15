package com.flomobility.anx.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import com.flomobility.anx.common.Result
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

    companion object {
        const val TAG = "WiFiManager"
    }

    /**
     * method call on connect to wifi
     */
    fun connectToWifi(networkSSID: String?, networkPassword: String?): Result {
        if (networkSSID.isNullOrEmpty() || networkPassword.isNullOrEmpty()) {
            return Result(
                success = false,
                message = "Empty values are not allowed"
            )
        }
        return connect(networkSSID, networkPassword)
    }

    private fun connect(networkSSID: String, networkPassword: String): Result {
        try {
            if (!wifiManager.isWifiEnabled) {
                wifiManager.isWifiEnabled = true
            }
            if (wifiManager.isWifiEnabled) {
                Timber.tag(TAG).d(".........")
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
            Timber.tag(TAG).d("Error connecting to $networkSSID : ${e.message}")
            return Result(false, e.message.toString())
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
