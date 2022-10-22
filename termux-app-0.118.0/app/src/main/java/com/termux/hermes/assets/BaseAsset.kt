package com.termux.hermes.assets

import com.termux.hermes.common.Result


interface BaseAsset {

    val id: String

    val type: AssetType

    val config: BaseAssetConfig

    val state: AssetState

    val name: String
        get() = "${type.alias}-$id"

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
