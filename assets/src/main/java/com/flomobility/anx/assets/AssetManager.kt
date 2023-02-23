package com.flomobility.anx.assets

import com.flomobility.anx.assets.imu.DeviceImu
import com.flomobility.anx.proto.Assets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetManager @Inject constructor(
    private val deviceImu: DeviceImu
) {

    fun getAssetState(): Assets.AssetState {
        val assetState = Assets.AssetState.newBuilder().apply {
            this.imu = deviceImu.getDeviceImuSelect()
            // TODO : add selectors for gnss, camera
        }
        return assetState.build()
    }

}
