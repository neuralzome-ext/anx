package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StopDeviceImuRpc @Inject constructor() :
    Rpc<Common.Empty, Common.StdResponse>() {

    private fun StopDeviceImu(): Common.StdResponse? {

        val stdResponse = Common.StdResponse.newBuilder().apply {
            this.success = true
            this.message = "Device IMU stopped"
        }.build()
        return stdResponse
    }
    override val name: String
        get() = "StopDeviceImu"

    override fun execute(req: Common.Empty): Common.StdResponse {
        val stdResponse = Common.StdResponse.newBuilder()
        stdResponse.apply {
            success = StopDeviceImu()?.success!!
            message = StopDeviceImu()?.message
        }
        return stdResponse.build()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
