package com.flomobility.hermes.assets

import android.os.Handler
import android.os.Looper
import android.os.Message
import com.flomobility.hermes.assets.types.Phone
import com.flomobility.hermes.assets.types.PhoneImu
import com.flomobility.hermes.assets.types.Speaker
import com.flomobility.hermes.common.Result
import com.flomobility.hermes.comms.SessionManager
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
    private val sessionManager: SessionManager,
    private val phoneImu: PhoneImu,
    private val phone: Phone,
    private val speaker: Speaker
) {

    private val _assets = mutableListOf<BaseAsset>()
    val assets: List<BaseAsset> = _assets

    private val assetsStatePublisherContext = ZContext()

    private var assetsStatePublisherThread: AssetsStatePublisher? = null

    fun init() {
        assetsStatePublisherThread = AssetsStatePublisher()
        assetsStatePublisherThread?.start()

        // Add inbuilt assets here
        addAsset(phoneImu)
        addAsset(phone)
        addAsset(speaker)
    }

    fun addAsset(asset: BaseAsset): Result {
        if (assets.find { it.id == asset.id && it.type == asset.type } != null) {
            return Result(
                success = false,
                message = "${asset.type} Asset with ${asset.id} already exists"
            )
        }
        _assets.add(asset)
        Timber.i("Registered asset ${asset.id} of type ${asset.type.alias}")
        publishAssetState()
        return Result(success = true)
    }

    private fun getAssets(): String {
        val assetsMap = hashMapOf<String, List<Map<String, Any>>>()
        AssetType.values().forEach { type ->
            val assets = this._assets.filter { it.type == type }.map { it.getDesc() }
            if (assets.isNotEmpty()) {
                assetsMap[type.alias] = assets
            }
        }
        return gson.toJson(assetsMap)
    }

    fun publishAssetState() {
        if (!sessionManager.connected) return
        assetsStatePublisherThread?.publishAssetState(getAssets())
    }

    fun updateAssetConfig(id: String, assetType: AssetType, config: BaseAssetConfig): Result {
        val asset = _assets.find { it.id == id && it.type == assetType }
            ?: throw IllegalStateException("Couldn't find asset")
        return asset.updateConfig(config.apply {
            this.connectedDeviceIp = sessionManager.connectedDeviceIp
        })
    }

    fun startAsset(id: String, assetType: AssetType): Result {
        val asset = _assets.find { it.id == id && it.type == assetType }
            ?: throw IllegalStateException("Couldn't find asset")
        return asset.start()
    }

    fun stopAsset(id: String, assetType: AssetType): Result {
        val asset = _assets.find { it.id == id && it.type == assetType }
            ?: throw IllegalStateException("Couldn't find asset")
        return asset.stop()
    }

    fun removeAsset(id: String, assetType: AssetType): Result {
        val asset = _assets.find { it.id == id && it.type == assetType }
            ?: throw IllegalStateException("Couldn't find asset")
        asset.stop()
        asset.destroy()
        _assets.remove(asset)
        Timber.i("UnRegistered asset ${asset.id} of type ${asset.type.alias}")
        publishAssetState()
        return Result(success = true)
    }

    fun stopAllAssets(): Result {
        handleExceptions(catchBlock = { e ->
            Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            _assets.forEach {
                it.stop()
            }
            return Result(success = true)
        }
        return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
    }

    inner class AssetsStatePublisher : Thread() {

        private var handler: PublisherHandler? = null

        private lateinit var publisher: ZMQ.Socket

        init {
            name = "assets-state-publisher-thread"
        }

        override fun run() {
            try {
                publisher = assetsStatePublisherContext.createSocket(SocketType.PUB)
                publisher.bind(ASSETS_STATE_PUBLISHER_ADDR)
                // wait for socket to bind
                sleep(500)
                Looper.prepare()
                handler = PublisherHandler(Looper.myLooper() ?: return)
                Looper.loop()
            } catch (e: Exception) {
                Timber.e("Exception in $name caught : $e")
            } finally {
                publisher.unbind(ASSETS_STATE_PUBLISHER_ADDR)
                sleep(500)
                publisher.close()
            }
        }

        fun publishAssetState(assetsState: String) {
            handler?.publishAssetState(assetsState)
        }

        inner class PublisherHandler(looper: Looper) : Handler(looper) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_PUBLISH_ASSET_STATE -> {
                        val assetState = msg.obj as String
                        Timber.d("Publishing asset state $assetState")
                        publisher.send(assetState.toByteArray(ZMQ.CHARSET), 0)
                    }
                }
            }

            fun publishAssetState(assetsState: String) {
                sendMessage(obtainMessage(MSG_PUBLISH_ASSET_STATE, assetsState))
            }
        }
    }

    companion object {
        const val ASSETS_STATE_PUBLISHER_ADDR = "tcp://*:10003"
        const val MSG_PUBLISH_ASSET_STATE = 1
    }

}