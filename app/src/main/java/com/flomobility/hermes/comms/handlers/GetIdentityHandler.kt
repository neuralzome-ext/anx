package com.flomobility.hermes.comms.handlers

import android.os.Build
import androidx.annotation.RequiresApi
import com.flomobility.hermes.comms.SocketManager.Companion.GET_IDENTITY_SOCKET_ADDR
import com.flomobility.hermes.other.Constants
import com.flomobility.hermes.phone.PhoneManager
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@RequiresApi(Build.VERSION_CODES.O)
class GetIdentityHandler @Inject constructor(
    private val phoneManager: PhoneManager
) : Runnable {

    lateinit var socket: ZMQ.Socket

    override fun run() {
        try {
            ZContext().use { ctx ->
                socket = ctx.createSocket(SocketType.REP)
                socket.sendTimeOut = Constants.RPC_DEFAULT_TIMEOUT_IN_MS
                socket.receiveTimeOut = Constants.RPC_DEFAULT_TIMEOUT_IN_MS
                socket.bind(GET_IDENTITY_SOCKET_ADDR)
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        socket.recv(0)?.let { bytes ->
                            val data = String(bytes, ZMQ.CHARSET)
                            if(data.isEmpty()) {
                                // valid request
                                val result = phoneManager.getIdentity()
                                socket.send(result.toByteArray(ZMQ.CHARSET), 0)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e("Error in GetIdentityHandler : $e")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("Error in GetIdentityHandler : $e")
        }
    }
}