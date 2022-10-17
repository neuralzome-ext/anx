package com.flomobility.hermes.comms.handlers

import android.os.Build
import androidx.annotation.RequiresApi
import com.flomobility.hermes.api.SignalRequest
import com.flomobility.hermes.api.StandardResponse
import com.flomobility.hermes.comms.SessionManager
import com.flomobility.hermes.comms.SocketManager
import com.flomobility.hermes.other.Constants
import com.flomobility.hermes.phone.Device
import com.flomobility.hermes.phone.PhoneManager
import com.google.gson.Gson
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class SignalRpcHandler @Inject constructor(
    private val phoneManager: PhoneManager,
    private val sessionManager: SessionManager,
    private val gson: Gson
): Runnable {

    lateinit var socket: ZMQ.Socket

    @Inject
    lateinit var device: Device

    override fun run() {
        try {
            ZContext().use { ctx ->
                socket = ctx.createSocket(SocketType.REP)
                socket.bind(SocketManager.SIGNAL_RPC_SOCKET_ADDR)
                Timber.i("Signal RPC handler running on ${SocketManager.SIGNAL_RPC_SOCKET_ADDR}")
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        socket.recv(0)?.let { bytes ->
                            val msgStr = String(bytes, ZMQ.CHARSET)
                            if (!sessionManager.connected) {
                                throw IllegalStateException("Cannot invoke signal without being subscribed! Subscribe first.")
                            }
                            Timber.d("[Signal RPC] -- Request : $msgStr")
                            val signalReq = gson.fromJson<SignalRequest>(
                                msgStr,
                                SignalRequest.type
                            )
                            val response = if (device.isRooted)
                                StandardResponse(success = device.isRooted)
                            else
                                StandardResponse(
                                    success = false,
                                    message = "Can't shutdown as system isn't rooted"
                                )
                            socket.send(gson.toJson(response).toByteArray(ZMQ.CHARSET), 0)

                            phoneManager.invokeSignal(signalReq.signal)
                        }
                    } catch (e: Exception) {
                        Timber.e("Error in GetIdentityHandler : $e")
                        val response = StandardResponse(success = false, message = (e.message ?: Constants.UNKNOWN_ERROR_MSG))
                        socket.send(gson.toJson(response).toByteArray(ZMQ.CHARSET), 0)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("Error in GetIdentityHandler : $e")
            val response = StandardResponse(success = false, message = (e.message ?: Constants.UNKNOWN_ERROR_MSG))
            socket.send(gson.toJson(response).toByteArray(ZMQ.CHARSET), 0)
        }
    }
}