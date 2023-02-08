package com.flomobility.anx.hermes.comms.handlers

import android.os.Build
import androidx.annotation.RequiresApi
import com.flomobility.anx.hermes.api.ConnectWifiRequest
import com.flomobility.anx.hermes.api.StandardResponse
import com.flomobility.anx.hermes.comms.SocketManager
import com.flomobility.anx.hermes.comms.SocketManager.Companion.CONNECT_WIFI_SOCKET_ADDR
import com.flomobility.anx.hermes.other.Constants
import com.flomobility.anx.hermes.wifi.WiFiManager
import com.google.gson.Gson
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@RequiresApi(Build.VERSION_CODES.O)
class ConnectWifiHandler @Inject constructor(
    private val wiFiManager: WiFiManager,
    private val gson: Gson
): Runnable {

    lateinit var socket: ZMQ.Socket

    val interrupt = AtomicBoolean(false)

    override fun run() {
        try {
            ZContext().use { ctx ->
                interrupt.set(false)
                socket = ctx.createSocket(SocketType.REP)
                socket.bind(CONNECT_WIFI_SOCKET_ADDR)
                Timber.i("Connect Wifi handler running on ${SocketManager.CONNECT_WIFI_SOCKET_ADDR}")

                val poller = ctx.createPoller(1)
                poller.register(socket)

                while (!interrupt.get()) {
                    try {
                        poller.poll(100)
                        if (poller.pollin(0)) {
                            socket.recv(0)?.let { bytes ->
                                val data = String(bytes, ZMQ.CHARSET)
                                val req = gson.fromJson<ConnectWifiRequest>(
                                    data,
                                    ConnectWifiRequest.type
                                )
                                Timber.d("Connecting to Wifi ${req.ssid}")

                                val result = wiFiManager.connectToWifi(req.ssid, req.password)
                                val response = StandardResponse(
                                    success = result.success,
                                    message = result.message
                                )
                                socket.send(gson.toJson(response).toByteArray(ZMQ.CHARSET), 0)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e("Error in Connect Wifi Handler : $e")
                        val response = StandardResponse(success = false, message = (e.message ?: Constants.UNKNOWN_ERROR_MSG))
                        socket.send(gson.toJson(response).toByteArray(ZMQ.CHARSET), 0)
                    }
                }
            }
            Timber.i("Closing Connect Wifi handler running on $CONNECT_WIFI_SOCKET_ADDR")
        } catch (e: Exception) {
            Timber.e("Error in Connect Wifi Handler : $e")
        }
    }
}
