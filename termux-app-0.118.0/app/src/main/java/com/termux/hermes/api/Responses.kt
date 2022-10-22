package com.termux.hermes.api

import com.google.gson.annotations.SerializedName

class StandardResponse(
    @SerializedName("success")
    var success: Boolean = false,
    @SerializedName("message")
    var message: String = ""
)

data class GetIdentityResponse(
    @SerializedName("imei")
    val imei: String = ""
)
