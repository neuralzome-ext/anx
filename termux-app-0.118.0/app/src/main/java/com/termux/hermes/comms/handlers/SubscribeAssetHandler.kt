package com.termux.hermes.comms.handlers

import com.termux.hermes.api.StandardResponse
import com.termux.hermes.api.SubscribeRequest
import com.termux.hermes.assets.AssetManager
import com.termux.hermes.comms.SessionManager
import com.termux.hermes.comms.SocketManager
import com.termux.hermes.other.Constants
import com.termux.hermes.other.handleExceptions
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

    private var onSubscribed: ((Boolean) -> Unit)? = null
    fun doOnSubscribed(func: (Boolean) -> Unit) {
        onSubscribed = func
    }

    override fun run() {
        try {
            ZContext().use { ctx ->
                socket = ctx.createSocket(SocketType.REP)
                socket.bind(SocketManager.SUBSCRIBE_ASSET_SOCKET_ADDR)
                Timber.i("Subscriber Handler running on ${SocketManager.SUBSCRIBE_ASSET_SOCKET_ADDR}")
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        socket.recv(0)?.let { bytes ->
                            val msgStr = String(bytes, ZMQ.CHARSET)
                            Timber.i("[Subscribe] -- Request : $msgStr")
                            val subscribeReq =
                                gson.fromJson<SubscribeRequest>(
                                    msgStr,
                                    SubscribeRequest.type
                                )
                            handleSubscribeRequest(subscribeReq)
                        }
                    } catch (e: Exception) {
                        Timber.e("Error in subscribe asset handler : $e")
                        socket.send(
                            gson.toJson(
                                StandardResponse(
                                    success = false,
                                    message = e.message ?: Constants.UNKNOWN_ERROR_MSG
                                )
                            ).toByteArray(ZMQ.CHARSET), 0
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("Error in subscribe asset handler : $e")
            socket.send(
                gson.toJson(
                    StandardResponse(
                        success = false,
                        message = e.message ?: Constants.UNKNOWN_ERROR_MSG
                    )
                ).toByteArray(ZMQ.CHARSET), 0
            )
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
                    sessionManager.connectedDeviceIp = request.ip
                    resp.success = true
                    socket.send(gson.toJson(resp).toByteArray(ZMQ.CHARSET), 0)
                    assetManager.publishAssetState()
                    onSubscribed?.invoke(true)
                }
                !request.subscribe && sessionManager.connected -> {
                    sessionManager.connected = false
                    sessionManager.connectedDeviceIp = ""
                    resp.success = true
                    socket.send(gson.toJson(resp).toByteArray(ZMQ.CHARSET), 0)
                    assetManager.stopAllAssets()
                    onSubscribed?.invoke(false)
                }
                else -> {}
            }
        }
    }
}
