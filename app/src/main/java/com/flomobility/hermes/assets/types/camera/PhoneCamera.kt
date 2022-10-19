package com.flomobility.hermes.assets.types.camera

import android.annotation.SuppressLint
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.flomobility.hermes.HermesApplication
import com.flomobility.hermes.assets.AssetState
import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.BaseAssetConfig
import com.flomobility.hermes.common.Result
import com.flomobility.hermes.other.Constants
import com.flomobility.hermes.other.Constants.SOCKET_BIND_DELAY_IN_MS
import com.flomobility.hermes.other.handleExceptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.Exception

class PhoneCamera : Camera() {

    private var _id: String = ""

    private val _config = Config()

    private var _state = AssetState.IDLE

    override val id: String
        get() = _id

    override val type: AssetType
        get() = AssetType.CAM

    override val config: BaseAssetConfig
        get() = _config

    override val state: AssetState
        get() = _state

    private var streamingThread: StreamingThread? = null

    private var shouldCompress = true

    /**
     * CameraX Related Initialisation
     */
    private val cameraProviderFuture = ProcessCameraProvider.getInstance(HermesApplication.appContext)

    private val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    private val cameraProvider = cameraProviderFuture.get()

    private val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var camera: androidx.camera.core.Camera


    object Builder {
        fun createNew(id: String): PhoneCamera {
            return PhoneCamera().apply {
                this._id = id
                this.streamingThread = StreamingThread()
                this.streamingThread?.start()
            }
        }
    }

    override fun updateConfig(config: BaseAssetConfig): Result {
        if (config !is Camera.Config) {
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

    @SuppressLint("UnsafeOptInUsageError")
    override fun start(): Result {
        handleExceptions(catchBlock = { e ->
            _state = AssetState.IDLE
            Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            _state = AssetState.STREAMING
            val stream = _config.stream.value
            streamingThread?.updateAddress()

            GlobalScope.launch(Dispatchers.Main) {

                camera = cameraProvider.bindToLifecycle(
                    HermesApplication.appContext as LifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                )

                val cameraControl = camera.cameraControl
                val camera2CameraControl = Camera2CameraControl.from(cameraControl)

                val captureRequestOptions = CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CameraMetadata.CONTROL_AF_MODE_OFF
                    )
                    .build()

                camera2CameraControl.captureRequestOptions = captureRequestOptions

                cameraPreviewCallBack()
            }
            return Result(success = true)
        }
        return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
    }

    private fun cameraPreviewCallBack(){
        cameraProviderFuture.addListener(Runnable {
            imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
                Timber.i("cameraPreview - Phone Camera CallBack")
                val bytes = ByteArray(image.planes[0].buffer.remaining())
                val byteBuffer = image.planes[0].buffer[bytes]
                streamingThread?.publishFrame(byteBuffer)
                image.close()
            })

        }, executor)
    }

    override fun stop(): Result {
        handleExceptions(catchBlock = { e ->
            _state = AssetState.STREAMING
            Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            _state = AssetState.IDLE
            return Result(success = true)
        }
        return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
    }

    override fun destroy() {
        streamingThread?.kill()
        streamingThread = null
    }


    inner class StreamingThread : Thread() {

        init {
            name = "${this@PhoneCamera.name}-streaming-thread"
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
            Timber.i("[${this@PhoneCamera.name}] - Starting Publisher on $address")
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
                Timber.i("[${this@PhoneCamera.name}] - Stopping Publisher on $address")
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

}