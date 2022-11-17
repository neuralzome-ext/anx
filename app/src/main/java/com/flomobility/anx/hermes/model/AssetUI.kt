package com.flomobility.anx.hermes.model

import androidx.annotation.DrawableRes
import com.flomobility.anx.hermes.assets.AssetType
import com.flomobility.anx.hermes.assets.BaseAsset

data class AssetUI(
    @DrawableRes
    val assetImage: Int,
    val assetType: AssetType,
    var assets: List<BaseAsset> = arrayListOf(),
    val isAvailable: Boolean = true
)