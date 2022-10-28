package com.termux.hermes.usb.camera

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.termux.hermes.assets.AssetManager
import com.termux.hermes.assets.AssetType
import com.termux.hermes.assets.types.camera.Camera
import com.termux.hermes.assets.types.camera.UsbCamera
import com.termux.hermes.usb.UsbDeviceType
import com.termux.hermes.usb.UsbPortManager
import com.termux.hermes.usb.getDeviceType
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usbcameracommon.CameraCallback
import com.serenegiant.usbcameracommon.UVCCameraHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
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

    private var deviceMutex = ReentrantLock(true)

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, intent: Intent) {
            if (intent.action == UsbPortManager.ACTION_USB_PERMISSION) {
                // when received the result of requesting USB permission
//                synchronized(UsbPortManager) {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        // get permission, call onConnect
                        mOnDeviceConnectListener.onAttach(device);
                    }
                } else {
                    // failed to get permission
                    mOnDeviceConnectListener.onDettach(device);
                }
//                }
            }
        }
    }
    private val mOnDeviceConnectListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice?) {
            if(device?.getDeviceType() == UsbDeviceType.VIDEO) {
                if (mUSBMonitor?.hasPermission(device) == true)
                    processAttach(device)
                else
                    mUSBMonitor?.requestPermission(device)
            }
        }

        override fun onDettach(device: UsbDevice?) {
            if(device?.getDeviceType() == UsbDeviceType.VIDEO) {
                if (mUSBMonitor?.hasPermission(device) == true)
                    processDetach(device)
            }
        }

        override fun onConnect(
            device: UsbDevice?,
            ctrlBlock: USBMonitor.UsbControlBlock?,
            createNew: Boolean
        ) {
            Timber.i("[UsbCam-CONNECTED] : ${device?.deviceId}")

        }

        override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
            Timber.i("[UsbCam-DISCONNECTED] : $device")
        }

        override fun onCancel(device: UsbDevice?) {
            /*NO-OP*/
        }
    }

    fun processAttach(device: UsbDevice?) {
        if (device?.getDeviceType() == UsbDeviceType.VIDEO) {
            Timber.i("[UsbCam-ATTACHED] : $device")
            val port = registerUsbCamDevice(device.deviceId)
            /*deviceMutex.lock()
            assetManager.addAsset(UsbCamera.Builder.createNew("$port"))
            deviceMutex.unlock()
            mUSBMonitor?.processConnect(device)*/
        }
    }

    fun processDetach(device: UsbDevice?) {
        if (device?.getDeviceType() == UsbDeviceType.VIDEO) {
            Timber.i("[UsbCam-DETACHED] : $device")
            val port = unRegisterUsbCamDevice(device.deviceId)
            assetManager.removeAsset("$port", AssetType.CAM)
        }
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
