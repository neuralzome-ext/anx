package com.flomobility.hermes.model

data class AssetsModel(
    val sensorImage: Int,
    val sensorName: String,
    val sensorStatuses: ArrayList<AssetsStatusModel> = arrayListOf(),
    val isAvailable: Boolean = true
)