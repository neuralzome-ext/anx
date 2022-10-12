package com.flomobility.hermes.comms.handlers

import android.os.Build
import androidx.annotation.RequiresApi
import com.flomobility.hermes.api.GetIdentityResponse
import com.flomobility.hermes.comms.SessionManager
import com.flomobility.hermes.comms.SocketManager
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
    private val sessionManager: SessionManager,
    private val gson: Gson
) : Runnable {

    lateinit var socket: ZMQ.Socket

    override fun run() {
        try {
            ZContext().use { ctx ->
                socket = ctx.createSocket(SocketType.REP)
                socket.bind(GET_IDENTITY_SOCKET_ADDR)
                Timber.i("Get Identity handler running on ${SocketManager.GET_IDENTITY_SOCKET_ADDR}")
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        socket.recv(0)?.let { bytes ->
                            val data = String(bytes, ZMQ.CHARSET)
                            if (!sessionManager.connected) {
                                throw IllegalStateException("Cannot start asset without being subscribed!")
                            }
                            Timber.d("[Get Identity] -- Requested : $data")
                            if (JSONObject(data).length() == 0) {
                                // valid request
                                val result = phoneManager.getIdentity()
                                val imeiResponse =
                                    GetIdentityResponse(imei = result, success = true)
                                socket.send(gson.toJson(imeiResponse).toByteArray(ZMQ.CHARSET), 0)
                            }
                        }
                    } catch (e: SecurityException) {
                        Timber.e("Error in GetIdentityHandler : $e")
                        val response = GetIdentityResponse(
                            success = false,
                            message = "Unable to provide identity -> Insufficient permissions to access IMEI. ${e.message}"
                        )
                        socket.send(gson.toJson(response).toByteArray(ZMQ.CHARSET), 0)
                    } catch (e: Exception) {
                        Timber.e("Error in GetIdentityHandler : $e")
                        val response = GetIdentityResponse(
                            success = false,
                            message = (e.message ?: Constants.UNKNOWN_ERROR_MSG)
                        )
                        socket.send(gson.toJson(response).toByteArray(ZMQ.CHARSET), 0)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("Error in GetIdentityHandler : $e")
            val response = GetIdentityResponse(
                success = false,
                message = (e.message ?: Constants.UNKNOWN_ERROR_MSG)
            )
            socket.send(gson.toJson(response).toByteArray(ZMQ.CHARSET), 0)
        }
    }
}