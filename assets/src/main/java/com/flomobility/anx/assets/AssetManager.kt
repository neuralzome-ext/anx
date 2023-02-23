package com.flomobility.anx.assets

import com.flomobility.anx.assets.imu.DeviceImu
import com.flomobility.anx.common.toStdResponse
import com.flomobility.anx.proto.Assets
import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetManager @Inject constructor(
    private val deviceImu: DeviceImu
) {

    fun startDeviceImu(startDeviceImu: Assets.StartDeviceImu): Common.StdResponse {
        val status = deviceImu.start(startDeviceImu)
        return status.toStdResponse()
    }

    fun stopDeviceImu(): Common.StdResponse {
        val status = deviceImu.stop()
        return status.toStdResponse()
    }

    fun getAssetState(): Assets.AssetState {
        val assetState = Assets.AssetState.newBuilder().apply {
            this.imu = deviceImu.getDeviceImuSelect()
            // TODO : add selectors for gnss, camera
        }
        return assetState.build()
    }

}
