package com.flomobility.hermes.usb.camera

import android.content.Context
import com.flomobility.hermes.assets.AssetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbCamManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assetManager: AssetManager
) {

    // camera registry mapping usbDeviceId to port
    private val cameraRegistry = hashMapOf<Int, Int>()

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