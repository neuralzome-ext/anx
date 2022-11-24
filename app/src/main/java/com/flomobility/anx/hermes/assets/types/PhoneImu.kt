package com.flomobility.anx.hermes.assets.types

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.flomobility.anx.hermes.api.model.AngularVelocity
import com.flomobility.anx.hermes.api.model.Imu
import com.flomobility.anx.hermes.api.model.LinearAcceleration
import com.flomobility.anx.hermes.api.model.Quaternion
import com.flomobility.anx.hermes.assets.AssetState
import com.flomobility.anx.hermes.assets.AssetType
import com.flomobility.anx.hermes.assets.BaseAsset
import com.flomobility.anx.hermes.assets.BaseAssetConfig
import com.flomobility.anx.hermes.common.Rate
import com.flomobility.anx.hermes.common.Result
import com.flomobility.anx.hermes.other.Constants
import com.flomobility.anx.hermes.other.handleExceptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneImu @Inject constructor(
    @ApplicationContext private val context: Context
) : BaseAsset() {

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val sensorEventListeners = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) {
                Timber.e("No sensor event happened")
                return
            } else {
                when (event.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> {
                        angularVelocity = AngularVelocity(
                            event.values[0].toDouble(),
                            event.values[1].toDouble(),
                            event.values[2].toDouble()
                        )
                    }
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        quaternion = Quaternion(
                            event.values[0].toDouble(),
                            event.values[1].toDouble(),
                            event.values[2].toDouble(),
                            event.values[3].toDouble()
                        )
                    }
                    Sensor.TYPE_LINEAR_ACCELERATION -> {
                        linearAcceleration =
                            LinearAcceleration(
                                event.values[0].toDouble(),
                                event.values[1].toDouble(),
                                event.values[2].toDouble()
                            )
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            Timber.d("Accuracy of ${sensor?.name} : $accuracy %")
        }
    }

    private var angularVelocity: AngularVelocity? = null

    private var linearAcceleration: LinearAcceleration? = null

    private var quaternion: Quaternion? = null

    private val _config = Config()

    private var publisherThread: Thread? = null

    override val id: String
        get() = "0"

    override val type: AssetType
        get() = AssetType.IMU

    override val config: BaseAssetConfig
        get() = _config


    override fun updateConfig(config: BaseAssetConfig): Result {
        if (config !is Config) {
            return Result(success = false, message = "Unknown config type")
        }

        this._config.apply {
            fps.value = config.fps.value
            portPub = config.portPub
            portSub = config.portSub
        }
        return Result(success = true)
    }

    override fun canRegister(): Boolean {
        if(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) == null) return false
        if(sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) == null) return false
        if(sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) return false
        return true
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
            updateState(AssetState.IDLE)
            return Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            updateState(AssetState.STREAMING)
            registerImu(Rate(hz = _config.fps.value as Int))
            publisherThread = Thread(Publisher(_config), "$type-$id-publisher-thread")
            publisherThread?.start()
            return Result(success = true)
        }
        return Result(success = false, Constants.UNKNOWN_ERROR_MSG)
    }

    override fun stop(): Result {
        handleExceptions(catchBlock = { e ->
            updateState(AssetState.STREAMING)
            return Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            updateState(AssetState.IDLE)
            publisherThread?.interrupt()
            publisherThread = null
            unregisterImu()
            return Result(success = true)
        }
        return Result(success = false, Constants.UNKNOWN_ERROR_MSG)
    }

    override fun destroy() {
        angularVelocity = null
        linearAcceleration = null
        quaternion = null
        publisherThread = null
    }

    private fun getImuData(): Imu {
        return Imu.new(
            linearAcceleration,
            angularVelocity,
            quaternion
        )
    }

    private fun registerImu(rate: Rate) {
        with(sensorManager) {
            getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.also { magnetometer ->
                registerListener(
                    sensorEventListeners,
                    magnetometer,
                    SensorManager.SENSOR_DELAY_FASTEST
//                    rate.toMicros().toInt()
                )
            }
            getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also { gyroscope ->
                registerListener(
                    sensorEventListeners,
                    gyroscope,
                    SensorManager.SENSOR_DELAY_FASTEST
//                    rate.toMicros().toInt()
                )
            }
            getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.also { linearAcc ->
                registerListener(
                    sensorEventListeners,
                    linearAcc,
                    SensorManager.SENSOR_DELAY_FASTEST
//                    rate.toMicros().toInt()
                )
            }
        }
    }

    private fun unregisterImu() {
        sensorManager.unregisterListener(sensorEventListeners)
    }

    inner class Publisher(val config: Config) : Runnable {

        lateinit var socket: ZMQ.Socket

        override fun run() {
            ZContext().use { ctx ->
                val address = "tcp://*:${config.portPub}"
                socket = ctx.createSocket(SocketType.PUB)
                socket.bind(address)
                // wait to bind
                Thread.sleep(500)
                Timber.d("[Publishing] imu on ${config.portPub} at delay of ${1000L / (config.fps.value as Int)}")
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        val jsonStr = this@PhoneImu.getImuData().toJson()
//                        Timber.d("[Publishing] -- imu : $jsonStr")
                        GlobalScope.launch {
                            assetOut.send(jsonStr)
                        }
                        socket.send(jsonStr.toByteArray(ZMQ.CHARSET), ZMQ.DONTWAIT)
                        Thread.sleep(1000L / (config.fps.value as Int))
                    } catch (e: InterruptedException) {
                        Timber.i("Publisher closed")
                        socket.unbind(address)
                        socket.close()
                    } catch (e: Exception) {
                        Timber.e(e)
                        return
                    }
                }
            }
        }
    }

    class Config : BaseAssetConfig() {

        val fps = Field<Int>()
        private val fpsRange = listOf(1, 2, 5, 10, 15, 25, 30, 60, 75, 100, 125, 150, 200)

        init {
            fps.range = fpsRange
            fps.name = "fps"
            fps.value = DEFAULT_FPS

            portSub = -1
        }

        companion object {
            private const val DEFAULT_FPS = 15
        }

        override fun getFields(): List<Field<*>> {
            return listOf(fps)
        }
    }

}
