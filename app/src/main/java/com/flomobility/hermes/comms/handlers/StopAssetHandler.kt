package com.flomobility.hermes.comms.handlers

import com.flomobility.hermes.comms.SocketManager
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StopAssetHandler @Inject constructor(): Runnable {
    lateinit var socket: ZMQ.Socket

    override fun run() {
        ZContext().use { ctx ->
            socket = ctx.createSocket(SocketType.REP)
            socket.bind(SocketManager.STOP_ASSET_SOCKET_ADDR)
            while (true) {
                try {
                    socket.recv(0)?.let { bytes ->
                        val msgStr = String(bytes, ZMQ.CHARSET)

                        // TODO handle stop asset
                        handleStopAssetReq(msgStr)
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    private fun handleStopAssetReq(reqStr: String) {

    }
}