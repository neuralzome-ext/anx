package com.flomobility.anx.hermes.network.responses

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName


@Keep
data class LoginResponse(
    @SerializedName("email") val email: String,
    @SerializedName("_id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("robot") val robot: Robot,
    @SerializedName("token") val token: String
) {
    @Keep
    data class Robot(
        @SerializedName("expiry") val expiry: String,
        @SerializedName("id") val id: String,
        @SerializedName("name") val name: String
    )
}

@Keep
data class InfoResponse(
    @SerializedName("access") val access: Boolean = false,
    @SerializedName("expiry") val expiry: String = "",
    @SerializedName("_id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("owner") val owner: String = ""
)

@Keep
data class FloApiError(
    @SerializedName("errorCode") var code: Int?,
    @SerializedName("message") val message: String?
)