package com.flomobility.hermes.usb_serial

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.flomobility.hermes.assets.AssetManager
import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.types.UsbSerial
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class UsbSerialPortManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assetManager: AssetManager,
    private val usbManager: UsbManager
) {

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, arg1: Intent) {
            if (arg1.action == ACTION_USB_ATTACHED) {
                val usbDevice = arg1.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                Timber.i("USB device attached $usbDevice")
                if(usbDevice == null) {
                    Timber.e("No usb device attached")
                    return
                }
                assetManager.addAsset(UsbSerial.create(usbDevice, usbManager))
            } else if (arg1.action == ACTION_USB_DETACHED) {
                val usbDevice = arg1.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                assetManager.removeAsset(usbDevice?.deviceId!!.toString(), AssetType.USB_SERIAL)
                Timber.d("$usbDevice disconnected")
            }
        }
    }

    fun init() {
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_DETACHED)
        filter.addAction(ACTION_USB_ATTACHED)
        context.registerReceiver(usbReceiver, filter)
    }

    companion object {
        const val ACTION_USB_ATTACHED = UsbManager.ACTION_USB_DEVICE_ATTACHED
        const val ACTION_USB_DETACHED = UsbManager.ACTION_USB_DEVICE_DETACHED
    }

}