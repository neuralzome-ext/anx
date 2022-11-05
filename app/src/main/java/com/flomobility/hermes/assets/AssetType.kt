package com.flomobility.hermes.assets

import com.flomobility.hermes.R

enum class AssetType(val alias: String, val image: Int) {
    IMU("imu", R.drawable.ic_imu),
    GNSS("gnss", R.drawable.ic_gps),
    USB_SERIAL("usb_serial", R.drawable.ic_usb_serial),
    CAM("camera", R.drawable.ic_video),
    CLASSIC_BT("classic_bt", R.drawable.ic_bluetooth),
    MIC("mic", R.drawable.ic_mic),
    BLE("ble", R.drawable.ic_bluetooth),
    SPEAKER("speaker", R.drawable.ic_speaker),
    PHONE("phone", R.drawable.ic_phone),
    UNK("unknown", R.drawable.anx_logo)
}

fun getAssetTypeFromAlias(alias: String) = when (alias) {
    AssetType.IMU.alias -> AssetType.IMU
    AssetType.GNSS.alias -> AssetType.GNSS
    AssetType.USB_SERIAL.alias -> AssetType.USB_SERIAL
    AssetType.CAM.alias -> AssetType.CAM
    AssetType.CLASSIC_BT.alias -> AssetType.CLASSIC_BT
    AssetType.BLE.alias -> AssetType.BLE
    AssetType.SPEAKER.alias -> AssetType.SPEAKER
    AssetType.PHONE.alias -> AssetType.PHONE
    else -> AssetType.UNK
}