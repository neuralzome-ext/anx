package com.flomobility.hermes.assets

import com.flomobility.hermes.common.Result


interface BaseAsset {

    val id: String

    val type: AssetType

    val config: BaseAssetConfig

    val state: AssetState

    fun updateConfig(config: BaseAssetConfig): Result

    fun getDesc(): Map<String, Any>

    fun start(): Result

    fun stop(): Result

    fun destroy()

}