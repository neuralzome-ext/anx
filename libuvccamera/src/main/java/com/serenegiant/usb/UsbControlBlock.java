package com.serenegiant.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.RequiresApi;

import com.serenegiant.utils.BuildCheck;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.Locale;

@RequiresApi(Build.VERSION_CODES.M)
public class UsbControlBlock {

    private final boolean DEBUG = false;
    private static final String TAG = "UsbControlBlock";
    private final WeakReference<UsbDevice> mWeakDevice;
    private final WeakReference<UsbManager> mUsbManager;
    protected UsbDeviceConnection mConnection;
    protected final UsbDeviceInfo mInfo;
    private final int mBusNum;
    private final int mDevNum;
    private final SparseArray<SparseArray<UsbInterface>> mInterfaces = new SparseArray<SparseArray<UsbInterface>>();

    public UsbControlBlock(final UsbManager usbManager, final UsbDevice device) {
        mUsbManager = new WeakReference<UsbManager>(usbManager);
        mWeakDevice = new WeakReference<UsbDevice>(device);
        mConnection = usbManager.openDevice(device);
        mInfo = updateDeviceInfo(usbManager, device, null);
        final String name = device.getDeviceName();
        final String[] v = !TextUtils.isEmpty(name) ? name.split("/") : null;
        int busnum = 0;
        int devnum = 0;
        if (v != null) {
            busnum = Integer.parseInt(v[v.length - 2]);
            devnum = Integer.parseInt(v[v.length - 1]);
        }
        mBusNum = busnum;
        mDevNum = devnum;
//			if (DEBUG) {
        if (mConnection != null) {
            final int desc = mConnection.getFileDescriptor();
            final byte[] rawDesc = mConnection.getRawDescriptors();
            Log.i(TAG, String.format(Locale.US, "name=%s,desc=%d,busnum=%d,devnum=%d,rawDesc=", name, desc, busnum, devnum) + rawDesc);
        } else {
            Log.e(TAG, "could not connect to device " + name);
        }
    }

    /**
     * copy constructor
     *
     * @param src
     * @throws IllegalStateException
     */
    private UsbControlBlock(final UsbControlBlock src) throws IllegalStateException {
        final UsbDevice device = src.getDevice();
        mUsbManager = src.mUsbManager;
        if (device == null) {
            throw new IllegalStateException("device may already be removed");
        }
        mConnection = mUsbManager.get().openDevice(device);
        if (mConnection == null) {
            throw new IllegalStateException("device may already be removed or have no permission");
        }
        mInfo = updateDeviceInfo(mUsbManager.get(), device, null);
        mWeakDevice = new WeakReference<UsbDevice>(device);
        mBusNum = src.mBusNum;
        mDevNum = src.mDevNum;
    }

    /**
     * duplicate by clone
     * need permission
     * USBMonitor never handle cloned UsbControlBlock, you should release it after using it.
     *
     * @return
     * @throws CloneNotSupportedException
     */
    @Override
    public UsbControlBlock clone() throws CloneNotSupportedException {
        final UsbControlBlock ctrlblock;
        try {
            ctrlblock = new UsbControlBlock(this);
        } catch (final IllegalStateException e) {
            throw new CloneNotSupportedException(e.getMessage());
        }
        return ctrlblock;
    }

    public final UsbDevice getDevice() {
        return mWeakDevice.get();
    }

    /**
     * get device name
     *
     * @return
     */
    public String getDeviceName() {
        final UsbDevice device = mWeakDevice.get();
        return device != null ? device.getDeviceName() : "";
    }

    /**
     * get device id
     *
     * @return
     */
    public int getDeviceId() {
        final UsbDevice device = mWeakDevice.get();
        return device != null ? device.getDeviceId() : 0;
    }

    /**
     * get device key string
     *
     * @return same value if the devices has same vendor id, product id, device class, device subclass and device protocol
     */
    public String getDeviceKeyName() {
        return USBMonitor.getDeviceKeyName(mWeakDevice.get());
    }

    /**
     * get device key string
     *
     * @param useNewAPI if true, try to use serial number
     * @return
     * @throws IllegalStateException
     */
    public String getDeviceKeyName(final boolean useNewAPI) throws IllegalStateException {
        if (useNewAPI) checkConnection();
        return USBMonitor.getDeviceKeyName(mWeakDevice.get(), mInfo.serial, useNewAPI);
    }

    /**
     * get device key
     *
     * @return
     * @throws IllegalStateException
     */
    public int getDeviceKey() throws IllegalStateException {
        checkConnection();
        return USBMonitor.getDeviceKey(mWeakDevice.get());
    }

    /**
     * get device key
     *
     * @param useNewAPI if true, try to use serial number
     * @return
     * @throws IllegalStateException
     */
    public int getDeviceKey(final boolean useNewAPI) throws IllegalStateException {
        if (useNewAPI) checkConnection();
        return USBMonitor.getDeviceKey(mWeakDevice.get(), mInfo.serial, useNewAPI);
    }

    /**
     * get device key string
     * if device has serial number, use it
     *
     * @return
     */
    public String getDeviceKeyNameWithSerial() {
        return USBMonitor.getDeviceKeyName(mWeakDevice.get(), mInfo.serial, false);
    }

    /**
     * get device key
     * if device has serial number, use it
     *
     * @return
     */
    public int getDeviceKeyWithSerial() {
        return getDeviceKeyNameWithSerial().hashCode();
    }

    /**
     * get UsbDeviceConnection
     *
     * @return
     */
    public synchronized UsbDeviceConnection getConnection() {
        return mConnection;
    }

    /**
     * get file descriptor to access USB device
     *
     * @return
     * @throws IllegalStateException
     */
    public synchronized int getFileDescriptor() throws IllegalStateException {
        checkConnection();
        return mConnection.getFileDescriptor();
    }

    /**
     * get raw descriptor for the USB device
     *
     * @return
     * @throws IllegalStateException
     */
    public synchronized byte[] getRawDescriptors() throws IllegalStateException {
        checkConnection();
        return mConnection.getRawDescriptors();
    }

    /**
     * get vendor id
     *
     * @return
     */
    public int getVenderId() {
        final UsbDevice device = mWeakDevice.get();
        return device != null ? device.getVendorId() : 0;
    }

    /**
     * get product id
     *
     * @return
     */
    public int getProductId() {
        final UsbDevice device = mWeakDevice.get();
        return device != null ? device.getProductId() : 0;
    }

    /**
     * get version string of USB
     *
     * @return
     */
    public String getUsbVersion() {
        return mInfo.usb_version;
    }

    /**
     * get manufacture
     *
     * @return
     */
    public String getManufacture() {
        return mInfo.manufacturer;
    }

    /**
     * get product name
     *
     * @return
     */
    public String getProductName() {
        return mInfo.product;
    }

    /**
     * get version
     *
     * @return
     */
    public String getVersion() {
        return mInfo.version;
    }

    /**
     * get serial number
     *
     * @return
     */
    public String getSerial() {
        return mInfo.serial;
    }

    public int getBusNum() {
        return mBusNum;
    }

    public int getDevNum() {
        return mDevNum;
    }

    /**
     * get interface
     *
     * @param interface_id
     * @throws IllegalStateException
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public synchronized UsbInterface getInterface(final int interface_id) throws IllegalStateException {
        return getInterface(interface_id, 0);
    }

    /**
     * get interface
     *
     * @param interface_id
     * @param altsetting
     * @return
     * @throws IllegalStateException
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public synchronized UsbInterface getInterface(final int interface_id, final int altsetting) throws IllegalStateException {
        checkConnection();
        SparseArray<UsbInterface> intfs = mInterfaces.get(interface_id);
        if (intfs == null) {
            intfs = new SparseArray<UsbInterface>();
            mInterfaces.put(interface_id, intfs);
        }
        UsbInterface intf = intfs.get(altsetting);
        if (intf == null) {
            final UsbDevice device = mWeakDevice.get();
            final int n = device.getInterfaceCount();
            for (int i = 0; i < n; i++) {
                final UsbInterface temp = device.getInterface(i);
                if ((temp.getId() == interface_id) && (temp.getAlternateSetting() == altsetting)) {
                    intf = temp;
                    break;
                }
            }
            if (intf != null) {
                intfs.append(altsetting, intf);
            }
        }
        return intf;
    }

    /**
     * open specific interface
     *
     * @param intf
     */
    public synchronized void claimInterface(final UsbInterface intf) {
        claimInterface(intf, true);
    }

    public synchronized void claimInterface(final UsbInterface intf, final boolean force) {
        checkConnection();
        mConnection.claimInterface(intf, force);
    }

    /**
     * close interface
     *
     * @param intf
     * @throws IllegalStateException
     */
    public synchronized void releaseInterface(final UsbInterface intf) throws IllegalStateException {
        checkConnection();
        final SparseArray<UsbInterface> intfs = mInterfaces.get(intf.getId());
        if (intfs != null) {
            final int index = intfs.indexOfValue(intf);
            intfs.removeAt(index);
            if (intfs.size() == 0) {
                mInterfaces.remove(intf.getId());
            }
        }
        mConnection.releaseInterface(intf);
    }

    /**
     * Close device
     * This also close interfaces if they are opened in Java side
     */
    public synchronized void close() {
        if (DEBUG) Log.i(TAG, "UsbControlBlock#close:");

        if (mConnection != null) {
            final int n = mInterfaces.size();
            for (int i = 0; i < n; i++) {
                final SparseArray<UsbInterface> intfs = mInterfaces.valueAt(i);
                if (intfs != null) {
                    final int m = intfs.size();
                    for (int j = 0; j < m; j++) {
                        final UsbInterface intf = intfs.valueAt(j);
                        mConnection.releaseInterface(intf);
                    }
                    intfs.clear();
                }
            }
            mInterfaces.clear();
            mConnection.close();
            mConnection = null;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null) return false;
        if (o instanceof USBMonitor.UsbControlBlock) {
            final UsbDevice device = ((USBMonitor.UsbControlBlock) o).getDevice();
            return device == null ? mWeakDevice.get() == null
                : device.equals(mWeakDevice.get());
        } else if (o instanceof UsbDevice) {
            return o.equals(mWeakDevice.get());
        }
        return super.equals(o);
    }

//		@Override
//		protected void finalize() throws Throwable {
///			close();
//			super.finalize();
//		}

    private synchronized void checkConnection() throws IllegalStateException {
        if (mConnection == null) {
            throw new IllegalStateException("already closed");
        }
    }

    public static class UsbDeviceInfo {
        public String usb_version;
        public String manufacturer;
        public String product;
        public String version;
        public String serial;

        private void clear() {
            usb_version = manufacturer = product = version = serial = null;
        }

        @Override
        public String toString() {
            return String.format("UsbDevice:usb_version=%s,manufacturer=%s,product=%s,version=%s,serial=%s",
                usb_version != null ? usb_version : "",
                manufacturer != null ? manufacturer : "",
                product != null ? product : "",
                version != null ? version : "",
                serial != null ? serial : "");
        }
    }

    /**
     * ベンダー名・製品名・バージョン・シリアルを取得する
     *
     * @param manager
     * @param device
     * @param _info
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static UsbDeviceInfo updateDeviceInfo(final UsbManager manager, final UsbDevice device, final UsbDeviceInfo _info) {
        final UsbDeviceInfo info = _info != null ? _info : new UsbDeviceInfo();
        info.clear();

        if (device != null) {
            if (BuildCheck.isLollipop()) {
                info.manufacturer = device.getManufacturerName();
                info.product = device.getProductName();
                info.serial = device.getSerialNumber();
            }
            if (BuildCheck.isMarshmallow()) {
                info.usb_version = device.getVersion();
            }
            if ((manager != null) && manager.hasPermission(device)) {
                final UsbDeviceConnection connection = manager.openDevice(device);
                final byte[] desc = connection.getRawDescriptors();

                if (TextUtils.isEmpty(info.usb_version)) {
                    info.usb_version = String.format("%x.%02x", ((int) desc[3] & 0xff), ((int) desc[2] & 0xff));
                }
                if (TextUtils.isEmpty(info.version)) {
                    info.version = String.format("%x.%02x", ((int) desc[13] & 0xff), ((int) desc[12] & 0xff));
                }
                if (TextUtils.isEmpty(info.serial)) {
                    info.serial = connection.getSerial();
                }

                final byte[] languages = new byte[256];
                int languageCount = 0;
                // controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length, int timeout)
                try {
                    int result = connection.controlTransfer(
                        USB_REQ_STANDARD_DEVICE_GET, // USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE
                        USB_REQ_GET_DESCRIPTOR,
                        (USB_DT_STRING << 8) | 0, 0, languages, 256, 0);
                    if (result > 0) {
                        languageCount = (result - 2) / 2;
                    }
                    if (languageCount > 0) {
                        if (TextUtils.isEmpty(info.manufacturer)) {
                            info.manufacturer = getString(connection, desc[14], languageCount, languages);
                        }
                        if (TextUtils.isEmpty(info.product)) {
                            info.product = getString(connection, desc[15], languageCount, languages);
                        }
                        if (TextUtils.isEmpty(info.serial)) {
                            info.serial = getString(connection, desc[16], languageCount, languages);
                        }
                    }
                } finally {
                    connection.close();
                }
            }
            if (TextUtils.isEmpty(info.manufacturer)) {
                info.manufacturer = USBVendorId.vendorName(device.getVendorId());
            }
            if (TextUtils.isEmpty(info.manufacturer)) {
                info.manufacturer = String.format("%04x", device.getVendorId());
            }
            if (TextUtils.isEmpty(info.product)) {
                info.product = String.format("%04x", device.getProductId());
            }
        }
        return info;
    }

    private static String getString(final UsbDeviceConnection connection, final int id, final int languageCount, final byte[] languages) {
        final byte[] work = new byte[256];
        String result = null;
        for (int i = 1; i <= languageCount; i++) {
            int ret = connection.controlTransfer(
                USB_REQ_STANDARD_DEVICE_GET, // USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE
                USB_REQ_GET_DESCRIPTOR,
                (USB_DT_STRING << 8) | id, languages[i], work, 256, 0);
            if ((ret > 2) && (work[0] == ret) && (work[1] == USB_DT_STRING)) {
                // skip first two bytes(bLength & bDescriptorType), and copy the rest to the string
                try {
                    result = new String(work, 2, ret - 2, "UTF-16LE");
                    if (!"Љ".equals(result)) {    // 変なゴミが返ってくる時がある
                        break;
                    } else {
                        result = null;
                    }
                } catch (final UnsupportedEncodingException e) {
                    // ignore
                }
            }
        }
        return result;
    }

    private static final int USB_DIR_OUT = 0;
    private static final int USB_DIR_IN = 0x80;
    private static final int USB_TYPE_MASK = (0x03 << 5);
    private static final int USB_TYPE_STANDARD = (0x00 << 5);
    private static final int USB_TYPE_CLASS = (0x01 << 5);
    private static final int USB_TYPE_VENDOR = (0x02 << 5);
    private static final int USB_TYPE_RESERVED = (0x03 << 5);
    private static final int USB_RECIP_MASK = 0x1f;
    private static final int USB_RECIP_DEVICE = 0x00;
    private static final int USB_RECIP_INTERFACE = 0x01;
    private static final int USB_RECIP_ENDPOINT = 0x02;
    private static final int USB_RECIP_OTHER = 0x03;
    private static final int USB_RECIP_PORT = 0x04;
    private static final int USB_RECIP_RPIPE = 0x05;
    private static final int USB_REQ_GET_STATUS = 0x00;
    private static final int USB_REQ_CLEAR_FEATURE = 0x01;
    private static final int USB_REQ_SET_FEATURE = 0x03;
    private static final int USB_REQ_SET_ADDRESS = 0x05;
    private static final int USB_REQ_GET_DESCRIPTOR = 0x06;
    private static final int USB_REQ_SET_DESCRIPTOR = 0x07;
    private static final int USB_REQ_GET_CONFIGURATION = 0x08;
    private static final int USB_REQ_SET_CONFIGURATION = 0x09;
    private static final int USB_REQ_GET_INTERFACE = 0x0A;
    private static final int USB_REQ_SET_INTERFACE = 0x0B;
    private static final int USB_REQ_SYNCH_FRAME = 0x0C;
    private static final int USB_REQ_SET_SEL = 0x30;
    private static final int USB_REQ_SET_ISOCH_DELAY = 0x31;
    private static final int USB_REQ_SET_ENCRYPTION = 0x0D;
    private static final int USB_REQ_GET_ENCRYPTION = 0x0E;
    private static final int USB_REQ_RPIPE_ABORT = 0x0E;
    private static final int USB_REQ_SET_HANDSHAKE = 0x0F;
    private static final int USB_REQ_RPIPE_RESET = 0x0F;
    private static final int USB_REQ_GET_HANDSHAKE = 0x10;
    private static final int USB_REQ_SET_CONNECTION = 0x11;
    private static final int USB_REQ_SET_SECURITY_DATA = 0x12;
    private static final int USB_REQ_GET_SECURITY_DATA = 0x13;
    private static final int USB_REQ_SET_WUSB_DATA = 0x14;
    private static final int USB_REQ_LOOPBACK_DATA_WRITE = 0x15;
    private static final int USB_REQ_LOOPBACK_DATA_READ = 0x16;
    private static final int USB_REQ_SET_INTERFACE_DS = 0x17;

    private static final int USB_REQ_STANDARD_DEVICE_SET = (USB_DIR_OUT | USB_TYPE_STANDARD | USB_RECIP_DEVICE);        // 0x10
    private static final int USB_REQ_STANDARD_DEVICE_GET = (USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_DEVICE);            // 0x90
    private static final int USB_REQ_STANDARD_INTERFACE_SET = (USB_DIR_OUT | USB_TYPE_STANDARD | USB_RECIP_INTERFACE);    // 0x11
    private static final int USB_REQ_STANDARD_INTERFACE_GET = (USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_INTERFACE);    // 0x91
    private static final int USB_REQ_STANDARD_ENDPOINT_SET = (USB_DIR_OUT | USB_TYPE_STANDARD | USB_RECIP_ENDPOINT);    // 0x12
    private static final int USB_REQ_STANDARD_ENDPOINT_GET = (USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_ENDPOINT);        // 0x92

    private static final int USB_REQ_CS_DEVICE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_DEVICE);                // 0x20
    private static final int USB_REQ_CS_DEVICE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_DEVICE);                    // 0xa0
    private static final int USB_REQ_CS_INTERFACE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_INTERFACE);            // 0x21
    private static final int USB_REQ_CS_INTERFACE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_INTERFACE);            // 0xa1
    private static final int USB_REQ_CS_ENDPOINT_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);                // 0x22
    private static final int USB_REQ_CS_ENDPOINT_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);                // 0xa2

    private static final int USB_REQ_VENDER_DEVICE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_DEVICE);                // 0x40
    private static final int USB_REQ_VENDER_DEVICE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_DEVICE);                // 0xc0
    private static final int USB_REQ_VENDER_INTERFACE_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_INTERFACE);        // 0x41
    private static final int USB_REQ_VENDER_INTERFACE_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_INTERFACE);        // 0xc1
    private static final int USB_REQ_VENDER_ENDPOINT_SET = (USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);            // 0x42
    private static final int USB_REQ_VENDER_ENDPOINT_GET = (USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_ENDPOINT);            // 0xc2

    private static final int USB_DT_DEVICE = 0x01;
    private static final int USB_DT_CONFIG = 0x02;
    private static final int USB_DT_STRING = 0x03;
    private static final int USB_DT_INTERFACE = 0x04;
    private static final int USB_DT_ENDPOINT = 0x05;
    private static final int USB_DT_DEVICE_QUALIFIER = 0x06;
    private static final int USB_DT_OTHER_SPEED_CONFIG = 0x07;
    private static final int USB_DT_INTERFACE_POWER = 0x08;
    private static final int USB_DT_OTG = 0x09;
    private static final int USB_DT_DEBUG = 0x0a;
    private static final int USB_DT_INTERFACE_ASSOCIATION = 0x0b;
    private static final int USB_DT_SECURITY = 0x0c;
    private static final int USB_DT_KEY = 0x0d;
    private static final int USB_DT_ENCRYPTION_TYPE = 0x0e;
    private static final int USB_DT_BOS = 0x0f;
    private static final int USB_DT_DEVICE_CAPABILITY = 0x10;
    private static final int USB_DT_WIRELESS_ENDPOINT_COMP = 0x11;
    private static final int USB_DT_WIRE_ADAPTER = 0x21;
    private static final int USB_DT_RPIPE = 0x22;
    private static final int USB_DT_CS_RADIO_CONTROL = 0x23;
    private static final int USB_DT_PIPE_USAGE = 0x24;
    private static final int USB_DT_SS_ENDPOINT_COMP = 0x30;
    private static final int USB_DT_CS_DEVICE = (USB_TYPE_CLASS | USB_DT_DEVICE);
    private static final int USB_DT_CS_CONFIG = (USB_TYPE_CLASS | USB_DT_CONFIG);
    private static final int USB_DT_CS_STRING = (USB_TYPE_CLASS | USB_DT_STRING);
    private static final int USB_DT_CS_INTERFACE = (USB_TYPE_CLASS | USB_DT_INTERFACE);
    private static final int USB_DT_CS_ENDPOINT = (USB_TYPE_CLASS | USB_DT_ENDPOINT);
    private static final int USB_DT_DEVICE_SIZE = 18;

}
