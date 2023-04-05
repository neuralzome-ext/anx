package com.flomobility.anx.hotspot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.HandlerThread
import com.flomobility.anx.common.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HotspotManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.tag(TAG).i("intent received : $intent, extras : ${intent?.getIntExtra(
                WifiManager.EXTRA_WIFI_STATE,
                WifiManager.WIFI_STATE_UNKNOWN
            )}")
            intent?.let {
                if (it.action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                    val state = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE,
                        WifiManager.WIFI_STATE_UNKNOWN
                    )
                    when (state) {
                        WifiManager.WIFI_STATE_DISABLED -> {
                            if(this@HotspotManager.state == State.IDLE) return
                            this@HotspotManager.state = State.HOTSPOT_OFF
                        }
                        WifiManager.WIFI_STATE_ENABLED -> {
                            if(this@HotspotManager.state == State.IDLE) return
                            this@HotspotManager.state = State.DONE
                        }
                        WifiManager.WIFI_STATE_UNKNOWN -> {
                            if(this@HotspotManager.state == State.IDLE) return
                            this@HotspotManager.state = State.ERROR
                        }
                    }
                }
            }
        }
    }


    fun init() {
        /*handlerThread.start()
        handler = Handler(handlerThread.looper)
        context.registerReceiver(
            receiver,
            IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION),
            null,
            handler
        )*/
    }

    var state = State.IDLE
        private set

    fun setHotspot(ssid: String, password: String): Result {
        val intent = Intent().apply {
            action = ACTION_UPDATE_HOTSPOT
            putExtra(KEY_SSID, ssid)
            putExtra(KEY_PASSWORD, password)
        }
        state = State.START
        context.sendBroadcast(intent)

        // blocking call
//        val result = waitForUpdate()
//        state = State.IDLE
        return Result(success = true)
    }

    private fun waitForUpdate(): Result {
        while(true) {
            when(state) {
                State.IDLE -> Unit
                State.START -> Unit
                State.HOTSPOT_OFF -> {
                    Timber.tag(TAG).i("Hotspot turned off.")
                }
                State.WAITING -> continue
                State.DONE -> return Result(success = true)
                State.ERROR -> return Result(success = false)
            }
        }
    }

    enum class State {
        IDLE, START, HOTSPOT_OFF, WAITING, DONE, ERROR
    }

    companion object {
        private const val ACTION_UPDATE_HOTSPOT = "com.android.settings.wifi.tether.UpdateHotspot"

        private const val KEY_SSID = "ssid"
        private const val KEY_PASSWORD = "password"

        private const val TAG = "HotspotManager"
    }

}
