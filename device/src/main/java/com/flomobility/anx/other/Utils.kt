package com.flomobility.anx.other

import com.flomobility.anx.proto.Common
import org.zeromq.ZMQ

fun ZMQ.Socket.sendStdResponse(
    success: Boolean, message: String = ""
) {
    val stdResponse = Common.StdResponse.newBuilder().apply {
        this.success = success
        this.message = message
    }.build()

    this.send(stdResponse.toByteArray(), 0)

}
