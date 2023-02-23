package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Common
import com.flomobility.anx.proto.Device
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetWifiRpc @Inject constructor() :
    Rpc<Device.SetWifiRequest, Common.StdResponse>() {

    private fun setWifi(): Common.StdResponse? {
        val stdResponse = Common.StdResponse.newBuilder().apply {
            this.success = true
            this.message = "Wifi set up done"
        }.build()
        return stdResponse
    }

    override val name: String
        get() = "SetWifi"

    override fun execute(req: Device.SetWifiRequest): Common.StdResponse {
        val stdResponse = Common.StdResponse.newBuilder()
        stdResponse.apply {
            success = setWifi()?.success!!
            message = setWifi()?.message
        }
        return stdResponse.build()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Device.SetWifiRequest.parseFrom(req))
    }
}
