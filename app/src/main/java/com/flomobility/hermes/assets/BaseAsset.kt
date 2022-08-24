package com.flomobility.hermes.assets

import com.flomobility.hermes.common.Result


interface BaseAsset {

    val id: String

    val type: AssetType

    val config: BaseAssetConfig

    val state: AssetState

    fun updateConfig(config: BaseAssetConfig): Result

    fun getDesc(): String

    fun startPublishing(): Result

    fun stopPublishing(): Result

}