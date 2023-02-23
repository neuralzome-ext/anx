package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Assets
import com.flomobility.anx.proto.Common
import com.flomobility.anx.proto.Device
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsRpc @Inject constructor() :
    Rpc<Device.TtsRequest, Common.StdResponse>() {

    private fun getTts(): Common.StdResponse? {
        val stdResponse = Common.StdResponse.newBuilder().apply {
            this.success = true
            this.message = "Tts set up done"
        }.build()
        return stdResponse
    }

    override val name: String
        get() = "Tts"

    override fun execute(req: Device.TtsRequest): Common.StdResponse {
        val stdResponse = Common.StdResponse.newBuilder()
        stdResponse.apply {
            success = getTts()?.success!!
            message = getTts()?.message
        }
        return stdResponse.build()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Device.TtsRequest.parseFrom(req))
    }
}
