package com.flomobility.anx.rpc

import com.flomobility.anx.logs.AndroidLogsUtil
import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartAndroidLogsRpc @Inject constructor(
    private val androidLogsUtil: AndroidLogsUtil
) : Rpc<Common.Empty, Common.StdResponse>() {

    override val name: String
        get() = "StartAndroidLogs"

    private fun startLogging(): Common.StdResponse {
        val stdResponse = Common.StdResponse.newBuilder()
        val result = androidLogsUtil.startLogging()
        stdResponse.success = result.success
        stdResponse.message = result.message
        return stdResponse.build()
    }

    override fun execute(req: Common.Empty): Common.StdResponse {
        return startLogging()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
