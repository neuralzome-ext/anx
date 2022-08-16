package com.flomobility.hermes.api

import com.google.gson.annotations.SerializedName

class StandardResponse(
    @SerializedName("success")
    var success: Boolean = false,
    @SerializedName("message")
    var message: String = ""
)