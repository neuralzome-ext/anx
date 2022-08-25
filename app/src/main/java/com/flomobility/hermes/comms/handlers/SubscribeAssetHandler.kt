package com.flomobility.hermes.comms.handlers

import com.flomobility.hermes.api.StandardResponse
import com.flomobility.hermes.api.SubscribeRequest
import com.flomobility.hermes.assets.AssetManager
import com.flomobility.hermes.comms.SessionManager
import com.flomobility.hermes.comms.SocketManager
import com.flomobility.hermes.other.handleExceptions
import com.google.gson.Gson
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscribeAssetHandler @Inject constructor(
    private val sessionManager: SessionManager,
    private val gson: Gson,
    private val assetManager: AssetManager
) : Runnable {

    lateinit var socket: ZMQ.Socket

    override fun run() {
        ZContext().use { ctx ->
            socket = ctx.createSocket(SocketType.REP)
            socket.bind(SocketManager.SUBSCRIBE_ASSET_SOCKET_ADDR)
            while (true) {
                try {
                    socket.recv(0)?.let { bytes ->
                        val msgStr = String(bytes, ZMQ.CHARSET)
                        Timber.d("[Subscribe] -- Request : $msgStr")
                        val subscribeReq =
                            gson.fromJson<SubscribeRequest>(
                                msgStr,
                                SubscribeRequest.type
                            )
                        handleSubscribeRequest(subscribeReq)
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    private fun handleSubscribeRequest(request: SubscribeRequest) {
        handleExceptions {
            val resp = StandardResponse()
            when {
                request.subscribe && sessionManager.connected -> {
                    resp.apply {
                        success = false
                        message = "active session currently running."
                    }
                    socket.send(gson.toJson(resp).toByteArray(ZMQ.CHARSET), 0)
                }
                request.subscribe && !sessionManager.connected -> {
                    sessionManager.connected = true
                    resp.success = true
                    socket.send(gson.toJson(resp).toByteArray(ZMQ.CHARSET), 0)
                    assetManager.publishAssetState()
                }
                !request.subscribe && sessionManager.connected -> {
                    sessionManager.connected = false
                    resp.success = true
                    socket.send(gson.toJson(resp).toByteArray(ZMQ.CHARSET), 0)
                    assetManager.stopAllAssets()
                }
                else -> {}
            }
        }
    }
}