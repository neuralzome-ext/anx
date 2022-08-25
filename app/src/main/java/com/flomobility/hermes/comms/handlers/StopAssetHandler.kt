package com.flomobility.hermes.comms.handlers

import com.flomobility.hermes.api.StandardResponse
import com.flomobility.hermes.assets.AssetManager
import com.flomobility.hermes.assets.getAssetTypeFromAlias
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
class StopAssetHandler @Inject constructor(
    private val assetManager: AssetManager,
    private val gson: Gson
): Runnable {
    lateinit var socket: ZMQ.Socket

    override fun run() {
        ZContext().use { ctx ->
            socket = ctx.createSocket(SocketType.REP)
            socket.bind(SocketManager.STOP_ASSET_SOCKET_ADDR)
            while (true) {
                try {
                    socket.recv(0)?.let { bytes ->
                        val msgStr = String(bytes, ZMQ.CHARSET)

                        // TODO handle stop asset
                        handleStopAssetReq(msgStr)
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    private fun handleStopAssetReq(reqStr: String) {
        val stopAssetReq = JSONObject(reqStr)
        val asset = stopAssetReq.getJSONObject("asset")
        val type = asset.getString("type")
        val id = asset.getString("id")

        val stopAsset = assetManager.stopAsset(id, getAssetTypeFromAlias(type))
        socket.send(
            gson.toJson(
                StandardResponse(
                    success = stopAsset.success,
                    message = stopAsset.message
                )
            ).toByteArray(ZMQ.CHARSET), 0
        )
    }
}