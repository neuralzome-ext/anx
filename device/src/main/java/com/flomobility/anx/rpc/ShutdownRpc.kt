package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Common
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShutdownRpc @Inject constructor() :
    Rpc<Common.Empty, Common.StdResponse>() {

    val stdResponse = Common.StdResponse.newBuilder()

    private fun shutdown(): Common.StdResponse {
        try {
            Runtime.getRuntime().exec("su -c reboot -p")
            stdResponse.apply {
                success = true
                message = "Shutdown success"
            }
        } catch (e: IOException) {
            stdResponse.apply {
                success = false
                message = "Shutdown failed"
            }
            Timber.d(e)
        }
        return stdResponse.build()
    }

    override val name: String
        get() = "Shutdown"

    override fun execute(req: Common.Empty): Common.StdResponse {
        return shutdown()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
