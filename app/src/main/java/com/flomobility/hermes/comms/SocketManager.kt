package com.flomobility.hermes.comms

import android.os.Build
import androidx.annotation.RequiresApi
import com.flomobility.hermes.comms.handlers.*
import com.flomobility.hermes.other.ThreadStatus
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class SocketManager @Inject constructor(
    private val subscribeAssetHandler: SubscribeAssetHandler,
    private val startAssetHandler: StartAssetHandler,
    private val stopAssetHandler: StopAssetHandler,
    private val getIdentityHandler: GetIdentityHandler,
    private val signalRpcHandler: SignalRpcHandler
) {

    var threadStatus = ThreadStatus.IDLE
        private set

    var subscribeAssetHandlerThread: Thread? = null
    var startAssetHandlerThread: Thread? = null
    var stopAssetHandlerThread: Thread? = null
    var getIdentityHandlerThread: Thread? = null
    var signalRpcHandlerThread: Thread? = null

    fun init() {
        threadStatus = ThreadStatus.ACTIVE
        // create standard sockets
        subscribeAssetHandlerThread = Thread(subscribeAssetHandler, "subscribe-asset-thread")
        subscribeAssetHandlerThread?.start()

        startAssetHandlerThread = Thread(startAssetHandler, "start-asset-socket-thread")
        startAssetHandlerThread?.start()

        stopAssetHandlerThread = Thread(stopAssetHandler, "stop-asset-socket-thread")
        stopAssetHandlerThread?.start()

        getIdentityHandlerThread = Thread(getIdentityHandler, "get-identity-socket-thread")
        getIdentityHandlerThread?.start()

        signalRpcHandlerThread = Thread(signalRpcHandler, "signal-rpc-socket-thread")
        signalRpcHandlerThread?.start()
    }

    fun doOnSubscribed(func: (Boolean) -> Unit) {
        subscribeAssetHandler.doOnSubscribed(func)
    }

    fun destroy() {
        threadStatus = ThreadStatus.DISPOSED

        subscribeAssetHandler.interrupt.set(true)
        subscribeAssetHandlerThread?.join()

        startAssetHandler.interrupt.set(true)
        startAssetHandlerThread?.join()

        stopAssetHandler.interrupt.set(true)
        stopAssetHandlerThread?.join()

        getIdentityHandler.interrupt.set(true)
        getIdentityHandlerThread?.join()

        signalRpcHandler.interrupt.set(true)
        signalRpcHandlerThread?.join()

        subscribeAssetHandlerThread = null
        startAssetHandlerThread = null
        stopAssetHandlerThread = null
        getIdentityHandlerThread = null
        signalRpcHandlerThread = null
    }

    companion object {
        const val SUBSCRIBE_ASSET_SOCKET_ADDR = "tcp://*:10000"
        const val START_ASSET_SOCKET_ADDR = "tcp://*:10001"
        const val STOP_ASSET_SOCKET_ADDR = "tcp://*:10002"
        const val GET_IDENTITY_SOCKET_ADDR = "tcp://*:10004"
        const val SIGNAL_RPC_SOCKET_ADDR = "tcp://*:10005"
    }
}