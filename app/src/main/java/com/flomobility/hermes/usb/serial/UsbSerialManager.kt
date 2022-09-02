package com.flomobility.hermes.usb.serial

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbSerialManager @Inject constructor() {


    private val serialPortsPool = (0..MAX_USB_SERIAL).toMutableList()

    // hashmap of serial port mapped to usb device id
    /**
     * Registry of USB Serial devices mapped to Serial port assigned
     * */
    private val serialPortRegistry = hashMapOf<Int, Int>()

    /**
     * Adds a usb device to the registry.
     *
     * @param usbDeviceId the usb device Id to register
     * @return available serial port, -1 if no ports are available
     * */
    fun registerSerialDevice(usbDeviceId: Int): Int {
        if (serialPortsPool.isEmpty()) return -1
        val serialPort = serialPortsPool[0]
        serialPortsPool.remove(serialPort)
        serialPortRegistry[usbDeviceId] = serialPort
        return serialPort
    }

    /**
     * Removes usb device from registry.
     * Adds the serial port back to the pool
     *
     * @param usbDeviceId the usb device Id to remove
     * @return the port added back to the pool, -1 if device not registered
     * */
    fun unRegisterSerialDevice(usbDeviceId: Int): Int {
        val serialPort = serialPortRegistry[usbDeviceId] ?: return -1
        serialPortRegistry.remove(usbDeviceId)
        serialPortsPool.add(0, serialPort)
        serialPortsPool.sort()
        return serialPort
    }

    companion object {
        private const val MAX_USB_SERIAL = 100
    }

}