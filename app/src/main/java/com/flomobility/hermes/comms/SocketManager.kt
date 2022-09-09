package com.flomobility.hermes.comms

import com.flomobility.hermes.comms.handlers.StartAssetHandler
import com.flomobility.hermes.comms.handlers.StopAssetHandler
import com.flomobility.hermes.comms.handlers.SubscribeAssetHandler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor(
    private val subscribeAssetHandler: SubscribeAssetHandler,
    private val startAssetHandler: StartAssetHandler,
    private val stopAssetHandler: StopAssetHandler
) {

    fun init() {
        // create standard sockets
        Thread(subscribeAssetHandler, "subscribe-asset-thread").start()
        Thread(startAssetHandler, "start-asset-socket-thread").start()
        Thread(stopAssetHandler, "stop-asset-socket-thread").start()
        Thread(stopAssetHandler, "stop-asset-socket-thread").start()
    }

    companion object {
        const val SUBSCRIBE_ASSET_SOCKET_ADDR = "tcp://*:10000"
        const val START_ASSET_SOCKET_ADDR = "tcp://*:10001"
        const val STOP_ASSET_SOCKET_ADDR = "tcp://*:10002"
        const val GET_IDENTITY_SOCKET_ADDR = "tcp://*:10004"
    }
}