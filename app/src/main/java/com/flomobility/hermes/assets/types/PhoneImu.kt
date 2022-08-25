package com.flomobility.hermes.assets.types

import com.flomobility.hermes.api.model.AngularVelocity
import com.flomobility.hermes.api.model.Imu
import com.flomobility.hermes.api.model.LinearAcceleration
import com.flomobility.hermes.api.model.Quaternion
import com.flomobility.hermes.assets.AssetState
import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.BaseAsset
import com.flomobility.hermes.assets.BaseAssetConfig
import com.flomobility.hermes.common.Result
import com.flomobility.hermes.other.Constants
import com.flomobility.hermes.other.handleExceptions
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber

class PhoneImu : BaseAsset {

    private val _config = Config()

    private var _state = AssetState.IDLE

    private var publisherThread: Thread? = null

    override val id: String
        get() = "in72e"

    override val type: AssetType
        get() = AssetType.IMU

    override val config: BaseAssetConfig
        get() = _config

    override val state: AssetState
        get() = _state

    override fun updateConfig(config: BaseAssetConfig): Result {
        if (config !is PhoneImu.Config) {
            return Result(success = false, message = "Unknown config type")
        }

        this._config.apply {
            fps.value = config.fps.value
            portPub = config.portPub
            portSub = config.portSub
        }
        return Result(success = true)
    }

    override fun getDesc(): Map<String, Any> {
        val map = hashMapOf<String, Any>("id" to id)
        config.getFields().forEach { field ->
            map[field.name] = field.range
        }
        return map
    }

    override fun start(): Result {
        handleExceptions(catchBlock = { e ->
            return Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            _state = AssetState.STREAMING
            publisherThread = Thread(Publisher(_config), "$type-$id-publisher-thread")
            publisherThread?.start()
            return Result(success = true)
        }
        return Result(success = false, Constants.UNKNOWN_ERROR_MSG)
    }

    override fun stop(): Result {
        handleExceptions(catchBlock = { e ->
            return Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            _state = AssetState.IDLE
            publisherThread?.interrupt()
            publisherThread = null
            return Result(success = true)
        }
        return Result(success = false, Constants.UNKNOWN_ERROR_MSG)
    }

    fun getImuData(): Imu {
        // TODO integrate with sensor manager
        return Imu(
            LinearAcceleration(0.0, 0.0, 0.0),
            AngularVelocity(0.0, 0.0, 0.0),
            Quaternion(0.0, 0.0, 0.0, 1.0)
        )
    }

    inner class Publisher(val config: PhoneImu.Config) : Runnable {

        lateinit var socket: ZMQ.Socket

        override fun run() {
            ZContext().use { ctx ->
                socket = ctx.createSocket(SocketType.PUB)
                socket.bind("tcp://*:${config.portPub}")
                // wait to bind
                Thread.sleep(500)
                Timber.d("[Publishing] imu on ${config.portPub} at delay of ${1000L / (config.fps.value as Int)}")
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        val jsonStr = this@PhoneImu.getImuData().toJson()
                        Timber.d("[Publishing] -- imu : $jsonStr")
                        socket.send(jsonStr.toByteArray(ZMQ.CHARSET), 0)
                        Thread.sleep(1000L / (config.fps.value as Int))
                    } catch (e: Exception) {
                        Timber.e(e)
                        break
                    }
                }
            }
        }
    }

    class Config : BaseAssetConfig() {

        val fps = Field<Int>()
        private val fpsRange = listOf(1, 2, 5, 10, 15, 25, 30, 60, 75, 100, 125, 150, 200)

        val stream = Field<Stream>()

        init {
            fps.range = fpsRange
            fps.name = "fps"
            fps.value = DEFAULT_FPS

            stream.range = listOf(
                Stream(
                    fps = 30,
                    width = 1920,
                    height = 1080,
                    pixelFormat = Stream.PixelFormat.MJPEG
                )
            )
            stream.name = "stream"
            stream.value = Stream.DEFAULT
        }

        companion object {
            private const val DEFAULT_FPS = 15
        }

        override fun getFields(): List<Field<*>> {
            return listOf(fps)
        }

        data class Stream(
            val fps: Int,
            val width: Int,
            val height: Int,
            val pixelFormat: PixelFormat
        ) {
            enum class PixelFormat(val alias: String) {
                MJPEG("mjpeg"), YUYV("yuyv")
            }

            companion object {
                val DEFAULT = Stream(
                    fps = 30,
                    width = 1920,
                    height = 1080,
                    pixelFormat = Stream.PixelFormat.MJPEG
                )
            }
        }
    }

}