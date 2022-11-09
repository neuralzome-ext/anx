package com.flomobility.anx.hermes.usb

import android.hardware.usb.UsbDevice

interface UsbDeviceCallback {
    fun onAttach(usbDevice: UsbDevice?)
    fun onDetach(usbDevice: UsbDevice?)
}