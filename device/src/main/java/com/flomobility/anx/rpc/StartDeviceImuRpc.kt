package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Assets
import com.flomobility.anx.proto.Common
import com.flomobility.anx.proto.Device
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartDeviceImuRpc @Inject constructor() :
    Rpc<Assets.StartDeviceImu, Common.StdResponse>() {

    private fun startDeviceImu(): Common.StdResponse? {
        val stdResponse = Common.StdResponse.newBuilder().apply {
            this.success = true
            this.message = "Device Imu started"
        }.build()
        return stdResponse
    }

    override val name: String
        get() = "StartDeviceImu"

    override fun execute(req: Assets.StartDeviceImu): Common.StdResponse {
        val stdResponse = Common.StdResponse.newBuilder()
        stdResponse.apply {
            success = startDeviceImu()?.success!!
            message = startDeviceImu()?.message
        }
        return stdResponse.build()
    }


    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Assets.StartDeviceImu.parseFrom(req))
    }
}
