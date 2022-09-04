package com.flomobility.hermes.assets.types.camera

import android.hardware.usb.UsbDevice
import com.flomobility.hermes.assets.AssetState
import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.BaseAssetConfig
import com.flomobility.hermes.common.Result
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import com.serenegiant.usbcameracommon.CameraCallback
import com.serenegiant.usbcameracommon.UVCCameraHandler
import timber.log.Timber
import java.nio.ByteBuffer

class UsbCamera : Camera() {

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
        TODO("Not yet implemented")
    }

    override fun stop(): Result {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }

    private var camThread: UVCCameraHandler? = null

    private val callbacks = mutableListOf<FrameCallback>()

    fun registerCallback(cb: FrameCallback) {
        callbacks.add(cb)
    }

    fun unRegisterCallback(cb: FrameCallback) {
        callbacks.remove(cb)
    }

    fun setCameraThread(cameraHandler: UVCCameraHandler) {
        this.camThread = cameraHandler
        this.camThread?.addCallback(object : CameraCallback {
            override fun onOpen() {
                this@UsbCamera.camThread?.camera?.setFrameCallback({ byteBuffer ->
                    callbacks.forEach { cb ->
                        cb.onFrame(byteBuffer)
                    }
                }, UVCCamera.PIXEL_FORMAT_NV21)
            }

            override fun onClose() {
//                this@UsbCamera.state = State.IDLE
            }

            override fun onStartPreview() {
//                this@UsbCamera.state = State.STREAMING
            }

            override fun onStopPreview() {
//                this@UsbCamera.state = State.IDLE
            }

            override fun onStartRecording() {
                /*NO-OP*/
            }

            override fun onStopRecording() {
                /*NO-OP*/
            }

            override fun onError(e: Exception?) {
                Timber.e(e)
            }
        })
    }

    fun initNew(metaInfo: MetaInfo) {
        /*this.metaInfo = metaInfo
        this.metaInfo?.let { meta ->
            val handler = UVCCameraHandler.createHandler(
                null,
                2,
                meta.width,
                meta.height,
                UVCCamera.FRAME_FORMAT_MJPEG,
                1f
            )
            setCameraThread(handler)
            Timber.d("Init done")
        }*/
    }

    fun startStream() {
        /*Timber.d("Starting stream")
        this.camThread?.open(this.metaInfo?.ctrlBlock)
        this.camThread?.startPreview()*/
    }

    fun close() {
        this.camThread?.close()
    }

    data class MetaInfo(
        val device: UsbDevice?,
        val ctrlBlock: USBMonitor.UsbControlBlock?,
        val width: Int = DEFAULT_WIDTH,
        val height: Int = DEFAULT_HEIGHT,
        val fps: Int = DEFAULT_FPS
    ) {
        companion object {
            const val DEFAULT_WIDTH = 1280
            const val DEFAULT_HEIGHT = 720
            const val DEFAULT_FPS = 30
        }
    }

    interface FrameCallback {
        fun onFrame(byteBuffer: ByteBuffer)
        fun unRegister()
    }

}