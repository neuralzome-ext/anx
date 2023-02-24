package com.flomobility.anx.assets

import android.os.Build
import androidx.annotation.RequiresApi
import com.flomobility.anx.assets.gnss.DeviceGnss
import com.flomobility.anx.assets.imu.DeviceImu
import com.flomobility.anx.common.toStdResponse
import com.flomobility.anx.proto.Assets
import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetManager @Inject constructor(
    private val deviceImu: DeviceImu,
    private val deviceGnss: DeviceGnss
) {

    fun startDeviceImu(startDeviceImu: Assets.StartDeviceImu): Common.StdResponse {
        val status = deviceImu.start(startDeviceImu)
        return status.toStdResponse()
    }

    fun stopDeviceImu(): Common.StdResponse {
        val status = deviceImu.stop()
        return status.toStdResponse()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun startDeviceGnss(startDeviceGnss: Common.Empty): Common.StdResponse {
        val status = deviceGnss.start(startDeviceGnss)
        return status.toStdResponse()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun stopDeviceGnss(): Common.StdResponse {
        val status = deviceGnss.stop()
        return status.toStdResponse()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun getAssetState(): Assets.AssetState {
        val assetState = Assets.AssetState.newBuilder().apply {
            this.imu = deviceImu.getDeviceImuSelect()
            this.gnss = deviceGnss.getDeviceGnssSelect()
            // TODO : add selectors for camera
        }
        return assetState.build()
    }

}
