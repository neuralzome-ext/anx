package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartDeviceGnssRpc @Inject constructor() :
    Rpc<Common.Empty, Common.StdResponse>() {

    private fun StartDeviceGnss(): Common.StdResponse? {

        val stdResponse = Common.StdResponse.newBuilder().apply {
            this.success = true
            this.message = "Device Gnss started"
        }.build()
        return stdResponse
    }

    override val name: String
        get() = "StartDeviceGnss"

    override fun execute(req: Common.Empty): Common.StdResponse {
        val stdResponse = Common.StdResponse.newBuilder()
        stdResponse.apply {
            success = StartDeviceGnss()?.success!!
            message = StartDeviceGnss()?.message
        }
        return stdResponse.build()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
