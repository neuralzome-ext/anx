package com.flomobility.hermes.model

data class SensorModel(
    val sensorImage: Int,
    val sensorName: String,
    val sensorStatuses: ArrayList<SensorStatusModel>? = null,
    val isAvailable: Boolean = true
)