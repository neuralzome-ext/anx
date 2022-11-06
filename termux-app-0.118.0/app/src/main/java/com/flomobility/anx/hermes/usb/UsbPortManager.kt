package com.flomobility.anx.hermes.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.flomobility.anx.hermes.assets.AssetManager
import com.flomobility.anx.hermes.assets.AssetType
import com.flomobility.anx.hermes.assets.types.UsbSerial
import com.flomobility.anx.hermes.assets.types.camera.Camera
import com.flomobility.anx.hermes.assets.types.camera.UsbCamera
import com.flomobility.anx.hermes.usb.camera.UsbCamManager
import com.flomobility.anx.hermes.usb.serial.UsbSerialManager
import com.serenegiant.usb.UsbControlBlock
import com.serenegiant.usbcameracommon.CameraCallback
import com.serenegiant.usbcameracommon.UVCCameraHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
@RequiresApi(Build.VERSION_CODES.M)
class UsbPortManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assetManager: AssetManager,
    private val usbManager: UsbManager,
    private val usbSerialManager: UsbSerialManager,
    private val usbCamManager: UsbCamManager
) {

    private val unAuthorizedDevices = hashMapOf<Int, SecureUsbDevice>()

    private var usbChecker: UsbCheckerThread? = null

    private var mPermissionIntent: PendingIntent? = null

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(arg0: Context, intent: Intent) {
            if (intent.action == ACTION_USB_PERMISSION) {
                // when received the result of requesting USB permission
                synchronized(UsbPortManager) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            unAuthorizedDevices[device.deviceId]?.isAuth = true
                            registerUsbDevice(device)
                        }
                    } else {
                        Timber.w("Permission not granted for $device")
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

                unAuthorizedDevices[usbDevice.deviceId] = SecureUsbDevice(usbDevice, false)

            } else if (intent.action == ACTION_USB_DETACHED) {
                val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (usbDevice == null) {
                    Timber.e("No usb device detached")
                    return
                }

                handleUsbDetached(usbDevice)
                Timber.d("$usbDevice disconnected")
            }
        }
    }

    private fun handleUsbDetached(usbDevice: UsbDevice) {
        when (usbDevice.getDeviceType()) {
            UsbDeviceType.VIDEO -> {
                val port = usbCamManager.unRegisterUsbCamDevice(usbDevice.deviceId)
                if (port == -1) {
                    Timber.e("Couldn't un-register ${usbDevice.deviceName}")
                    return
                }
                assetManager.removeAsset("$port", AssetType.CAM)
            }
            UsbDeviceType.SERIAL -> {
                val serialPort = usbSerialManager.unRegisterSerialDevice(usbDevice.deviceId)
                if (serialPort == -1) {
                    Timber.e("Couldn't un-register ${usbDevice.deviceName}")
                    return
                }
                assetManager.removeAsset("$serialPort", AssetType.USB_SERIAL)
            }
            UsbDeviceType.UNK -> {
                Timber.e("Cannot un-register unknown usb device - ${usbDevice.deviceId} as asset")
            }
        }
    }

    private fun registerUsbDevice(usbDevice: UsbDevice) {
        when (usbDevice.getDeviceType()) {
            UsbDeviceType.VIDEO -> {
                val port = usbCamManager.registerUsbCamDevice(usbDevice.deviceId)
                val usbCam = UsbCamera.Builder.createNew("$port")
                val handler =
                    UVCCameraHandler.createHandler(2)
                usbCam.setCameraThread(handler)
                handler?.addCallback(object : CameraCallback {
                    override fun onOpen() {
                        val supportedStreams = handler.camera?.supportedStreams ?: return
                        (usbCam.config as Camera.Config).loadStreams(
                            Camera.Config.toStreamList(supportedStreams)
                        )
                        assetManager.addAsset(usbCam)
                    }

                    override fun onClose() {}

                    override fun onStartPreview() {}

                    override fun onStopPreview() {}

                    override fun onStartRecording() {}

                    override fun onStopRecording() {}

                    override fun onError(e: Exception?) {}
                })
                handler?.open(UsbControlBlock(usbManager, usbDevice))
            }
            UsbDeviceType.SERIAL -> {
                val serialPort = usbSerialManager.registerSerialDevice(usbDevice.deviceId)
                if (serialPort == -1) {
                    Timber.e("No ports available for ${usbDevice.deviceName}")
                    return
                }
                assetManager.addAsset(UsbSerial.create("$serialPort", usbDevice, usbManager))
            }
            UsbDeviceType.UNK -> {
                Timber.e("Cannot register unknown usb device - ${usbDevice.deviceId} as asset")
            }
        }
    }


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
                usbManager.requestPermission(device, mPermissionIntent)
                result = false
            }
        } else {
            result = true
        }
        return result
    }

    fun init() {
        mPermissionIntent =
            PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), 0);
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(ACTION_USB_DETACHED)
        filter.addAction(ACTION_USB_ATTACHED)
        context.registerReceiver(usbReceiver, filter)

        usbChecker = UsbCheckerThread()
        usbChecker?.start()
    }

    inner class UsbCheckerThread : Thread() {

        init {
            name = "usb-checker-thread"
        }

        override fun run() {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    val keys = mutableSetOf<Int>()
                    keys.addAll(unAuthorizedDevices.keys)
                    for (key in keys) {
                        val secureDevice =
                            unAuthorizedDevices[key] ?: throw Throwable("Null secure device")
                        if (secureDevice.isAuth) {
                            unAuthorizedDevices.remove(key)
                            continue
                        }

                        requestPermission(secureDevice.usbDevice)
                        while (!secureDevice.isAuth) {
                            // Wait till permission is granted
                            continue
                        }
                        unAuthorizedDevices.remove(key)
                        Timber.i("Permission granted for $secureDevice")
                    }
                    sleep(1000L)
                }
            } catch (t: Throwable) {
                Timber.e(t)
            }
        }

    }

    data class SecureUsbDevice(
        val usbDevice: UsbDevice,
        var isAuth: Boolean
    )

    companion object {
        private const val ACTION_USB_ATTACHED = UsbManager.ACTION_USB_DEVICE_ATTACHED
        private const val ACTION_USB_DETACHED = UsbManager.ACTION_USB_DEVICE_DETACHED
        private const val ACTION_USB_PERMISSION_BASE = "com.flomobility.anx.hermes.USB_PERMISSION."
        val ACTION_USB_PERMISSION = ACTION_USB_PERMISSION_BASE + hashCode()
    }

}
