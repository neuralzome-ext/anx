package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Common
import com.flomobility.anx.proto.Device
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetImeiNumbersRpc @Inject constructor() :
    Rpc<Common.Empty, Device.GetImeiNumbersResponse>() {

    private fun getImeiNumbers(){
        val imeiNumbers = Device.GetImeiNumbersResponse.newBuilder()
        imeiNumbers.setImeis(1,"dummy")
    }

    override val name: String
        get() = "GetImeiNumbers"

    override fun execute(req: Common.Empty): Device.GetImeiNumbersResponse {
        getImeiNumbers() //TODO fetch imei numbers
        val imeiNumbers = Device.GetImeiNumbersResponse.newBuilder()
        imeiNumbers.getImeis(1)
        return imeiNumbers.build()
    }

    override fun execute(req: ByteArray): Device.GetImeiNumbersResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
