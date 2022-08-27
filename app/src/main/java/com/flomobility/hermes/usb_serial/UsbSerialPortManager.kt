package com.flomobility.hermes.usb_serial

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.felhr.usbserial.SerialPortBuilder
import com.felhr.usbserial.SerialPortCallback
import com.felhr.usbserial.UsbSerialInterface
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
) {

    /*private val usbManager by lazy {
        context.getSystemService(Context.USB_SERVICE) as UsbManager
    }*/

    private val serialPortCallback = SerialPortCallback { devices ->
        Timber.d("[USB Devices] : ${devices.size}")
        devices.forEach { device ->
            Timber.d("${device.isOpen} ${device.portName} ${device.deviceId} ${device.pid} ${device.vid}")
            assetManager.addAsset(UsbSerial.create(device))
        }
    }

    private val serialPortBuilder = SerialPortBuilder.createSerialPortBuilder(serialPortCallback)

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, arg1: Intent) {
            if (arg1.action == ACTION_USB_ATTACHED) {
                val usbDevice = arg1.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                Timber.d("USB device attached $usbDevice")
                val ret: Boolean = serialPortBuilder.openSerialPorts(
                    context, DEFAULT_BAUD_RATE,
                    UsbSerialInterface.DATA_BITS_8,
                    UsbSerialInterface.STOP_BITS_1,
                    UsbSerialInterface.PARITY_NONE,
                    UsbSerialInterface.FLOW_CONTROL_OFF
                )
                if (!ret) {
                    Timber.e("Error opening device")
                }
            } else if (arg1.action == ACTION_USB_DETACHED) {
                val usbDevice = arg1.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                val ret: Boolean = serialPortBuilder.disconnectDevice(usbDevice)
                if(!ret) {
                    Timber.e("Error disconnecting $usbDevice")
                    return
                }
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
        serialPortBuilder.getSerialPorts(context)
    }

    companion object {
        const val ACTION_USB_ATTACHED = UsbManager.ACTION_USB_DEVICE_ATTACHED
        const val ACTION_USB_DETACHED = UsbManager.ACTION_USB_DEVICE_DETACHED
        const val DEFAULT_BAUD_RATE = 9600;
    }

}