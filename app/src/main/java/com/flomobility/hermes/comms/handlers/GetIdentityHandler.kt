package com.flomobility.hermes.comms.handlers

import com.flomobility.hermes.comms.SocketManager.Companion.GET_IDENTITY_SOCKET_ADDR
import com.flomobility.hermes.other.Constants
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetIdentityHandler @Inject constructor(
    // TODO add phone identity manager here
) : Runnable {

    lateinit var socket: ZMQ.Socket

    override fun run() {
        try {
            ZContext().use { ctx ->
                socket = ctx.createSocket(SocketType.REP)
                socket.receiveTimeOut = Constants.RPC_DEFAULT_TIMEOUT_IN_MS
                socket.bind(GET_IDENTITY_SOCKET_ADDR)
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        socket.recv(0)?.let { bytes ->
                            val data = String(bytes, ZMQ.CHARSET)
                            if(data.isEmpty()) {
                                // valid request
                                /*val result = phoneManager.getImei()
                                socket.send()*/
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