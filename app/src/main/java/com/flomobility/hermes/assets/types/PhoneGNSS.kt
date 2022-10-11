package com.flomobility.hermes.assets.types

import android.content.Context
import android.location.OnNmeaMessageListener
import android.os.Build
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

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
    }

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

    private var publisherThread: PublisherGnssThread? = null
    private var gnssData: GNSSData? = null

    override fun updateConfig(config: BaseAssetConfig): Result {
        if (config !is Config) {
            return Result(success = false, message = "Unknown config type")
        }

        this._config.apply {
            this.time.value = config.time.value
            this.distance.value = config.distance.value
            this.fps.value = config.fps.value
            this.portPub = config.portPub
        }

        phoneGnssManager.updateConfig(config, this)

        return Result(success = true)
    }

    override fun start(): Result {
        handleExceptions(catchBlock = { e ->
            return Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            try {
                _state = AssetState.STREAMING
                phoneGnssManager.init(this, Config())
                publisherThread = PublisherGnssThread()
                publisherThread?.start()
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
            phoneGnssManager.stop(this)
            publisherThread?.interrupt()
            publisherThread = null
            return Result(success = true)
        }
        return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
    }

    override fun destroy() {
        gnssData = null
        publisherThread = null
    }

    class Config : BaseAssetConfig() {

        val distance = Field<Float>()
        val time = Field<Long>()
        val fps = Field<Int>()

        init {
            time.value = TimeUnit.SECONDS.toMillis(60L)
            distance.value = 1.0f
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
        gnssData = GNSSData(
            nmea = nmeadata.toString(),
            //timestamp = timestamp
        )
    }

    inner class PublisherGnssThread : Thread() {

        lateinit var socket: ZMQ.Socket

        override fun run() {
            val address = "tcp://*:${_config.portPub}"
            try {
                ZContext().use { ctx ->
                    Timber.d("Starting phoneGNSS-${this@PhoneGNSS._id} publisher on ${_config.portPub}")
                    socket = ctx.createSocket(SocketType.PUB)
                    socket.bind(address)
                    // wait
                    sleep(Constants.SOCKET_BIND_DELAY_IN_MS)
                    while (!Thread.currentThread().isInterrupted) {
                        try {
                            gnssData?.let {
                                socket.send(gson.toJson(it).toByteArray(ZMQ.CHARSET), 0)
                            }
                            sleep(1000L / (_config.fps.value as Int))
                        } catch (e: InterruptedException) {
                            break
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                    Timber.d("Stopping phoneGNSS-${this@PhoneGNSS._id} publisher")
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}