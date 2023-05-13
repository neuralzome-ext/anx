package com.flomobility.anx.assets.camera

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.flomobility.anx.assets.Asset
import com.flomobility.anx.common.Result
import com.flomobility.anx.native.NativeCamera
import com.flomobility.anx.proto.Assets
import com.flomobility.anx.utils.AddressUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCamera @Inject constructor(
    @ApplicationContext private val context: Context
) : Asset<Assets.StartDeviceCamera>() {

    companion object {
        private const val TAG = "DeviceCamera"

        private const val MSG_CREATE_CAMERA = 1001
        private const val MSG_START_CAMERA = 1002
        private const val MSG_STOP_CAMERA = 1003
    }

    private var cameraThread: CameraThread? = null

    private var isRunning = false

    private var cameraPtr = 0L

    fun getDeviceCameraSelect(): Assets.DeviceCameraSelect {
        return Assets.DeviceCameraSelect.parseFrom(NativeCamera.getStreams())
    }

    fun init() {
        cameraThread = CameraThread()
        cameraThread?.start()

        Thread.sleep(1000L)
        cameraThread?.sendMsg(MSG_CREATE_CAMERA)
    }

    override fun start(options: Assets.StartDeviceCamera?): Result {
        if (options == null) {
            return Result(success = false, message = "Null options specified")
        }

        if (cameraPtr == 0L) {
            // Initialize and start thread
            cameraThread?.sendMsg(MSG_CREATE_CAMERA)
        }

        cameraThread?.sendMsg(MSG_START_CAMERA, options)
        return Result(success = true)

    }

    override fun stop(): Result {
        Timber.tag(TAG).i("Stopping $TAG ....")

        cameraThread?.sendMsg(MSG_STOP_CAMERA)
        return Result(success = true, message = "")
    }

    inner class CameraThread : Thread() {

        init {
            name = "device-camera-handler-thread"
        }

        private var handler: CameraThreadHandler? = null

        override fun run() {
            Looper.prepare()
            handler = CameraThreadHandler(Looper.myLooper() ?: return)
            Looper.loop()
        }

        inner class CameraThreadHandler(private val myLooper: Looper) : Handler(myLooper) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_CREATE_CAMERA -> {
                        Timber.tag(TAG).i("Creating camera")
                        cameraPtr = NativeCamera.initCam(
                            AddressUtils.getNamedPipeAddress(
                                context,
                                "device_camera"
                            )
                        )
                    }
                    MSG_START_CAMERA -> {
                        val options = msg.obj as Assets.StartDeviceCamera
                        Timber.tag(TAG).i("Starting camera")
                        NativeCamera.startCam(options.toByteArray())
                        isRunning = true
                    }
                    MSG_STOP_CAMERA -> {
                        if (!isRunning) return
                        NativeCamera.stopCam()
                        Timber.tag(TAG).i("Stopped Camera")
                        isRunning = false
                    }
                }
            }
        }

        fun sendMsg(what: Int, obj: Any? = null) {
            handler?.sendMessage(handler?.obtainMessage(what, obj) ?: return)
        }
    }

}
