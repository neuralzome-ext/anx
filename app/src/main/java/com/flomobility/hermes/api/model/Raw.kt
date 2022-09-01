package com.flomobility.hermes.api.model

import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

data class Raw(
    @SerializedName("data")
    val data: String = ""
) {
    companion object {
        val type = object : TypeToken<Raw>() {}.type
    }
}