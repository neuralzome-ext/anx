package com.flomobility.anx.hermes.api.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class PhoneStates(
    @SerializedName("battery") val battery: Battery = Battery(),
    @SerializedName("cpu") val cpu: Cpu = Cpu(),
    @SerializedName("gpu") val gpu: Gpu = Gpu(),
    @SerializedName("vram") val vram: Memory = Memory(),
    @SerializedName("ram") val ram: Memory = Memory(),
    @SerializedName("storage") val storage: Memory = Memory(),
    @SerializedName("thermals") val thermals: List<Thermal> = listOf(),
    @SerializedName("uptime") val uptime: Double = 0.0
) {
    @Keep
    data class Battery(
        @SerializedName("charging") val charging: Boolean = false,
        @SerializedName("current") val current: Int = 0,
        @SerializedName("level") val level: Int = 0,
        @SerializedName("voltage") val voltage: Int = 0
    )

    @Keep
    data class Cpu(
        @SerializedName("architectures") val architectures: String = "",
        @SerializedName("freq") val cpuFreq: List<CpuFreq> = listOf(),
        @SerializedName("processor") val processor: String = "",
        @SerializedName("type") val type: String = ""
    ) {
        @Keep
        data class CpuFreq(
            @SerializedName("current") val current: Double = 0.0,
            @SerializedName("max") val maximum: Double = 0.0,
            @SerializedName("min") val minimum: Double = 0.0
        )
    }

    @Keep
    data class Gpu(
        @SerializedName("renderer")
        val renderer: String = ""
    )

    @Keep
    data class Memory(
        @SerializedName("total") val total: Long = 0L,
        @SerializedName("used") val used: Long = 0L
    )

    @Keep
    data class Thermal(
        @SerializedName("name") val name: String = "",
        @SerializedName("temp") val temperature: Double = 0.0
    )
}
