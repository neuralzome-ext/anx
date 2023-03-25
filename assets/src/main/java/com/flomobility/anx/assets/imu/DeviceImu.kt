package com.flomobility.anx.assets.imu

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.flomobility.anx.assets.Asset
import com.flomobility.anx.common.Result
import com.flomobility.anx.native.NativeSensors
import com.flomobility.anx.proto.Assets
import com.flomobility.anx.utils.AddressUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceImu @Inject constructor(
    @ApplicationContext private val context: Context
) : Asset<Assets.StartDeviceImu>() {

    companion object {
        private const val TAG = "DeviceImu"

        private const val MSG_CREATE_IMU = 1001
        private const val MSG_START_IMU = 1002
        private const val MSG_STOP_IMU = 1003
    }

    private var imuThread: ImuThread? = null

    fun getDeviceImuSelect(): Assets.DeviceImuSelect {
        return Assets.DeviceImuSelect.newBuilder().apply {
            this.addAllFps(getAvailableFps())
        }.build()
    }

    private fun getAvailableFps(): List<Int> {
        return listOf(1, 2, 5, 10, 15, 25, 30, 60, 75, 100, 125, 150, 200)
    }

    fun init() {
        imuThread = ImuThread()
        imuThread?.start()

    }

    override fun start(options: Assets.StartDeviceImu?): Result {
        if (options == null) {
            return Result(success = false, message = "Null options specified")
        }

        // Initialize and start thread
        imuThread?.sendMsg(MSG_CREATE_IMU, options)
        imuThread?.sendMsg(MSG_START_IMU)
        return Result(success = true)

    }

    override fun stop(): Result {
        Timber.tag(TAG).i("Stopping $TAG ....")

        imuThread?.sendMsg(MSG_STOP_IMU)
        return Result(success = true, message = "")
    }

    inner class ImuThread: Thread() {

        init {
            name = "device-imu-handler-thread"
        }

        private var handler: ImuThreadHandler? = null

        override fun run() {
            Looper.prepare()
            handler = ImuThreadHandler(Looper.myLooper() ?: return)
            Looper.loop()
        }

        inner class ImuThreadHandler(private val myLooper: Looper): Handler(myLooper) {
            override fun handleMessage(msg: Message) {
                when(msg.what) {
                    MSG_CREATE_IMU -> {
                        val options = msg.obj as Assets.StartDeviceImu
                        NativeSensors.initImu(options.fps, AddressUtils.getNamedPipeAddress(context, "device_imu"))
                    }
                    MSG_START_IMU -> {
                        NativeSensors.startImu()
                    }
                    MSG_STOP_IMU -> {
                        NativeSensors.stopImu()
                        Timber.tag(TAG).i("Stopped IMU")
                    }
                }
            }
        }

        fun sendMsg(what: Int, obj: Any? = null) {
            handler?.sendMessage(handler?.obtainMessage(what, obj) ?: return)
        }
    }

}
