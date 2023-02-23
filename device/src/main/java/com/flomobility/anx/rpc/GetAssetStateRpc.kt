package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Assets
import com.flomobility.anx.proto.Assets.DeviceCameraSelect
import com.flomobility.anx.proto.Assets.DeviceGnssSelect
import com.flomobility.anx.proto.Assets.DeviceImuSelect
import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAssetStateRpc @Inject constructor() :
    Rpc<Common.Empty, Assets.AssetState>() {

    private fun getimu(): DeviceImuSelect {
        val deviceImuSelect = Assets.DeviceImuSelect.newBuilder()
        deviceImuSelect.getFps(1)
        return deviceImuSelect.build() // TODO : change this
    }

    private fun getgnss(): DeviceGnssSelect {
        val deviceGnssSelect = Assets.DeviceGnssSelect.newBuilder()
        deviceGnssSelect.available
        return deviceGnssSelect.build() // TODO : change this
    }

    private fun getcamera(): DeviceCameraSelect {
        val deviceCameraSelect = Assets.DeviceCameraSelect.newBuilder()
        deviceCameraSelect.getCameraStreams(1) // TODO : change this
        return deviceCameraSelect.build()
    }

    override val name: String
        get() = "GetAssetState"

    override fun execute(req: Common.Empty): Assets.AssetState {
        val assetState = Assets.AssetState.newBuilder()
        assetState.imu = getimu()
        assetState.gnss = getgnss()
        assetState.camera = getcamera()
        return assetState.build()
    }

    override fun execute(req: ByteArray): Assets.AssetState {
        return execute(Common.Empty.parseFrom(req))
    }
}
