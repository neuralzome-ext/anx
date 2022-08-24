package com.flomobility.hermes.comms.handlers

import com.flomobility.hermes.api.StandardResponse
import com.flomobility.hermes.assets.AssetManager
import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.getAssetTypeFromAlias
import com.flomobility.hermes.assets.types.PhoneImu
import com.flomobility.hermes.comms.SocketManager
import com.google.gson.Gson
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartAssetHandler @Inject constructor(
    private val gson: Gson,
    private val assetManager: AssetManager
) : Runnable {

    lateinit var socket: ZMQ.Socket

    override fun run() {
        ZContext().use { ctx ->
            socket = ctx.createSocket(SocketType.REP)
            socket.bind(SocketManager.START_ASSET_SOCKET_ADDR)
            while (true) {
                try {
                    socket.recv(0)?.let { bytes ->
                        val msgStr = String(bytes, ZMQ.CHARSET)
                        Timber.d("[Start-Asset] -- Request : $msgStr")
                        handleStartAssetRequest(msgStr)
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    private fun handleStartAssetRequest(reqStr: String) {
        val startAssetReq = JSONObject(reqStr)
        val asset = startAssetReq.getJSONObject("asset")
        val type = asset.getString("type")

        val portPub = startAssetReq.getJSONObject("port").getInt("pub")
        var portSub: Int = -1
        if (startAssetReq.getJSONObject("port").has("sub"))
            portSub = startAssetReq.getJSONObject("port").getInt("sub")

        val assetType = getAssetTypeFromAlias(type)
        val meta = asset.getJSONObject("meta")
        val id = meta.getString("id")
        val config = when (assetType) {
            AssetType.IMU -> {
                val config = PhoneImu.Config()
                meta.keys().forEach { key ->
                    // TODO add reflection property
                    if (key == "fps")
                        config.fps = meta.getInt(key)
                }
                config.apply {
                    this.portPub = portPub
                    this.portSub = portSub
                }
                config
            }
            AssetType.UNK -> throw IllegalArgumentException("Unknown asset type")
        }
        assetManager.updateAssetConfig(id, assetType, config)
        val res = assetManager.startAsset(id, assetType)
        val resp = StandardResponse().apply {
            success = res.success
            message = res.message
        }
        socket.send(gson.toJson(resp).toByteArray(ZMQ.CHARSET), 0)
    }
}