package com.flomobility.anx.other

import com.flomobility.anx.proto.Common
import org.zeromq.ZMQ
import timber.log.Timber

fun ZMQ.Socket.sendStdResponse(
    success: Boolean, message: String = ""
) {
    val stdResponse = Common.StdResponse.newBuilder().apply {
        this.success = success
        this.message = message
    }.build()

    this.send(stdResponse.toByteArray(), 0)

}

/**
 * Function to run commands with root access.
 *
 * @param cmd The command that needs to be executed
 * */
fun runAsRoot(cmd: String) {
    try {
        //-c will cause the next argument to be treated as a command
        val process = Runtime.getRuntime().exec("su -c $cmd")
        process.waitFor() //wait for the native process to finish executing.
    } catch (e: java.lang.Exception) {
        Timber.e("Root error: ${e.message}")
//        Timber.d("Root error: " + e.message)
    }
}
