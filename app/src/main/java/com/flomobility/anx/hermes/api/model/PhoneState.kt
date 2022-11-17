package com.flomobility.anx.hermes.api.model


import com.google.gson.annotations.SerializedName

data class PhoneState(
    @SerializedName("charging")
    var charging: Boolean = false,
    @SerializedName("cpu_ram_usage")
    var cpuRamUsage: Double = 0.0,
    @SerializedName("cpu_temp")
    var cpuTemp: Double = 0.0,
    @SerializedName("cpu_usage")
    var cpuUsage: Double = 0.0,
    @SerializedName("gpu_usage")
    var gpuUsage: Double = 0.0,
    @SerializedName("gpu_vram_usage")
    var gpuVramUsage: Double = 0.0
)