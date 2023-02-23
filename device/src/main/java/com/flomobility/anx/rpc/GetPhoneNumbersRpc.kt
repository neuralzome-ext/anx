package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Common
import com.flomobility.anx.proto.Device
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetPhoneNumbersRpc @Inject constructor() :
    Rpc<Common.Empty, Device.GetPhoneNumbersResponse>() {

    private fun getPhoneNumbers(){
        val phoneNumbers = Device.GetImeiNumbersResponse.newBuilder()
        phoneNumbers.setImeis(1,"dummy")
    }

    override val name: String
        get() = "GetPhoneNumbers"

    override fun execute(req: Common.Empty): Device.GetPhoneNumbersResponse {
        getPhoneNumbers() //TODO fetch phone numbers
        val phoneNumbers = Device.GetPhoneNumbersResponse.newBuilder()
        phoneNumbers.getPhoneNumbers(1)
        return phoneNumbers.build()
    }

    override fun execute(req: ByteArray): Device.GetPhoneNumbersResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
