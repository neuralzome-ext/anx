package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestartAnxServiceRpc @Inject constructor() :
    Rpc<Common.Empty, Common.StdResponse>() {

    private fun RestartAnxService(): Common.StdResponse? {

        val stdResponse = Common.StdResponse.newBuilder().apply {
            this.success = true
            this.message = "Anx restarted"
        }.build()
        return stdResponse
    }

    override val name: String
        get() = "RestartAnxService"

    override fun execute(req: Common.Empty): Common.StdResponse {
        val stdResponse = Common.StdResponse.newBuilder()
        stdResponse.apply {
            success = RestartAnxService()?.success!!
            message = RestartAnxService()?.message
        }
        return stdResponse.build()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
