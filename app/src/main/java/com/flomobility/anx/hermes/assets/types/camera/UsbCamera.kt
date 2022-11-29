package com.flomobility.anx.hermes.assets.types.camera

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.flomobility.anx.hermes.assets.AssetState
import com.flomobility.anx.hermes.assets.AssetType
import com.flomobility.anx.hermes.assets.BaseAssetConfig
import com.flomobility.anx.hermes.common.Result
import com.flomobility.anx.hermes.other.Constants
import com.flomobility.anx.hermes.other.Constants.SOCKET_BIND_DELAY_IN_MS
import com.flomobility.anx.hermes.other.handleExceptions
import com.flomobility.anx.hermes.other.toJpeg
import com.serenegiant.usb.UVCCamera
import com.serenegiant.usbcameracommon.CameraCallback
import com.serenegiant.usbcameracommon.UVCCameraHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import java.nio.ByteBuffer
import kotlin.Exception

class UsbCamera : Camera() {

    private var _id: String = ""

    private val _config = Config()

    override val id: String
        get() = _id

    override val type: AssetType
        get() = AssetType.CAM

    override val config: BaseAssetConfig
        get() = _config

    private var camThread: UVCCameraHandler? = null

    private var streamingThread: StreamingThread? = null

    private val callbacks = mutableListOf<FrameCallback>()

    private var shouldCompress = true


    object Builder {
        fun createNew(id: String): UsbCamera {
            return UsbCamera().apply {
                this._id = id
                this.streamingThread = StreamingThread()
                this.streamingThread?.start()
            }
        }
    }

    private val cameraCallback = object : CameraCallback {
        override fun onOpen() {
            Timber.i("[$name] - Closing asset")
        }

        override fun onClose() {
            Timber.i("[$name] - Closing asset")
        }

        override fun onStartPreview() {
            Timber.i("[$name] - Started preview")
            this@UsbCamera.camThread?.camera?.setFrameCallback({ byteBuffer ->
                callbacks.forEach { cb ->
                    cb.onFrame(byteBuffer)
                }
            }, UVCCamera.PIXEL_FORMAT_JPEG)
        }

        override fun onStopPreview() {
            Timber.i("[$name] - Stopped preview")
        }

        override fun onStartRecording() {
            /*NO-OP*/
        }

        override fun onStopRecording() {
            /*NO-OP*/
        }

        override fun onError(e: Exception?) {
            Timber.e("[$name] : $e")
        }
    }

    private val frameCallback = object : FrameCallback {
        override fun onFrame(byteBuffer: ByteBuffer) {
            if (debug) {
                CoroutineScope(dispatcher).launch(dispatcher) {
                    cameraOut.send(byteBuffer)
                }
            }
            streamingThread?.publishFrame(byteBuffer)
        }
    }

    override fun updateConfig(config: BaseAssetConfig): Result {
        if (config !is Config) {
            return Result(success = false, message = "unknown config type")
        }
        this._config.apply {
            stream.value = config.stream.value
            compressionQuality.value = config.compressionQuality.value
            portPub = config.portPub
            portSub = config.portSub
            connectedDeviceIp = config.connectedDeviceIp
        }
        shouldCompress =
            (this._config.stream.value as Config.Stream).pixelFormat == Config.Stream.PixelFormat.MJPEG
        return Result(success = true)
    }

    override fun start(): Result {
        handleExceptions(catchBlock = { e ->
            updateState(AssetState.IDLE)
            Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            updateState(AssetState.STREAMING)
            val stream = _config.stream.value
            streamingThread?.updateAddress()
            registerCallback(frameCallback)
            camThread?.setStreamingParams(
                stream.width,
                stream.height,
                stream.fps,
                stream.pixelFormat.uvcCode,
                1f
            )
            camThread?.addCallback(this.cameraCallback)
            camThread?.startPreview()
            return Result(success = true)
        }
        return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
    }

    override fun stop(): Result {
        handleExceptions(catchBlock = { e ->
            updateState(AssetState.STREAMING)
            Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            updateState(AssetState.IDLE)
            camThread?.stopPreview()
            unRegisterCallback(frameCallback)
            return Result(success = true)
        }
        return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
    }

    override fun destroy() {
        streamingThread?.kill()
        camThread?.close()
        camThread = null
        streamingThread = null
    }

    fun registerCallback(cb: FrameCallback) {
        callbacks.add(cb)
    }

    fun unRegisterCallback(cb: FrameCallback) {
        callbacks.remove(cb)
    }

    fun setCameraThread(cameraHandler: UVCCameraHandler) {
        this.camThread = cameraHandler
    }

    inner class StreamingThread : Thread() {

        init {
            name = "${this@UsbCamera.name}-streaming-thread"
        }

        private var handler: StreamingThreadHandler? = null

        private lateinit var socket: ZMQ.Socket

        private var address = ""

        fun updateAddress() {
            address = "tcp://*:${_config.portPub}"
        }

        override fun run() {
            while (address.isEmpty()) {
                continue
            }
            Timber.i("[${this@UsbCamera.name}] - Starting Publisher on $address")
            try {
                ZContext().use { ctx ->
                    socket = ctx.createSocket(SocketType.PUB)
                    socket.bind(address)
                    sleep(SOCKET_BIND_DELAY_IN_MS)
                    Looper.prepare()
                    handler = StreamingThreadHandler(Looper.myLooper() ?: return)
                    Looper.loop()
                }
                socket.unbind(address)
                sleep(SOCKET_BIND_DELAY_IN_MS)
                Timber.i("[${this@UsbCamera.name}] - Stopping Publisher on $address")
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        fun publishFrame(frame: ByteBuffer) {
            handler?.sendMsg(MSG_STREAM_FRAME, frame)
        }

        fun kill() {
            handler?.sendMsg(Constants.SIG_KILL_THREAD)
        }

        inner class StreamingThreadHandler(private val myLooper: Looper) : Handler(myLooper) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_STREAM_FRAME -> {
//                        val elapsed = measureTimeMillis {
                            val frame = msg.obj as ByteBuffer
                            socket.sendByteBuffer(frame, ZMQ.DONTWAIT)
//                        }
//                        Timber.d("$elapsed")
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

    companion object {
        private const val MSG_STREAM_FRAME = 9
    }

    interface FrameCallback {
        fun onFrame(byteBuffer: ByteBuffer)
    }

}
