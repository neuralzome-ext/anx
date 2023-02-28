package com.flomobility.anx.rpc

import com.flomobility.anx.common.toStdResponse
import com.flomobility.anx.proto.Common
import com.flomobility.anx.proto.Device
import com.flomobility.anx.wifi.WiFiManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SetWifiRpc @Inject constructor(
    private val wiFiManager: WiFiManager
) : Rpc<Device.SetWifiRequest, Common.StdResponse>() {

    override val name: String
        get() = "SetWifi"

    override fun execute(req: Device.SetWifiRequest): Common.StdResponse {
        val wifiStatus = wiFiManager.connectToWifi(req.ssid, req.password)
        return wifiStatus.toStdResponse()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Device.SetWifiRequest.parseFrom(req))
    }
}
