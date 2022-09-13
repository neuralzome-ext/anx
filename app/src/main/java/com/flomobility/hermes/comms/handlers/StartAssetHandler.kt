package com.flomobility.hermes.comms.handlers

import android.os.Build
import androidx.annotation.RequiresApi
import com.flomobility.hermes.api.StandardResponse
import com.flomobility.hermes.assets.AssetManager
import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.getAssetTypeFromAlias
import com.flomobility.hermes.assets.types.Phone
import com.flomobility.hermes.assets.types.PhoneImu
import com.flomobility.hermes.assets.types.UsbSerial
import com.flomobility.hermes.comms.SocketManager
import com.flomobility.hermes.other.Constants
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
    private val assetManager: AssetManager
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

        val portPub = startAssetReq.getJSONObject("port").getInt("pub")
        var portSub: Int = -1
        if (startAssetReq.getJSONObject("port").has("sub"))
            portSub = startAssetReq.getJSONObject("port").getInt("sub")

        val assetType = getAssetTypeFromAlias(type)
        val meta = asset.getJSONObject("meta")
        val id = meta.getString("id")
        val config = when (assetType) {
            AssetType.IMU -> {
                PhoneImu.Config()
            }
            AssetType.USB_SERIAL -> {
                UsbSerial.Config()
            }
            AssetType.PHONE -> {
                Phone.Config()
            }
            AssetType.UNK -> throw IllegalArgumentException("Unknown asset type")
            else -> throw IllegalArgumentException("Unknown asset type")
        }

        kotlin.run lit@{
            meta.keys().forEach { key ->
                if(key == "id") {
                    return@lit
                }
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
                // TODO update field in config with value
                val inRange = field.inRange(fieldValue/*, field::value::class*/)
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
                field.value = fieldValue
            }
        }
        config.apply {
            this.portPub = portPub
            this.portSub = portSub
        }

        val updateAssetConfig = assetManager.updateAssetConfig(id, assetType, config)

        val startAsset = assetManager.startAsset(id, assetType)
        val resp = StandardResponse().apply {
            success = startAsset.success
            message = startAsset.message
        }
        socket.send(gson.toJson(resp).toByteArray(ZMQ.CHARSET), 0)
    }
}