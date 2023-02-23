package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Assets
import com.flomobility.anx.proto.Common
import com.flomobility.anx.proto.Device
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAvailableLanguagesRpc @Inject constructor() :
    Rpc<Common.Empty, Device.GetAvailableLanguagesResponse>() {

    private fun getAvaialableLanguages() {
        val availableLanguages = Device.GetAvailableLanguagesResponse.newBuilder()
        availableLanguages.setLanguages(1, "dummy")
    }

    override val name: String
        get() = "GetAvailableLanguages"

    override fun execute(req: Common.Empty): Device.GetAvailableLanguagesResponse {
        getAvaialableLanguages() //TODO fetch available languages
        val availableLanguages = Device.GetAvailableLanguagesResponse.newBuilder()
        availableLanguages.getLanguages(1)
        return availableLanguages.build()
    }

    override fun execute(req: ByteArray): Device.GetAvailableLanguagesResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
