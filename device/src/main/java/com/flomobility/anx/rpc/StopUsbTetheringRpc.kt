package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StopUsbTetheringRpc @Inject constructor() :
    Rpc<Common.Empty, Common.StdResponse>() {

    private fun stopUsbTethering(): Common.StdResponse? {

        val stdResponse = Common.StdResponse.newBuilder().apply {
            this.success = true
            this.message = "USB Tethering stopped"
        }.build()
        return stdResponse
    }

    override val name: String
        get() = "StopUsbTethering"

    override fun execute(req: Common.Empty): Common.StdResponse {
        val stdResponse = Common.StdResponse.newBuilder()
        stdResponse.apply {
            success = stopUsbTethering()?.success!!
            message = stopUsbTethering()?.message
        }
        return stdResponse.build()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
