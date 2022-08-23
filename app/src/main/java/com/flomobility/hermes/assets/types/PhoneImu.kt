package com.flomobility.hermes.assets.types

import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.BaseAsset
import com.flomobility.hermes.assets.BaseAssetConfig
import com.flomobility.hermes.common.Result
import com.google.gson.Gson
import org.json.JSONObject

class PhoneImu : BaseAsset {

    private val _config = Config()

    override val id: String
        get() = "in72e"

    override val type: AssetType
        get() = AssetType.IMU

    override val config: BaseAssetConfig
        get() = _config

    override fun updateConfig(config: BaseAssetConfig): Result {
        if (config !is PhoneImu.Config) {
            return Result(success = false, message = "Unknown config type")
        }

        this._config.apply {
            this.fps = config.fps
        }
        return Result(success = true)
    }

    override fun getDesc(): String {
        return JSONObject().apply {
            put("id", id)
            config.getFields().forEach { (key, field) ->
                put(key, field)
            }
        }.toString().replace("\\", "")
    }

    class Config: BaseAssetConfig {

        var fps: Int = 0

        private val fpsRange = listOf(1, 2, 5, 10, 15, 25, 30, 60, 75, 100, 125, 150, 200)

        override fun getFields(): Map<String, Any> {
            return mapOf(
                "fps" to fpsRange
            )
        }
    }

}