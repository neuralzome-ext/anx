package com.flomobility.hermes.model

data class AssetsModel(
    val assetImage: Int,
    val assetName: String,
    var assetStatuses: ArrayList<AssetsStatusModel> = arrayListOf(),
    val isAvailable: Boolean = true
)