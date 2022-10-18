package com.flomobility.hermes.assets.types

import android.content.Context
import android.location.OnNmeaMessageListener
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.annotation.RequiresApi
import com.flomobility.hermes.api.model.GNSSData
import com.flomobility.hermes.assets.AssetState
import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.BaseAsset
import com.flomobility.hermes.assets.BaseAssetConfig
import com.flomobility.hermes.common.Result
import com.flomobility.hermes.phonegnss.PhoneGNSSManager
import com.flomobility.hermes.other.Constants
import com.flomobility.hermes.other.handleExceptions
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


/**
 * Provides implementation of Asset's methods.
 * Start / Stop / Update Config
 * Publish NMEA data over ZMQ
 */
@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class PhoneGNSS @Inject constructor(
    @ApplicationContext private val context: Context,
    private val phoneGnssManager: PhoneGNSSManager,
    private val gson: Gson
) : BaseAsset, OnNmeaMessageListener {

    companion object {
        const val TAG = "PhoneGNSS"
        private const val MSG_NMEA_DATA = 10
    }

    private var nmeaMsgThread: NMEAMessageThread? = null

    private val _id = "0"

    private val _config = Config()

    private var _state = AssetState.IDLE

    lateinit var socket: ZMQ.Socket

    override val id: String
        get() = _id
    override val type: AssetType
        get() = AssetType.GNSS
    override val config: BaseAssetConfig
        get() = _config
    override val state: AssetState
        get() = AssetState.IDLE

    override fun updateConfig(config: BaseAssetConfig): Result {
        if (config !is Config) {
            return Result(success = false, message = "Unknown config type")
        }

        this._config.apply {
            this.portPub = config.portPub
        }

        return Result(success = true)
    }

    override fun start(): Result {
        handleExceptions(catchBlock = { e ->
            return Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            try {
                _state = AssetState.STREAMING
                GlobalScope.launch(Dispatchers.Main) {
                    phoneGnssManager.init(this@PhoneGNSS)
                }
                nmeaMsgThread = NMEAMessageThread()
                nmeaMsgThread?.updateAddress()
                nmeaMsgThread?.start()
            } catch (e: Exception) {
                Timber.e(e)
            }


            return Result(success = true)
        }
        return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
    }

    override fun stop(): Result {
        handleExceptions(catchBlock = { e ->
            return Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            _state = AssetState.IDLE
            GlobalScope.launch(Dispatchers.Main) {
                phoneGnssManager.stop(this@PhoneGNSS)
            }
            return Result(success = true)
        }
        return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
    }

    override fun destroy() {
        nmeaMsgThread?.kill()
        nmeaMsgThread = null
    }

    class Config : BaseAssetConfig() {

        val fps = Field<Int>()

        init {
            fps.range = listOf(1)
            fps.name = "fps"
            fps.value = DEFAULT_FPS
        }

        override fun getFields(): List<Field<*>> {
            return listOf(fps)
        }

        companion object {
            private const val DEFAULT_FPS = 1
        }
    }

    override fun onNmeaMessage(nmeadata: String?, timestamp: Long) {
        Timber.d("onNmeaMessage $nmeadata\n $timestamp")
        val gnssData = GNSSData(
            nmea = nmeadata.toString(),
            //timestamp = timestamp
        )

        nmeaMsgThread?.publishNMEAData(gnssData)
    }


    inner class NMEAMessageThread : Thread() {

        init {
            name = "${this@PhoneGNSS.name}-NMEAMessage-thread"
        }

        private var handler: NMEAMessageThreadHandler? = null

        private lateinit var socket: ZMQ.Socket

        private var address = ""

        fun updateAddress() {
            address = "tcp://*:${_config.portPub}"
        }

        override fun run() {
            while (address.isEmpty()) {
                continue
            }
            Timber.i("[${this@PhoneGNSS.name}] - Starting Publisher on $address")
            try {
                ZContext().use { ctx ->
                    socket = ctx.createSocket(SocketType.PUB)
                    socket.bind(address)
                    sleep(Constants.SOCKET_BIND_DELAY_IN_MS)
                    Looper.prepare()
                    handler = NMEAMessageThreadHandler(Looper.myLooper() ?: return)
                    Looper.loop()
                }
                socket.unbind(address)
                sleep(Constants.SOCKET_BIND_DELAY_IN_MS)
                Timber.i("[${this@PhoneGNSS.name}] - Stopping Publisher on $address")
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        fun publishNMEAData(nmea: GNSSData) {
            handler?.sendMsg(MSG_NMEA_DATA, nmea)
        }

        fun kill() {
            handler?.sendMsg(Constants.SIG_KILL_THREAD)
        }

        inner class NMEAMessageThreadHandler(private val myLooper: Looper) : Handler(myLooper) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_NMEA_DATA -> {
                        val gnssData = msg.obj as GNSSData
                        gnssData.let {
                            socket.send(gson.toJson(it).toByteArray(ZMQ.CHARSET), 0)
                        }
                    }
                    Constants.SIG_KILL_THREAD -> {
                        myLooper.quitSafely()
                    }
                }
            }

            fun sendMsg(what: Int, obj: Any? = null) {
                sendMessage(obtainMessage(what, obj))
            }
        }
    }

}