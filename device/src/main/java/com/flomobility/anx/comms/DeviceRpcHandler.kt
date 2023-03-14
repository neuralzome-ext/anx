package com.flomobility.anx.comms

import android.content.Context
import com.flomobility.anx.native.zmq.RpcServer
import com.flomobility.anx.rpc.*
import com.flomobility.anx.utils.AddressUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import org.zeromq.ZMQ
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class to handle RPC calls received on a specified port
 *
 * */
@Singleton
class DeviceRpcHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getAnxVersionRpc: GetAnxVersionRpc,
    private val getAssetStateRpc: GetAssetStateRpc,
    private val getAvailableLanguagesRpc: GetAvailableLanguagesRpc,
    private val getFloOsVersionRpc: GetFloOsVersionRpc,
    private val getImeiNumbersRpc: GetImeiNumbersRpc,
    private val getPhoneNumbersRpc: GetPhoneNumbersRpc,
    private val getIsTtsBusyRpc: IsTtsBusyRpc,
    private val getRebootRpc: RebootRpc,
    private val getRestartAnxServiceRpc: RestartAnxServiceRpc,
    private val getSetHotspotRpc: SetHotspotRpc,
    private val getSetWifiRpc: SetWifiRpc,
    private val getShutdownRpc: ShutdownRpc,
    private val getStartDeviceCameraRpc: StartDeviceCameraRpc,
    private val getStartDeviceGnssRpc: StartDeviceGnssRpc,
    private val getStartDeviceImuRpc: StartDeviceImuRpc,
    private val getStartUsbTetheringRpc: StartUsbTetheringRpc,
    private val geStopDeviceCameraRpc: StopDeviceCameraRpc,
    private val getStopDeviceGnssRpc: StopDeviceGnssRpc,
    private val getStopDeviceImuRpc: StopDeviceImuRpc,
    private val getStopUsbTetheringRpc: StartUsbTetheringRpc,
    private val getTtsRpc: TtsRpc
) {

    private var port: Int = 10002

    private var rpcThread: RpcThread? = null

    private val rpcRegistry = hashMapOf<String, Rpc<*, *>>()

    init {
        addAllRpcToRegistry()
    }

    private fun addAllRpcToRegistry() {
        rpcRegistry[getAnxVersionRpc.name] = getAnxVersionRpc
        rpcRegistry[getAssetStateRpc.name] = getAssetStateRpc
        rpcRegistry[getAvailableLanguagesRpc.name] = getAvailableLanguagesRpc
        rpcRegistry[getFloOsVersionRpc.name] = getFloOsVersionRpc
        rpcRegistry[getImeiNumbersRpc.name] = getImeiNumbersRpc
        rpcRegistry[getPhoneNumbersRpc.name] = getPhoneNumbersRpc
        rpcRegistry[getIsTtsBusyRpc.name] = getIsTtsBusyRpc
        rpcRegistry[getRebootRpc.name] = getRebootRpc
        rpcRegistry[getRestartAnxServiceRpc.name] = getRestartAnxServiceRpc
        rpcRegistry[getSetHotspotRpc.name] = getSetHotspotRpc
        rpcRegistry[getSetWifiRpc.name] = getSetWifiRpc
        rpcRegistry[getShutdownRpc.name] = getShutdownRpc
        rpcRegistry[getStartDeviceCameraRpc.name] = getStartDeviceCameraRpc
        rpcRegistry[getStartDeviceGnssRpc.name] = getStartDeviceGnssRpc
        rpcRegistry[getStartDeviceImuRpc.name] = getStartDeviceImuRpc
        rpcRegistry[getStartUsbTetheringRpc.name] = getStartUsbTetheringRpc
        rpcRegistry[geStopDeviceCameraRpc.name] = geStopDeviceCameraRpc
        rpcRegistry[getStopDeviceGnssRpc.name] = getStopDeviceGnssRpc
        rpcRegistry[getStopDeviceImuRpc.name] = getStopDeviceImuRpc
        rpcRegistry[getStopUsbTetheringRpc.name] = getStopUsbTetheringRpc
        rpcRegistry[getTtsRpc.name] = getTtsRpc
    }

    fun init(port: Int) {
        this.port = port
        this.rpcThread = RpcThread()
        this.rpcThread?.start()
    }

    fun destroy() {
        rpcThread?.interrupt?.set(true)
        rpcThread?.join()
        rpcThread = null
    }

    inner class RpcThread : Thread() {

        val interrupt = AtomicBoolean(false)

//        private lateinit var socket: ZMQ.Socket

        private lateinit var rpcServer: RpcServer

        override fun run() {
            rpcServer = RpcServer()
            try {
                rpcServer.init(
                    AddressUtils.getNamedPipeAddress(
                        context, "device"
                    )
                )
                /*ZContext().use { ctx ->
                    interrupt.set(false)

                    val address = "tcp://localhost:$port"

                    socket = ctx.createSocket(SocketType.REP)
                    socket.bind(address)

                    Thread.sleep(Constants.SOCKET_BIND_DELAY_IN_MS)

                    Timber.tag(TAG).i("Device RPC Handler server is running on $address")
                    val poller = ctx.createPoller(1)
                    poller.register(socket)

                    while (!interrupt.get()) {
                        try {
                            poller.poll(100)
                            if (poller.pollin(0)) {
                                socket.recv(0)?.let { bytes ->
                                    if (!socket.hasReceiveMore()) {
                                        Timber.tag(TAG).i("Invalid RPC")
                                        socket.sendStdResponse(
                                            success = false,
                                            message = "Invalid RPC : No data attached"
                                        )
                                    } else {
                                        Timber.tag(TAG).i("RPC data")
                                        val data = socket.recv()
                                        val rpcName = String(bytes, ZMQ.CHARSET)
                                        val rpc = rpcRegistry[rpcName]
                                        handleRpc(rpc, data)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Timber.tag(TAG).e("Error in $TAG : ${e.message}")
                        }
                    }
                }*/

                while (!this.interrupt.get()) {
                    val payload = rpcServer.listen()
                    Timber.tag(TAG).i("Payload received : $payload")
                    val rpc = rpcRegistry[payload.rpcName]
                    handleRpc(rpc, payload.data)
                }
                Timber.tag(TAG).i("Successfully stopped device RPC handler server")
            } catch (e: Exception) {
                Timber.tag(TAG).e("Error in $TAG : ${e.message}")
            }
        }

        private fun handleRpc(rpc: Rpc<*, *>?, data: ByteArray) {
            if (rpc == null) {
                /*socket.sendStdResponse(
                    success = false,
                    message = "Invalid RPC received : "
                )*/
                return
            }
            rpcServer.send(rpc.execute(data).toByteArray())
//            socket.send(rpc.execute(data).toByteArray())
        }

    }

    companion object {
        private const val TAG = "DeviceRpcHandler"
    }

}
