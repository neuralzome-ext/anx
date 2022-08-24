package com.flomobility.hermes.assets

enum class AssetType(val alias: String) {
    IMU("imu"), UNK("unknown")
}

fun getAssetTypeFromAlias(alias: String) = when(alias) {
    AssetType.IMU.alias -> AssetType.IMU
    else -> AssetType.UNK
}