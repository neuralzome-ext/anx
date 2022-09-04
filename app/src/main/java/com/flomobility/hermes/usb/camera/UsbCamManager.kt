package com.flomobility.hermes.usb.camera

import android.content.Context
import android.hardware.usb.UsbDevice
import com.flomobility.hermes.assets.AssetManager
import com.flomobility.hermes.usb.UsbDeviceType
import com.flomobility.hermes.usb.getDeviceType
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import com.serenegiant.usbcameracommon.CameraCallback
import com.serenegiant.usbcameracommon.UVCCameraHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbCamManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assetManager: AssetManager
) {

    // camera registry mapping usbDeviceId to port
    private val cameraRegistry = hashMapOf<Int, Int>()

    private var mUSBMonitor: USBMonitor? = null

    private val mOnDeviceConnectListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice?) {
            if (device?.getDeviceType() == UsbDeviceType.VIDEO) {
                Timber.i("[UsbCam-ATTACHED] : $device")
                val port = registerUsbCamDevice(device.deviceId)
                // TODO add usb-cam asset to asset manager
//                assetManager.addAsset()
                mUSBMonitor?.processConnect(device)
            }
        }

        override fun onDettach(device: UsbDevice?) {
            if (device?.getDeviceType() == UsbDeviceType.VIDEO) {
                Timber.i("[UsbCam-DETACHED] : $device")
                val port = unRegisterUsbCamDevice(device.deviceId)
//                assetManager.removeAsset("$port", AssetType.CAM)
            }
//            cameraRegistry[device?.deviceName]?.setState(UsbCamera.State.DISCONNECTED)
        }

        override fun onConnect(
            device: UsbDevice?,
            ctrlBlock: USBMonitor.UsbControlBlock?,
            createNew: Boolean
        ) {
            Timber.i("[UsbCam-CONNECTED] : ${device?.deviceId}")
            val handler =
                UVCCameraHandler.createHandler(null, 2, 1280, 720, UVCCamera.FRAME_FORMAT_MJPEG, 1f)
            handler?.addCallback(object : CameraCallback {
                override fun onOpen() {
                    Timber.d("Supported streams : ${handler.camera?.supportedStreams}")
                }

                override fun onClose() {
                }

                override fun onStartPreview() {
                }

                override fun onStopPreview() {
                }

                override fun onStartRecording() {
                }

                override fun onStopRecording() {
                }

                override fun onError(e: Exception?) {
                }
            })
            handler?.open(ctrlBlock)
        }

        override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
            Timber.i("[UsbCam-DISCONNECTED] : $device")
        }

        override fun onCancel(device: UsbDevice?) {
            /*NO-OP*/
        }
    }

    fun register() {
        mUSBMonitor = USBMonitor(context, mOnDeviceConnectListener)
        mUSBMonitor?.register()
    }

    fun dispose() {
        mUSBMonitor?.destroy()
        mUSBMonitor = null
    }

    /**
     * 0, 1 are occupied by device camera
     * */
    private val cameraPortsPool = (2..MAX_USB_CAMERAS).toMutableList()

    /**
     * Adds a usb device to the registry.
     *
     * @param usbDeviceId the usb device Id to register
     * @return available cam port, -1 if no ports are available
     * */
    fun registerUsbCamDevice(usbDeviceId: Int): Int {
        if (cameraPortsPool.isEmpty()) return -1
        val serialPort = cameraPortsPool[0]
        cameraPortsPool.remove(serialPort)
        cameraRegistry[usbDeviceId] = serialPort
        return serialPort
    }

    /**
     * Removes usb cam device from registry.
     * Adds the camera port back to the pool
     *
     * @param usbDeviceId the usb device Id to remove
     * @return the port added back to the pool, -1 if device not registered
     * */
    fun unRegisterUsbCamDevice(usbDeviceId: Int): Int {
        val serialPort = cameraRegistry[usbDeviceId] ?: return -1
        cameraRegistry.remove(usbDeviceId)
        cameraPortsPool.add(0, serialPort)
        cameraPortsPool.sort()
        return serialPort
    }

    companion object {
        private const val MAX_USB_CAMERAS = 100
    }
}