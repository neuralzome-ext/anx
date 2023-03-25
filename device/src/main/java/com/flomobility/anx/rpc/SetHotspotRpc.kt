package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Common
import com.flomobility.anx.proto.Device
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetHotspotRpc @Inject constructor() :
    Rpc<Device.SetWifiRequest, Common.StdResponse>() {

    private fun setHotspot(): Common.StdResponse? {
        val stdResponse = Common.StdResponse.newBuilder().apply {
            this.success = true
            this.message = "Hotspot set up done"
        }.build()
        return stdResponse
    }

    override val name: String
        get() = "SetHotspot"

    override fun execute(req: Device.SetWifiRequest): Common.StdResponse {
        val stdResponse = Common.StdResponse.newBuilder()
        stdResponse.apply {
            success = setHotspot()?.success!!
            message = setHotspot()?.message
        }
        return stdResponse.build()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Device.SetWifiRequest.parseFrom(req))
    }
}
