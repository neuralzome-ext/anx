package com.flomobility.hermes.assets

import com.flomobility.hermes.common.Result


interface BaseAsset {

    val id: String

    val type: AssetType

    val config: BaseAssetConfig

    val state: AssetState

    fun updateConfig(config: BaseAssetConfig): Result

    fun getDesc(): Map<String, Any> {
        val map = hashMapOf<String, Any>("id" to id)
        config.getFields().forEach { field ->
            map[field.name] = field.range
        }
        return map
    }

    fun start(): Result

    fun stop(): Result

    fun destroy()

}