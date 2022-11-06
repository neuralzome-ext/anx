package com.flomobility.anx.hermes.comms.handlers

import android.os.Build
import androidx.annotation.RequiresApi
import com.flomobility.anx.hermes.api.StandardResponse
import com.flomobility.anx.hermes.assets.AssetManager
import com.flomobility.anx.hermes.assets.getAssetTypeFromAlias
import com.flomobility.anx.hermes.comms.SessionManager
import com.flomobility.anx.hermes.comms.SocketManager
import com.flomobility.anx.hermes.other.Constants
import com.google.gson.Gson
import org.json.JSONObject
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class StartAssetHandler @Inject constructor(
    private val gson: Gson,
    private val assetManager: AssetManager,
    private val sessionManager: SessionManager
) : Runnable {

    lateinit var socket: ZMQ.Socket

    override fun run() {
        try {
            ZContext().use { ctx ->
                socket = ctx.createSocket(SocketType.REP)
                socket.bind(SocketManager.START_ASSET_SOCKET_ADDR)
                Timber.i("Start asset handler running on ${SocketManager.START_ASSET_SOCKET_ADDR}")
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        socket.recv(0)?.let { bytes ->
                            val msgStr = String(bytes, ZMQ.CHARSET)
                            Timber.d("[Start-Asset] -- Request : $msgStr")
                            if (!sessionManager.connected) {
                                throw IllegalStateException("Cannot start asset without being subscribed! Subscribe first")
                            }
                            handleStartAssetRequest(msgStr)
                        }
                    } catch (e: Exception) {
                        Timber.e("Error in start asset handler : $e")
                        socket.send(
                            gson.toJson(
                                StandardResponse(
                                    success = false,
                                    message = e.message ?: Constants.UNKNOWN_ERROR_MSG
                                )
                            ).toByteArray(ZMQ.CHARSET), 0
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("Error in start asset handler : $e")
            socket.send(
                gson.toJson(
                    StandardResponse(
                        success = false,
                        message = e.message ?: Constants.UNKNOWN_ERROR_MSG
                    )
                ).toByteArray(ZMQ.CHARSET), 0
            )
        }
    }

    private fun handleStartAssetRequest(reqStr: String) {
        val startAssetReq = JSONObject(reqStr)
        val asset = startAssetReq.getJSONObject("asset")
        val type = asset.getString("type")

        var portPub = -1
        if (startAssetReq.getJSONObject("port").has("pub"))
            portPub = startAssetReq.getJSONObject("port").getInt("pub")

        var portSub: Int = -1
        if (startAssetReq.getJSONObject("port").has("sub"))
            portSub = startAssetReq.getJSONObject("port").getInt("sub")

        val assetType = getAssetTypeFromAlias(type)
        val meta = asset.getJSONObject("meta")
        val id = meta.getString("id")
        val config = assetManager.assets.find { it.id == id && it.type == assetType }?.config
            ?: throw IllegalArgumentException("Asset $id of type $type doesn't exist")
        meta.keys().forEach { key ->
            if (key != "id") {
                val field = config.findField(key)
                if (field == null) {
                    socket.send(
                        gson.toJson(
                            StandardResponse(
                                success = false,
                                message = "$key field doesn't exist for asset-type -- $type"
                            )
                        ).toByteArray(ZMQ.CHARSET), 0
                    )
                    return
                }
                val fieldValue = meta.get(key)
                val inRange = field.inRange(fieldValue)
                if (!inRange.success) {
                    socket.send(
                        gson.toJson(
                            StandardResponse(
                                success = false,
                                message = "Value $fieldValue passed for $key is invalid"
                            )
                        ).toByteArray(ZMQ.CHARSET), 0
                    )
                    return
                }
                field.updateValue(fieldValue)
            }
        }

        config.apply {
            this.portPub = portPub
            this.portSub = portSub
        }

        val updateAssetConfig = assetManager.updateAssetConfig(id, assetType, config)
        if(!updateAssetConfig.success) {
            val resp = StandardResponse(success = false, updateAssetConfig.message)
            socket.send(gson.toJson(resp).toByteArray(ZMQ.CHARSET), 0)
            return
        }

        val startAsset = assetManager.startAsset(id, assetType)
        val resp = StandardResponse().apply {
            success = startAsset.success
            message = startAsset.message
        }
        socket.send(gson.toJson(resp).toByteArray(ZMQ.CHARSET), 0)
    }
}
