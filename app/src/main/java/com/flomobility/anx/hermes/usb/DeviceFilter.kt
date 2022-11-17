package com.flomobility.anx.hermes.usb

import android.hardware.usb.UsbDevice

fun UsbDevice.getDeviceType(): UsbDeviceType {
    val interfacesCount = this.interfaceCount
    for (i in 0..interfacesCount) {
        val usbInterface = getInterface(i)
        return when (usbInterface.interfaceClass) {
            in UsbDeviceType.VIDEO.codes -> {
                UsbDeviceType.VIDEO
            }
            in UsbDeviceType.SERIAL.codes -> {
                UsbDeviceType.SERIAL
            }
            else -> {
                UsbDeviceType.UNK
            }
        }
    }
    return UsbDeviceType.UNK
}

enum class UsbDeviceType(val codes: List<Int>) {
    VIDEO(listOf(14)), SERIAL(listOf(2, 10, 255)), UNK(listOf(-1))
}