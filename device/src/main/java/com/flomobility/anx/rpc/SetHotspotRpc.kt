package com.flomobility.anx.rpc

import com.flomobility.anx.hotspot.HotspotManager
import com.flomobility.anx.proto.Common
import com.flomobility.anx.proto.Device
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetHotspotRpc @Inject constructor(
    private val hotspotManager: HotspotManager
) : Rpc<Device.SetWifiRequest, Common.StdResponse>() {

    private fun setHotspot(req: Device.SetWifiRequest): Common.StdResponse {
        val result = hotspotManager.setHotspot(req.ssid, req.password)
        val stdResponse = Common.StdResponse.newBuilder().apply {
            this.success = result.success
            this.message = result.message
        }.build()
        return stdResponse
    }

    override val name: String
        get() = "SetHotspot"

    override fun execute(req: Device.SetWifiRequest): Common.StdResponse {
        return setHotspot(req)
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Device.SetWifiRequest.parseFrom(req))
    }
}
