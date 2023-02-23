package com.flomobility.anx.assets.imu

import android.content.Context
import android.hardware.SensorManager
import com.flomobility.anx.assets.Asset
import com.flomobility.anx.common.Rate
import com.flomobility.anx.common.Result
import com.flomobility.anx.proto.Assets
import dagger.hilt.android.qualifiers.ApplicationContext
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
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

    private var publisherThread: PublisherThread? = null

    fun getDeviceImuSelect(): Assets.DeviceImuSelect {
        return Assets.DeviceImuSelect.newBuilder().apply {
            this.addAllFps(getAvailableFps())
        }.build()
    }

    private fun getAvailableFps(): List<Int> {
       return listOf(1, 2, 5, 10, 15, 25, 30, 60, 75, 100, 125, 150, 200)
    }

    private fun registerImu() {
        // TODO : add sensor listeners
    }

    private fun unRegisterImu() {
        // TODO : unregister listeners
    }

    override fun start(options: Assets.StartDeviceImu?): Result {
        if (options == null) {
            return Result(success = false, message = "Null options specified")
        }

        // Register IMU
        registerImu()

        // Initialize and start thread
        this.publisherThread = PublisherThread(fps = options.fps)
        this.publisherThread?.start()
        return Result(success = true)

    }

    override fun stop(): Result {
        // Stop publishing
        this.publisherThread?.interrupt?.set(true)
        this.publisherThread?.join()
        this.publisherThread = null

        // Unregister IMU
        unRegisterImu()
        return Result(success = false, message = "")
    }

    inner class PublisherThread(val fps: Int) : Thread() {

        private lateinit var socket: ZMQ.Socket

        private val address = "tcp://127.0.0.1:10003"

        val interrupt = AtomicBoolean(false)

        init {
            name = "device-imu-publisher-thread"
        }

        override fun run() {
            try {
                ZContext().use { ctx ->
                    socket = ctx.createSocket(SocketType.PUB)
                    socket.bind(address)
                    // wait to bind
                    Thread.sleep(500L)
                    Timber.tag(TAG).d("Publishing imu on $address")
                    val rate = Rate(hz = fps)
                    while (!interrupt.get()) {
                        try {
                            // TODO : add proto API model
    //                        socket.send(/**/, ZMQ.DONTWAIT)
                            rate.sleep()
                        } catch (e: Exception) {
                            Timber.e(e)
                            return
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}
