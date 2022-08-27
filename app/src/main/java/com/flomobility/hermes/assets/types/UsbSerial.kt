package com.flomobility.hermes.assets.types

import com.felhr.usbserial.UsbSerialDevice
import com.flomobility.hermes.assets.AssetState
import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.BaseAsset
import com.flomobility.hermes.assets.BaseAssetConfig
import com.flomobility.hermes.common.Result
import com.flomobility.hermes.other.Constants
import com.flomobility.hermes.other.handleExceptions
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber

class UsbSerial : BaseAsset {

    private var _id: String = ""

    private val _config = Config()

    private var _state = AssetState.IDLE

    private var usbSerialDevice: UsbSerialDevice? = null

    private var readerThread: Thread? = null

    private var writerThread: Thread? = null

    private val delimeter: Byte
        get() = (_config.delimeter.value as String).toCharArray()[0].code.toByte()

    companion object {
        fun create(usbSerialDevice: UsbSerialDevice): UsbSerial {
            return UsbSerial().apply {
                this._id = "${usbSerialDevice.deviceId}"
                this.usbSerialDevice = usbSerialDevice
            }
        }

        private const val BUFFER_SIZE_IN_BYTES = 2048
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
        // close usb serial and open it with new baud rate
        handleExceptions(catchBlock = { e->
            return Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            if (usbSerialDevice?.isOpen!!)
                usbSerialDevice?.syncClose()

            usbSerialDevice?.setBaudRate(_config.baudRate.value as Int)
            usbSerialDevice?.syncOpen()

            readerThread = Thread(Reader(), "${type.alias}-$id-reader-thread")
            readerThread?.start()

            writerThread = Thread(Writer(), "${type.alias}-$id-writer-thread")
            writerThread?.start()

            return Result(success = true)
        }
        return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
    }

    override fun stop(): Result {
        handleExceptions(catchBlock = { e->
            return Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            usbSerialDevice?.syncClose()
            readerThread?.interrupt()
            writerThread?.interrupt()
            readerThread = null
            writerThread = null

            return Result(success = true)
        }
        return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
    }

    inner class Reader : Runnable {

        lateinit var socket: ZMQ.Socket

        override fun run() {
            val address = "tcp://*:${config.portPub}"
            val inputStream = usbSerialDevice?.inputStream
            try {
                ZContext().use { ctx ->
                    socket = ctx.createSocket(SocketType.PUB)
                    socket.bind(address)
                    while(!Thread.currentThread().isInterrupted) {
                        val bytes = ByteArray(size = BUFFER_SIZE_IN_BYTES)
                        val ret = inputStream?.read(bytes)
                        if(ret == -1) {
                            Timber.d("Error reading from ${type.alias}-$id")
                            continue
                        }
                        val idxDelimeter = mutableListOf<Int>()
                        bytes.forEachIndexed { index, byte ->
                            if(byte == delimeter) {
                                idxDelimeter.add(index)
                            }
                        }
                        if (idxDelimeter.isEmpty()) {
                            socket.send(bytes, 0)
                            continue
                        }
                        var offset = 0
                        idxDelimeter.forEach { idx ->
                            val filteredBytes = bytes.copyOfRange(offset, idx)
                            socket.send(filteredBytes, 0)
                            offset += 1
                        }

                        if(offset < bytes.size) {
                            val filteredBytes = bytes.copyOfRange(offset, bytes.size)
                            socket.send(filteredBytes, 0)
                        }
                    }
                }
            } catch (e: InterruptedException) {
                Timber.i("${type.alias}-$id publisher on port:${_config.portPub} closed")
                socket.unbind(address)
                socket.close()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    inner class Writer : Runnable {

        lateinit var socket: ZMQ.Socket

        override fun run() {
            val address = "tcp://*:${config.portSub}"
            val outputStream = usbSerialDevice?.outputStream
            try {
                ZContext().use { ctx ->
                    socket = ctx.createSocket(SocketType.SUB)
                    socket.bind(address)
                    while (!Thread.currentThread().isInterrupted) {
                        val recvBytes = socket.recv(0) ?: continue
                        var bytes = ByteArray(recvBytes.size + 1)
                        bytes += recvBytes
                        bytes += delimeter
                        outputStream?.write(bytes)
                    }
                }
            } catch (e: InterruptedException) {
                Timber.i("${type.alias}-$id subscriber on port:${_config.portSub} closed")
                socket.unbind(address)
                socket.close()
            } catch (e: Exception) {
                Timber.e(e)
            }
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