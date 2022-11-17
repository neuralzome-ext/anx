package com.flomobility.anx.hermes.comms.handlers

import com.flomobility.anx.hermes.api.StandardResponse
import com.flomobility.anx.hermes.api.SubscribeRequest
import com.flomobility.anx.hermes.assets.AssetManager
import com.flomobility.anx.hermes.comms.SessionManager
import com.flomobility.anx.hermes.comms.SocketManager
import com.flomobility.anx.hermes.other.Constants
import com.flomobility.anx.hermes.other.handleExceptions
import com.google.gson.Gson
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscribeAssetHandler @Inject constructor(
    private val sessionManager: SessionManager,
    private val gson: Gson,
    private val assetManager: AssetManager
) : Runnable {

    val interrupt = AtomicBoolean(false)

    lateinit var socket: ZMQ.Socket

    private var onSubscribed: ((Boolean) -> Unit)? = null
    fun doOnSubscribed(func: (Boolean) -> Unit) {
        onSubscribed = func
    }

    override fun run() {
        try {
            ZContext().use { ctx ->
                interrupt.set(false)

                socket = ctx.createSocket(SocketType.REP)
                socket.bind(SocketManager.SUBSCRIBE_ASSET_SOCKET_ADDR)
                Timber.i("Subscriber Handler running on ${SocketManager.SUBSCRIBE_ASSET_SOCKET_ADDR}")

                val poller = ctx.createPoller(1)
                poller.register(socket)

                while (!interrupt.get()) {
                    try {
                        poller.poll(100)
                        if (poller.pollin(0)) {
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
            Timber.i("Closing subscribe asset handler running on ${SocketManager.SUBSCRIBE_ASSET_SOCKET_ADDR}")
        } catch (e: InterruptedException) {
            Timber.i("Successfully stopped subscribe asset handler")
        } catch (e: Exception) {
            Timber.e("Error in subscribe asset handler : $e")
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