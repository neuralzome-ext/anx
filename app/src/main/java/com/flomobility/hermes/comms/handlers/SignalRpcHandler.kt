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
import java.util.concurrent.atomic.AtomicBoolean
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

    val interrupt = AtomicBoolean(false)

    @Inject
    lateinit var device: Device

    override fun run() {
        try {
            ZContext().use { ctx ->
                interrupt.set(false)

                socket = ctx.createSocket(SocketType.REP)
                socket.bind(SocketManager.SIGNAL_RPC_SOCKET_ADDR)
                Timber.i("Signal RPC handler running on ${SocketManager.SIGNAL_RPC_SOCKET_ADDR}")

                val poller = ctx.createPoller(1)
                poller.register(socket)

                while (!interrupt.get()) {
                    try {
                        poller.poll(100)
                        if (poller.pollin(0)) {
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
                                val result = phoneManager.invokeSignal(signalReq.signal)
                                val response = StandardResponse(
                                    success = result.success,
                                    message = result.message
                                )
                                socket.send(gson.toJson(response).toByteArray(ZMQ.CHARSET), 0)

                            }
                        }
                    } catch (e: Exception) {
                        Timber.e("Error in GetIdentityHandler : $e")
                        val response = StandardResponse(success = false, message = (e.message ?: Constants.UNKNOWN_ERROR_MSG))
                        socket.send(gson.toJson(response).toByteArray(ZMQ.CHARSET), 0)
                    }
                }
            }
            Timber.i("Closing Signal RPC handler running on ${SocketManager.SIGNAL_RPC_SOCKET_ADDR}")
        } catch (e: InterruptedException) {
           Timber.i("Successfully stopped Signal RPC handler")
        } catch (e: Exception) {
            Timber.e("Error in GetIdentityHandler : $e")
        }
    }
}