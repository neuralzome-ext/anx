package com.flomobility.anx.rpc

import com.flomobility.anx.proto.Common
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RebootRpc @Inject constructor() :
    Rpc<Common.Empty, Common.StdResponse>() {

    val stdResponse = Common.StdResponse.newBuilder()

    private fun reboot(): Common.StdResponse {
        try {
            Runtime.getRuntime().exec("su -c reboot")
            stdResponse.apply {
                success = true
                message = "Reboot success"
            }
        } catch (e: IOException) {
            stdResponse.apply {
                success = false
                message = "Reboot failed"
            }
            Timber.d(e)
        }
        return stdResponse.build()
    }

    override val name: String
        get() = "Reboot"

    override fun execute(req: Common.Empty): Common.StdResponse {
        return reboot()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Common.Empty.parseFrom(req))
    }


}
