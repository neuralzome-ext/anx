package com.flomobility.hermes.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.flomobility.hermes.assets.AssetManager
import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.types.UsbSerial
import com.flomobility.hermes.usb.camera.UsbCamManager
import com.flomobility.hermes.usb.serial.UsbSerialManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class UsbPortManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assetManager: AssetManager,
    private val usbManager: UsbManager,
    private val usbSerialManager: UsbSerialManager,
    private val usbCamManager: UsbCamManager
) {
    private var mPermissionIntent: PendingIntent? = null
    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, intent: Intent) {
            if (intent.action == ACTION_USB_PERMISSION) {
                // when received the result of requesting USB permission
                synchronized(UsbPortManager) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // get permission, call onConnect
                            usbDeviceCallback.onAttach(device);
                        }
                    } else {
                        // failed to get permission
                        usbDeviceCallback.onDetach(device);
                    }
                }
            }
            if (intent.action == ACTION_USB_ATTACHED) {
                val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                Timber.i("USB device attached $usbDevice")
                if (usbDevice == null) {
                    Timber.e("No usb device attached")
                    return
                }
                requestPermission(usbDevice)
//                usbDeviceCallback.onAttach(usbDevice)
            } else if (intent.action == ACTION_USB_DETACHED) {
                val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (usbDevice == null) {
                    Timber.e("No usb device detached")
                    return
                }
                usbDeviceCallback.onDetach(usbDevice)
                Timber.d("$usbDevice disconnected")
            }
        }
    }

    private val usbDeviceCallback = object : UsbDeviceCallback {
        override fun onAttach(usbDevice: UsbDevice?) {
            if (usbDevice?.getDeviceType() == UsbDeviceType.SERIAL) {
                val serialPort = usbSerialManager.registerSerialDevice(usbDevice.deviceId)
                if (serialPort == -1) {
                    Timber.e("No ports available for ${usbDevice.deviceName}")
                    return
                }
                assetManager.addAsset(UsbSerial.create("$serialPort", usbDevice, usbManager))
            }
        }

        override fun onDetach(usbDevice: UsbDevice?) {
            if (usbDevice?.getDeviceType() == UsbDeviceType.SERIAL) {
                val serialPort = usbSerialManager.unRegisterSerialDevice(usbDevice.deviceId)
                if (serialPort == -1) {
                    Timber.e("Couldn't un-register ${usbDevice.deviceName}")
                    return
                }
                assetManager.removeAsset("$serialPort", AssetType.USB_SERIAL)
            }
        }
    }

    private fun attachDevices() {
        Timber.i("Looking for devices")
        val devices = usbManager.deviceList ?: return
        devices.values.sortedBy { it.deviceId }.forEach { usbDevice ->
            requestPermission(usbDevice)
//            usbDeviceCallback.onAttach(usbDevice)
        }
    }

    @Throws(IllegalStateException::class)
    fun hasPermission(device: UsbDevice?): Boolean {
//        check(!destroyed) { "already destroyed" }
        return device != null && usbManager.hasPermission(device)
//        return updatePermission(device, device != null && usbManager.hasPermission(device))
    }

    /**
     * 内部で保持しているパーミッション状態を更新
     * @param device
     * @param hasPermission
     * @return hasPermission
     */
/*
    private fun updatePermission(device: UsbDevice?, hasPermission: Boolean): Boolean {
        val deviceKey: Int = getDeviceKey(device, true)
        synchronized(mHasPermissions) {
            if (hasPermission) {
                if (mHasPermissions.get(deviceKey) == null) {
                    mHasPermissions.put(
                        deviceKey,
                        WeakReference<UsbDevice>(device)
                    )
                }
            } else {
                mHasPermissions.remove(deviceKey)
            }
        }
        return hasPermission
    }
*/

    /**
     * request permission to access to USB device
     * @param device
     * @return true if fail to request permission
     */
    @Synchronized
    fun requestPermission(device: UsbDevice?): Boolean {
//		if (DEBUG) Log.v(TAG, "requestPermission:device=" + device);
        var result = false
        if (mPermissionIntent != null) {
            if (device != null) {
                if (usbManager.hasPermission(device)) {
                    // call onConnect if app already has permission
                    usbDeviceCallback.onAttach(device)
                } else {
                    try {
                        // パーミッションがなければ要求する
                        usbManager.requestPermission(device, mPermissionIntent)
                    } catch (e: Exception) {
                        // Android5.1.xのGALAXY系でandroid.permission.sec.MDM_APP_MGMTという意味不明の例外生成するみたい
//                        Log.w(TAG, e)
                        usbDeviceCallback.onDetach(device)
                        result = true
                    }
                }
            } else {
                usbDeviceCallback.onDetach(device)
                result = true
            }
        } else {
            usbDeviceCallback.onDetach(device)
            result = true
        }
        return result
    }

    fun init() {
        mPermissionIntent =
            PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_IMMUTABLE else 0);
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(ACTION_USB_DETACHED)
        filter.addAction(ACTION_USB_ATTACHED)
        context.registerReceiver(usbReceiver, filter)

        usbCamManager.register()

        attachDevices()
    }

    companion object {
        private const val ACTION_USB_ATTACHED = UsbManager.ACTION_USB_DEVICE_ATTACHED
        private const val ACTION_USB_DETACHED = UsbManager.ACTION_USB_DEVICE_DETACHED
        private const val ACTION_USB_PERMISSION_BASE = "com.flomobility.hermes.USB_PERMISSION."
        val ACTION_USB_PERMISSION = ACTION_USB_PERMISSION_BASE + hashCode()
    }

}