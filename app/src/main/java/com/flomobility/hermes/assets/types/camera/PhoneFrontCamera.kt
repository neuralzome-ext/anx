package com.flomobility.hermes.assets.types.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.quirk.CamcorderProfileResolutionQuirk
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.ProcessLifecycleOwner
import com.flomobility.hermes.assets.AssetState
import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.BaseAssetConfig
import com.flomobility.hermes.common.Result
import com.flomobility.hermes.other.Constants
import com.flomobility.hermes.other.Constants.SOCKET_BIND_DELAY_IN_MS
import com.flomobility.hermes.other.handleExceptions
import com.flomobility.hermes.other.toJpeg
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("RestrictedApi", "UnsafeOptInUsageError", "VisibleForTests")
@Singleton
class PhoneFrontCamera @Inject constructor(
    @ApplicationContext private val context: Context
) : Camera() {

    private var _id: String = "1"

    private val _config = Config()

    override val id: String
        get() = _id

    override val type: AssetType
        get() = AssetType.CAM

    override val config: BaseAssetConfig
        get() = _config

    private var streamingThread: StreamingThread? = null

    private var shouldCompress = true

    var frameCounter = 0
    var lastFpsTimestamp = System.currentTimeMillis()

    /**
     * CameraX Related Initialisation
     */
    private val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    private val imageAnalysisBuilder = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

    private val cameraProvider = cameraProviderFuture.get()

    private val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
        .build()

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var camera: androidx.camera.core.Camera

    init {
        GlobalScope.launch(Dispatchers.Main) {
            if(!canRegister()) return@launch
            camera = cameraProvider.bindToLifecycle(
                ProcessLifecycleOwner.get(),
                cameraSelector
            )
            val characteristics = CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
                Camera2CameraInfo.extractCameraCharacteristics(camera.cameraInfo)
            )
            val previewSizes = CamcorderProfileResolutionQuirk(characteristics).supportedResolutions
//            Timber.d("Preview sizes : $previewSizes")
            val streams = mutableListOf<Config.Stream>()
            streams.add(
                Config.Stream(
                    fps = 30,
                    width = 640,
                    height = 480,
                    pixelFormat = Config.Stream.PixelFormat.MJPEG
                )
            )
/*            listOf(1, 2, 5, 10, 15, 30).forEach { fps ->
                previewSizes.forEach { size ->
                    streams.add(
                        Config.Stream(
                            fps = fps,
                            width = size.width,
                            height = size.height,
                            pixelFormat = Config.Stream.PixelFormat.MJPEG
                        )
                    )
                }
            }*/
            this@PhoneFrontCamera._config.loadStreams(streams)
            cameraProvider.unbindAll()
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

    override fun canRegister(): Boolean {
        if(!cameraProvider.hasCamera(cameraSelector)) {
            Timber.e("Front camera not present")
            return false
        }
        return true
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun start(): Result {
        handleExceptions(catchBlock = { e ->
            updateState(AssetState.IDLE)
            Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            updateState(AssetState.STREAMING)
            streamingThread = StreamingThread()
            streamingThread?.start()
            streamingThread?.updateAddress()

            GlobalScope.launch(Dispatchers.Main) {

                cameraProvider.unbindAll()
                val imageAnalysis = imageAnalysisBuilder
                    /*.setTargetResolution(
                        when (context.resources.configuration.orientation) {
                            Configuration.ORIENTATION_PORTRAIT -> Size(
                                _config.stream.value.height,
                                _config.stream.value.width
                            )
                            Configuration.ORIENTATION_LANDSCAPE -> Size(
                                _config.stream.value.width,
                                _config.stream.value.height
                            )
                            else -> Size(_config.stream.value.width, _config.stream.value.height)
                        }
                    )*/
                    .build()

                camera = cameraProvider.bindToLifecycle(
                    ProcessLifecycleOwner.get(),
                    cameraSelector,
                    imageAnalysis
                )

                val cameraControl = camera.cameraControl
                val camera2CameraControl = Camera2CameraControl.from(cameraControl)

                val captureRequestOptions = CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CameraMetadata.CONTROL_AF_MODE_OFF,
                    )
                    /*.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        Range(_config.stream.value.fps, _config.stream.value.fps)
                    )*/
                    .build()

                camera2CameraControl.captureRequestOptions = captureRequestOptions

                cameraPreviewCallBack(imageAnalysis)
            }
            return Result(success = true)
        }
        return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
    }

    private fun cameraPreviewCallBack(imageAnalysis: ImageAnalysis) {
        cameraProviderFuture.addListener(Runnable {
            val frameCount = 30
            imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
                if (++frameCounter % frameCount == 0) {
                    frameCounter = 0
                    val now = System.currentTimeMillis()
                    val delta = now - lastFpsTimestamp
                    val fps = 1000 * frameCount.toFloat() / delta
                    Timber.d("FPS: ${"%.02f".format(fps)}")
                    lastFpsTimestamp = now
                }
                image.use {
                    try {
//                        val imageBuffer = image.image?.planes?.toNV21(image.width, image.height)
                        streamingThread?.publishFrame(
                            image.toJpeg() ?: throw Throwable("Couldn't get JPEG image")
                        )
                    } catch (t: Throwable) {
                        Timber.e("Error in getting Img : ${t.message}")
                    }
                }
            })
        }, executor)
    }

    override fun stop(): Result {
        handleExceptions(catchBlock = { e ->
            updateState(AssetState.STREAMING)
            Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            updateState(AssetState.IDLE)
            GlobalScope.launch(Dispatchers.Main) {
                cameraProvider.unbindAll()
            }
            streamingThread?.kill()
            streamingThread = null
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
            name = "${this@PhoneFrontCamera.name}-streaming-thread"
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
            Timber.i("[${this@PhoneFrontCamera.name}] - Starting Publisher on $address")
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
                Timber.i("[${this@PhoneFrontCamera.name}] - Stopping Publisher on $address")
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
                        socket.sendByteBuffer(msg.obj as ByteBuffer, 0)
//                        }
//                        Timber.d("$elapsed")
                    }
                    MSG_STREAM_FRAME_BYTE_ARRAY -> {
                        val frame = msg.obj as ByteArray
                        socket.send(frame, ZMQ.DONTWAIT)
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
        private const val MSG_STREAM_FRAME_BYTE_ARRAY = 10
    }


}