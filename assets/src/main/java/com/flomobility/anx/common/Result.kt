package com.flomobility.anx.common

import com.flomobility.anx.proto.Common

data class Result(
    val success: Boolean,
    val message: String = ""
)

fun Result.toStdResponse(): Common.StdResponse {
    return Common.StdResponse.newBuilder().apply {
        this.success = this@toStdResponse.success
        this.message = this@toStdResponse.message
    }.build()
}
