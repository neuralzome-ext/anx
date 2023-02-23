package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RebootRpc @Inject constructor() :
    Rpc<Common.Empty, Common.StdResponse>() {

    private fun Reboot(): Common.StdResponse? {

        val stdResponse = Common.StdResponse.newBuilder().apply {
            this.success = true
            this.message = "Rebooted"
        }.build()
        return stdResponse
    }

    override val name: String
        get() = "Reboot"

    override fun execute(req: Common.Empty): Common.StdResponse {
        val stdResponse = Common.StdResponse.newBuilder()
        stdResponse.apply {
            success = Reboot()?.success!!
            message = Reboot()?.message
        }
        return stdResponse.build()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Common.Empty.parseFrom(req))
    }


}
