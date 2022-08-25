package com.flomobility.hermes.assets

import com.flomobility.hermes.assets.types.PhoneImu
import com.flomobility.hermes.common.Result
import com.flomobility.hermes.other.Constants
import com.flomobility.hermes.other.handleExceptions
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.Gson
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber

@Singleton
class AssetManager @Inject constructor(
    private val gson: Gson,
    private val phoneImu: PhoneImu
) {

    val assets = listOf<BaseAsset>(
        phoneImu
    )

    private fun getAssets(): String {
        val assetsMap = hashMapOf<String, List<Map<String, Any>>>()
        AssetType.values().forEach { type ->
            val assets = this.assets.filter { it.type == type }.map { it.getDesc() }
            if (assets.isNotEmpty()) {
                assetsMap[type.alias] = assets
            }
        }
        return gson.toJson(assetsMap)
    }

    fun publishAssetState() {
        Thread(AssetsStatePublisher(getAssets()), "assets-state-publisher").start()
    }

    fun updateAssetConfig(id: String, assetType: AssetType, config: BaseAssetConfig): Result {
        val asset = assets.find { it.id == id && it.type == assetType }
            ?: throw IllegalStateException("Couldn't find asset")
        return asset.updateConfig(config)
    }

    fun startAsset(id: String, assetType: AssetType): Result {
        val asset = assets.find { it.id == id && it.type == assetType }
            ?: throw IllegalStateException("Couldn't find asset")
        return asset.start()
    }

    fun stopAsset(id: String, assetType: AssetType): Result {
        val asset = assets.find { it.id == id && it.type == assetType }
            ?: throw IllegalStateException("Couldn't find asset")
        return asset.stop()
    }

    fun stopAllAssets(): Result {
        handleExceptions(catchBlock = { e->
            Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            assets.forEach {
                it.stop()
            }
            return Result(success = true)
        }
        return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
    }

    class AssetsStatePublisher(private val assetState: String) : Runnable {

        lateinit var publisher: ZMQ.Socket

        override fun run() {
            Timber.d("Publishing asset state $assetState")
            ZContext().use { ctx ->
                publisher = ctx.createSocket(SocketType.PUB)
                publisher.bind(ASSETS_STATE_PUBLISHER_ADDR)
                // sleep so that publisher is bound
                Thread.sleep(500)
                publisher.send(assetState, 0)
                publisher.unbind(ASSETS_STATE_PUBLISHER_ADDR)
                publisher.close()
            }
        }

        companion object {
            const val ASSETS_STATE_PUBLISHER_ADDR = "tcp://*:10003"
        }
    }

}