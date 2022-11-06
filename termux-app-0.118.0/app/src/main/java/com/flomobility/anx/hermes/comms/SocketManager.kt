package com.flomobility.anx.hermes.comms

import com.flomobility.anx.hermes.comms.handlers.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor(
    private val subscribeAssetHandler: SubscribeAssetHandler,
    private val startAssetHandler: StartAssetHandler,
    private val stopAssetHandler: StopAssetHandler,
    private val getIdentityHandler: GetIdentityHandler,
    private val signalRpcHandler: SignalRpcHandler
) {

    fun init() {
        // create standard sockets
        Thread(subscribeAssetHandler, "subscribe-asset-thread").start()
        Thread(startAssetHandler, "start-asset-socket-thread").start()
        Thread(stopAssetHandler, "stop-asset-socket-thread").start()
        Thread(getIdentityHandler, "get-identity-socket-thread").start()
        Thread(signalRpcHandler, "signal-rpc-socket-thread").start()
    }

    fun doOnSubscribed(func: (Boolean) -> Unit) {
        subscribeAssetHandler.doOnSubscribed(func)
    }

    fun destroy() {
        // TODO : interrupt all threads here
    }

    companion object {
        const val SUBSCRIBE_ASSET_SOCKET_ADDR = "tcp://*:10000"
        const val START_ASSET_SOCKET_ADDR = "tcp://*:10001"
        const val STOP_ASSET_SOCKET_ADDR = "tcp://*:10002"
        const val GET_IDENTITY_SOCKET_ADDR = "tcp://*:10004"
        const val SIGNAL_RPC_SOCKET_ADDR = "tcp://*:10005"
    }
}
