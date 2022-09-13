package com.flomobility.hermes.comms.handlers

import android.os.Build
import androidx.annotation.RequiresApi
import com.flomobility.hermes.api.GetIdentityResponse
import com.flomobility.hermes.comms.SocketManager.Companion.GET_IDENTITY_SOCKET_ADDR
import com.flomobility.hermes.other.Constants
import com.flomobility.hermes.phone.PhoneManager
import com.google.gson.Gson
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@RequiresApi(Build.VERSION_CODES.O)
class GetIdentityHandler @Inject constructor(
    private val phoneManager: PhoneManager,
    private val gson: Gson
) : Runnable {

    lateinit var socket: ZMQ.Socket

    override fun run() {
        try {
            ZContext().use { ctx ->
                socket = ctx.createSocket(SocketType.REP)
                socket.bind(GET_IDENTITY_SOCKET_ADDR)
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        socket.recv(0)?.let { bytes ->
                            val data = String(bytes, ZMQ.CHARSET)
                            Timber.d("[Get Identity] -- Requested : $data")
                            if (JSONObject(data).length() == 0) {
                                // valid request
                                val result = phoneManager.getIdentity()
                                val imeiResponse = GetIdentityResponse(imei = result)
                                socket.send(gson.toJson(imeiResponse).toByteArray(ZMQ.CHARSET), 0)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e("Error in GetIdentityHandler : $e")
                        socket.send((e.message ?: Constants.UNKNOWN_ERROR_MSG).toByteArray(ZMQ.CHARSET), 0)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("Error in GetIdentityHandler : $e")
            socket.send((e.message ?: Constants.UNKNOWN_ERROR_MSG).toByteArray(ZMQ.CHARSET), 0)
        }
    }
}