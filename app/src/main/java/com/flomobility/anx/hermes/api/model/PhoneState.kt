package com.flomobility.anx.hermes.api.model


import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class PhoneStates(
    @SerializedName("battery") val battery: Battery = Battery(),
    @SerializedName("cpu") val cpu: Cpu = Cpu(),
    @SerializedName("gpu_usage") val gpuUsage: Double = 0.0,
    @SerializedName("gpu_vram_usage") val gpuVramUsage: Double = 0.0,
    @SerializedName("ram") val ram: Ram = Ram(),
    @SerializedName("storage") val storage: Storage = Storage(),
    @SerializedName("thermals") val thermals: List<Thermal> = listOf(),
    @SerializedName("uptime") val uptime: Double = 0.0
) {
    @Keep
    data class Battery(
        @SerializedName("charging") val charging: Boolean = false,
        @SerializedName("current") val current: Int = 0,
        @SerializedName("level") val level: Int = 0,
        @SerializedName("temp") val temp: Int = 0,
        @SerializedName("voltage") val voltage: Int = 0
    )

    @Keep
    data class Cpu(
        @SerializedName("architectures") val architectures: String = "",
        @SerializedName("cpu_freq") val cpuFreq: List<CpuFreq> = listOf(),
        @SerializedName("processor") val processor: String = "",
        @SerializedName("type") val type: String = ""
    ) {
        @Keep
        data class CpuFreq(
            @SerializedName("current") val current: Double = 0.0,
            @SerializedName("maximum") val maximum: Double = 0.0,
            @SerializedName("minimum") val minimum: Double = 0.0
        )
    }

    @Keep
    data class Ram(
        @SerializedName("total") val total: Double = 0.0,
        @SerializedName("used") val used: Double = 0.0
    )

    @Keep
    data class Storage(
        @SerializedName("available") val available: Double = 0.0,
        @SerializedName("total") val total: Double = 0.0
    )

    @Keep
    data class Thermal(
        @SerializedName("name") val name: String = "",
        @SerializedName("thermal") val thermal: Double = 0.0
    )
}
