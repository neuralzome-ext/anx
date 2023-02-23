package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShutdownRpc @Inject constructor() :
    Rpc<Common.Empty, Common.StdResponse>() {

    private fun Shutdown(): Common.StdResponse? {

        val stdResponse = Common.StdResponse.newBuilder().apply {
            this.success = true
            this.message = "Shutdown succesfully"
        }.build()
        return stdResponse
    }

    override val name: String
        get() = "Shutdown"

    override fun execute(req: Common.Empty): Common.StdResponse {
        val stdResponse = Common.StdResponse.newBuilder()
        stdResponse.apply {
            success = Shutdown()?.success!!
            message = Shutdown()?.message
        }
        return stdResponse.build()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
