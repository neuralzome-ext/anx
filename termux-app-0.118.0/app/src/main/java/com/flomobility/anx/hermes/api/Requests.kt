package com.flomobility.anx.hermes.api

import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken


data class SubscribeRequest(
    @SerializedName("subscribe")
    val subscribe: Boolean,
    @SerializedName("subscribers_ip")
    val ip: String
) {

    companion object {
        val type = object : TypeToken<SubscribeRequest>() {}.type
    }
}

data class SignalRequest(
    @SerializedName("signal")
    val signal: Int
) {
    companion object {
        val type = object : TypeToken<SignalRequest>() {}.type
    }
}
