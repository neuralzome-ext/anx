package com.flomobility.anx.assets.imu

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.flomobility.anx.assets.Asset
import com.flomobility.anx.common.Rate
import com.flomobility.anx.common.Result
import com.flomobility.anx.native.zmq.Publisher
import com.flomobility.anx.proto.Assets
import com.flomobility.anx.proto.Assets.ImuData
import com.flomobility.anx.proto.Assets.ImuData.Filtered
import com.flomobility.anx.proto.Common
import com.flomobility.anx.utils.AddressUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import org.zeromq.ZContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceImu @Inject constructor(
    @ApplicationContext private val context: Context
) : Asset<Assets.StartDeviceImu>() {

    companion object {
        private const val TAG = "DeviceImu"
    }

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private var angularVelocity = Common.Vector3.newBuilder()

    private var acceleration = Common.Vector3.newBuilder()

    private var orientation = Common.Quaternion.newBuilder()

    private var rawAngularVelocity = Common.Vector3.newBuilder()

    private var rawAcceleration = Common.Vector3.newBuilder()

    private var magneticField = Common.Vector3.newBuilder()

    private var imuData = ImuData.newBuilder()

    private var publisherThread: PublisherThread? = null

    fun getDeviceImuSelect(): Assets.DeviceImuSelect {
        return Assets.DeviceImuSelect.newBuilder().apply {
            this.addAllFps(getAvailableFps())
        }.build()
    }

    private fun getAvailableFps(): List<Int> {
        return listOf(1, 2, 5, 10, 15, 25, 30, 60, 75, 100, 125, 150, 200)
    }

    private val sensorEventListeners = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) {
                Timber.e("No sensor event happened")
                return
            } else {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        acceleration.apply {
                            x = event.values[0].toDouble()
                            y = event.values[1].toDouble()
                            z = event.values[2].toDouble()
                        }
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        angularVelocity.apply {
                            x = event.values[0].toDouble()
                            y = event.values[1].toDouble()
                            z = event.values[2].toDouble()
                        }
                    }
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        orientation.apply {
                            x = event.values[0].toDouble()
                            y = event.values[1].toDouble()
                            z = event.values[2].toDouble()
                            w = event.values[3].toDouble()
                        }
                    }
                    Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> {
                        rawAcceleration.apply {
                            x = event.values[0].toDouble()
                            y = event.values[1].toDouble()
                            z = event.values[2].toDouble()
                        }
                    }
                    Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> {
                        rawAngularVelocity.apply {
                            x = event.values[0].toDouble()
                            y = event.values[1].toDouble()
                            z = event.values[2].toDouble()
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        magneticField.apply {
                            x = event.values[0].toDouble()
                            y = event.values[1].toDouble()
                            z = event.values[2].toDouble()
                        }
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            Timber.d("Accuracy of ${sensor?.name} : $accuracy %")
        }
    }

    private fun getImuData(): ImuData {
        imuData.apply {
            filtered = Filtered.newBuilder().apply {
                acceleration = this@DeviceImu.acceleration.build()
                angularVelocity = this@DeviceImu.angularVelocity.build()
                orientation = this@DeviceImu.orientation.build()
            }.build()
            raw = ImuData.Raw.newBuilder().apply {
                acceleration = rawAcceleration.build()
                angularVelocity = this@DeviceImu.rawAngularVelocity.build()
                magneticFieldInMicroTesla = this@DeviceImu.magneticField.build()
            }.build()
        }
        return imuData.build()
    }

    private fun selectSensorDelay(fps: Int): Int {
        return when {
            fps <= 10 -> SensorManager.SENSOR_DELAY_UI
            fps > 10 && fps < 45 -> SensorManager.SENSOR_DELAY_GAME
            fps > 45 -> SensorManager.SENSOR_DELAY_FASTEST
            else -> SensorManager.SENSOR_DELAY_NORMAL
        }
    }

    private fun registerImu(fps: Int) {
        val sensorDelay = selectSensorDelay(fps)
        with(sensorManager) {
            getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also { magnetometer ->
                registerListener(
                    sensorEventListeners,
                    magnetometer,
                    sensorDelay
                )
            }
            getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { gyroscope ->
                registerListener(
                    sensorEventListeners,
                    gyroscope,
                    sensorDelay
                )
            }
            getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.also { linearAcc ->
                registerListener(
                    sensorEventListeners,
                    linearAcc,
                    sensorDelay
                )
            }
            getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED)?.also { magnetometer ->
                registerListener(
                    sensorEventListeners,
                    magnetometer,
                    sensorDelay
                )
            }
            getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED)?.also { gyroscope ->
                registerListener(
                    sensorEventListeners,
                    gyroscope,
                    sensorDelay
                )
            }
            getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { linearAcc ->
                registerListener(
                    sensorEventListeners,
                    linearAcc,
                    sensorDelay
                )
            }
        }
    }

    private fun unRegisterImu() {
        sensorManager.unregisterListener(sensorEventListeners)
    }

    override fun start(options: Assets.StartDeviceImu?): Result {
        if (options == null) {
            return Result(success = false, message = "Null options specified")
        }

        // Register IMU
        registerImu(options.fps)

        // Initialize and start thread
        this.publisherThread = PublisherThread(fps = options.fps)
        this.publisherThread?.start()
        return Result(success = true)

    }

    override fun stop(): Result {
        Timber.tag(TAG).i("Stopping $TAG ....")
        // Stop publishing
        this.publisherThread?.interrupt?.set(true)
        this.publisherThread?.join()
        this.publisherThread = null

        // Unregister IMU
        unRegisterImu()
        return Result(success = true, message = "")
    }

    inner class PublisherThread(val fps: Int) : Thread() {

        private val address = AddressUtils.getNamedPipeAddress(context, "device_imu")

        val interrupt = AtomicBoolean(false)

        private lateinit var publisher: Publisher

        init {
            name = "device-imu-publisher-thread"
        }

        override fun run() {
            try {
                publisher = Publisher()
                publisher.init(address)
                ZContext().use { ctx ->
//                    socket = ctx.createSocket(SocketType.PUB)
//                    socket.bind(address)
                    // wait to bind
                    Thread.sleep(500L)
                    Timber.tag(TAG).d("Publishing imu on $address")
                    val rate = Rate(hz = fps)
                    while (!interrupt.get()) {
                        try {
//                            socket.send(getImuData().toByteArray(), ZMQ.DONTWAIT)
                            publisher.publish(getImuData().toByteArray())
                            rate.sleep()
                        } catch (e: Exception) {
                            Timber.e(e)
                            return
                        }
                    }
                    publisher.close()
                }
                Timber.tag(TAG).i("Stopped publishing $TAG data")
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}
