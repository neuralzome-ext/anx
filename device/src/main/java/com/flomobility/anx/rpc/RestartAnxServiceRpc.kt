package com.flomobility.anx.rpc

import android.os.Process
import com.flomobility.anx.other.Constants
import com.flomobility.anx.other.runAsRoot
import com.flomobility.anx.proto.Common
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestartAnxServiceRpc @Inject constructor() :
    Rpc<Common.Empty, Common.StdResponse>() {

    private var restartProcedure : (() -> Unit)? = null

    fun doRestartProcedure(func: () -> Unit) {
        restartProcedure = func
    }

    private fun restartAnxService(): Common.StdResponse {
        val stdResponse = Common.StdResponse.newBuilder()
        try {
            stdResponse.success = true
            stdResponse.message = "Restarting in 3s"
            Thread {
                Thread.sleep(3000L)
                restartProcedure?.invoke()
                runAsRoot("killall logcat")
                runAsRoot("kill ${Process.myPid()}; su -c am start -n com.flomobility.anx.headless/com.flomobility.anx.activity.MainActivity")
            }.start()
        } catch (e: Exception) {
            stdResponse.success = false
            stdResponse.message = e.message ?: Constants.UNKNOWN_ERROR_MSG
        }
        return stdResponse.build()
    }

    override val name: String
        get() = "RestartAnxService"

    override fun execute(req: Common.Empty): Common.StdResponse {
        return restartAnxService()
    }

    override fun execute(req: ByteArray): Common.StdResponse {
        return execute(Common.Empty.parseFrom(req))
    }
}
