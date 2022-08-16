package com.flomobility.hermes.comms

import com.flomobility.hermes.comms.handlers.SubscribeAssetHandler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor(
    private val subscribeAssetHandler: SubscribeAssetHandler,
) {

    fun init() {
        // create standard sockets
        Thread(subscribeAssetHandler, "subscribe-asset-thread").start()

    }

    companion object {
        const val SUBSCRIBE_ASSET_SOCKET_ADDR = "tcp://*:10000"
    }
}