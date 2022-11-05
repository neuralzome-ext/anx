package com.flomobility.hermes.assets.types

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.flomobility.hermes.api.model.PhoneState
import com.flomobility.hermes.assets.AssetState
import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.BaseAsset
import com.flomobility.hermes.assets.BaseAssetConfig
import com.flomobility.hermes.common.Result
import com.flomobility.hermes.other.Constants
import com.flomobility.hermes.other.handleExceptions
import com.flomobility.hermes.phone.PhoneManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class Phone @Inject constructor(
    @ApplicationContext private val context: Context,
    private val phoneManager: PhoneManager,
    private val gson: Gson
) : BaseAsset() {

    private val _id = "0"

    private val _config = Config()

    private var publisherThread: PublisherThread? = null

    override val id: String
        get() = _id

    override val type: AssetType
        get() = AssetType.PHONE

    override val config: BaseAssetConfig
        get() = _config

    override fun updateConfig(config: BaseAssetConfig): Result {
        if (config !is Config) {
            return Result(success = false, message = "Unknown config type")
        }

        this._config.apply {
            this.fps.value = config.fps.value
            this.portPub = config.portPub
        }

        return Result(success = true)
    }

    override fun start(): Result {
        handleExceptions(catchBlock = { e ->
            updateState(AssetState.IDLE)
            return Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            updateState(AssetState.STREAMING)
            publisherThread = PublisherThread()
            publisherThread?.start()
            return Result(success = true)
        }
        return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
    }

    override fun stop(): Result {
        handleExceptions(catchBlock = { e ->
            updateState(AssetState.STREAMING)
            return Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            updateState(AssetState.IDLE)
            publisherThread?.interrupt()
            publisherThread = null
            return Result(success = true)
        }
        return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
    }

    override fun destroy() {
        publisherThread = null
    }

    inner class PublisherThread : Thread() {

        lateinit var socket: ZMQ.Socket

        override fun run() {
            val address = "tcp://*:${_config.portPub}"
            try {
                ZContext().use { ctx ->
                    Timber.d("Starting phone-${this@Phone._id} publisher on ${_config.portPub}")
                    socket = ctx.createSocket(SocketType.PUB)
                    socket.bind(address)
                    // wait
                    sleep(500)
                    while (!currentThread().isInterrupted) {
                        try {
                            val chargingStatus = phoneManager.getChargingStatus()
                            val cpuRam = phoneManager.getMemoryInfo()
                            val cpuTemp = phoneManager.getCPUTemperature()
                            val cpuUsage = phoneManager.getCpu()
                            val gpuUsage = phoneManager.getGpuUsage()
                            val phoneState = PhoneState(
                                charging = chargingStatus,
                                cpuRamUsage = cpuRam,
                                cpuTemp = cpuTemp,
                                cpuUsage = cpuUsage,
                                gpuUsage = gpuUsage,
                                gpuVramUsage = -1.0
                            )
                            socket.send(gson.toJson(phoneState).toByteArray(ZMQ.CHARSET), 0)
                            sleep(1000L / _config.fps.value)
                        } catch (e: InterruptedException) {
                            break
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                    Timber.d("Stopping phone-${this@Phone._id} publisher")
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

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
}