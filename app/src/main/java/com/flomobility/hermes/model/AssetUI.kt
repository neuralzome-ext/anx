package com.flomobility.hermes.model

import androidx.annotation.DrawableRes
import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.BaseAsset

data class AssetUI(
    @DrawableRes
    val assetImage: Int,
    val assetType: AssetType,
    var assets: List<BaseAsset> = arrayListOf(),
    val isAvailable: Boolean = true
)