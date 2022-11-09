package com.flomobility.anx.hermes.network.requests

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("deviceId") val deviceID: String?
)

@Keep
data class InfoRequest(
    @SerializedName("deviceId") val deviceID: String
)