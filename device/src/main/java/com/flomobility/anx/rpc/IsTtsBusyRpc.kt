package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IsTtsBusyRpc @Inject constructor() :
    Rpc<Common.Empty, Common.StdResponse>() {

    private fun IsTtsBusy(): Common.StdResponse? {

        val stdResponse = Common.StdResponse.newBuilder().apply {
            this.success = true
            this.message = "TTS busy"
        }.build()
        return stdResponse
    }

    override val name: String
        get() = "IsTtsBusy"

    override fun execute(req: Common.Empty): Common.StdResponse {
        val stdResponse = Common.StdResponse.newBuilder()
        stdResponse.apply {
            success = IsTtsBusy()?.success!!
            message = IsTtsBusy()?.message
        }
        return stdResponse.build()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
