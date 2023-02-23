package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Common
import com.flomobility.anx.proto.Device
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartUsbTetheringRpc @Inject constructor() :
    Rpc<Common.Empty, Common.StdResponse>() {

    private fun startUsbTethering(): Common.StdResponse? {

        val stdResponse = Common.StdResponse.newBuilder().apply {
            this.success = true
            this.message = "USB Tethering started"
        }.build()
        return stdResponse
    }

    override val name: String
        get() = "StartUsbTethering"

    override fun execute(req: Common.Empty): Common.StdResponse {
        val stdResponse = Common.StdResponse.newBuilder()
        stdResponse.apply {
            success = startUsbTethering()?.success!!
            message = startUsbTethering()?.message
        }
        return stdResponse.build()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Common.Empty.parseFrom(req))
    }

}
