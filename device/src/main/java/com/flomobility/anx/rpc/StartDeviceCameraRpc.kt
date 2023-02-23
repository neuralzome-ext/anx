package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Assets
import com.flomobility.anx.proto.Common
import com.flomobility.anx.proto.Device
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartDeviceCameraRpc @Inject constructor() :
    Rpc<Assets.StartDeviceCamera, Common.StdResponse>() {

    private fun startDeviceCamera(): Common.StdResponse? {
        val stdResponse = Common.StdResponse.newBuilder().apply {
            this.success = true
            this.message = "Device Camera started"
        }.build()
        return stdResponse
    }

    override val name: String
        get() = "StartDeviceCamera"

    override fun execute(req: Assets.StartDeviceCamera): Common.StdResponse {
        val stdResponse = Common.StdResponse.newBuilder()
        stdResponse.apply {
            success = startDeviceCamera()?.success!!
            message = startDeviceCamera()?.message
        }
        return stdResponse.build()

    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Assets.StartDeviceCamera.parseFrom(req))
    }
}
