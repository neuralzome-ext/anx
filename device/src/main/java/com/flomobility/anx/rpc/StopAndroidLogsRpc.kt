package com.flomobility.anx.rpc

import com.flomobility.anx.logs.AndroidLogsUtil
import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StopAndroidLogsRpc @Inject constructor(
    private val androidLogsUtil: AndroidLogsUtil
) : Rpc<Common.Empty, Common.StdResponse>() {

    override val name: String
        get() = "StopAndroidLogs"

    private fun stopLogging(): Common.StdResponse {
        val stdResponse = Common.StdResponse.newBuilder()
        val result = androidLogsUtil.stopLogging()
        stdResponse.success = result.success
        stdResponse.message = result.message
        return stdResponse.build()
    }

    override fun execute(req: Common.Empty): Common.StdResponse {
        return stopLogging()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
