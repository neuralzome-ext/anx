package com.flomobility.hermes.assets.types

import com.felhr.usbserial.UsbSerialDevice
import com.flomobility.hermes.assets.AssetState
import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.BaseAsset
import com.flomobility.hermes.assets.BaseAssetConfig
import com.flomobility.hermes.common.Result

class UsbSerial : BaseAsset {

    private var _id: String = ""

    private val _config = Config()

    private var _state = AssetState.IDLE

    private var usbSerialDevice: UsbSerialDevice? = null

    companion object {
        fun create(usbSerialDevice: UsbSerialDevice): UsbSerial {
            return UsbSerial().apply {
                this._id = "${usbSerialDevice.deviceId}"
                this.usbSerialDevice = usbSerialDevice
            }
        }
    }

    override val id: String
        get() = _id

    override val type: AssetType
        get() = AssetType.USB_SERIAL

    override val config: BaseAssetConfig
        get() = _config

    override val state: AssetState
        get() = _state

    override fun updateConfig(config: BaseAssetConfig): Result {
        if (config !is UsbSerial.Config) {
            return Result(success = false, message = "unknown config type")
        }
        this._config.apply {
            baudRate.value = config.baudRate.value
            delimeter.value = config.delimeter.value
            portPub = config.portPub
            portSub = config.portSub
        }
        return Result(success = true)
    }

    override fun getDesc(): Map<String, Any> {
        val map = hashMapOf<String, Any>("id" to id)
        config.getFields().forEach { field ->
            map[field.name] = field.range
        }
        return map
    }

    override fun start(): Result {
        TODO("Not yet implemented")
    }

    override fun stop(): Result {
        TODO("Not yet implemented")
    }

    inner class Publisher : Runnable {

        override fun run() {
            // TODO
        }
    }

    class Config : BaseAssetConfig() {

        val baudRate = Field<Int>()

        val delimeter = Field<String>()

        init {
            baudRate.range = listOf(
                300,
                600,
                1200,
                2400,
                4800,
                9600,
                19200,
                38400,
                57600,
                115200,
                230400,
                460800,
                921600
            )
            baudRate.name = "baud"
            baudRate.value = DEFAULT_BAUD_RATE

            delimeter.range = listOf("\n", ",", ";")
            delimeter.name = "delimeter"
            delimeter.value = DEFAULT_DELIMETER
        }

        companion object {
            private const val DEFAULT_BAUD_RATE = 115200
            private const val DEFAULT_DELIMETER = "\n"
        }

        override fun getFields(): List<Field<*>> {
            return listOf(baudRate, delimeter)
        }
    }

}