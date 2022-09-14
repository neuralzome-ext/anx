package com.flomobility.hermes.assets.types

import android.content.Context
import android.speech.tts.TextToSpeech
import com.flomobility.hermes.api.model.Raw
import com.flomobility.hermes.assets.AssetState
import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.BaseAsset
import com.flomobility.hermes.assets.BaseAssetConfig
import com.flomobility.hermes.common.Result
import com.flomobility.hermes.other.Constants
import com.flomobility.hermes.other.GsonUtils
import com.flomobility.hermes.other.handleExceptions
import dagger.hilt.android.qualifiers.ApplicationContext
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Speaker @Inject constructor(
    @ApplicationContext private val context: Context
) : BaseAsset, TextToSpeech.OnInitListener {

    private val _id: String = "0"

    private val _state = AssetState.IDLE

    private val _config = Speaker.Config()

    private var speakerThread: SpeakerThread? = null

    private lateinit var t2s: TextToSpeech

    init {
        initalize()
    }

    private fun initalize() {
        t2s = TextToSpeech(
            context,
            this,
            "com.google.android.tts"
        )
    }

    override val id: String
        get() = _id

    override val type: AssetType
        get() = AssetType.SPEAKER

    override val config: BaseAssetConfig
        get() = _config

    override val state: AssetState
        get() = _state

    override fun updateConfig(config: BaseAssetConfig): Result {
        if (config !is Speaker.Config) {
            return Result(
                success = false,
                message = "Need config of type ${Speaker.Config::class.simpleName}, received ${config::class.simpleName}"
            )
        }

        this._config.apply {
            language.value = config.language.value
            portSub = config.portSub
            connectedDeviceIp = config.connectedDeviceIp
        }
        return Result(success = true)
    }

    override fun start(): Result {
        handleExceptions(catchBlock = { e ->
            Timber.e(e)
            return Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            t2s.voice = t2s.voices.find { it.locale.language == _config.language.value}
            speakerThread = SpeakerThread()
            speakerThread?.start()
            return Result(success = true)
        }
        return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
    }

    override fun stop(): Result {
        handleExceptions(catchBlock = { e ->
            return Result(success = false, message = e.message ?: Constants.UNKNOWN_ERROR_MSG)
        }) {
            speakerThread?.interrupt?.set(true)
            speakerThread = null
            return Result(success = true)
        }
        return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
    }

    override fun destroy() {
        speakerThread = null
    }

    inner class SpeakerThread : Thread() {

        lateinit var socket: ZMQ.Socket

        val interrupt = AtomicBoolean(false)

        init {
            name = "${type.alias}-${this@Speaker.id}-listener-thread"
        }

        override fun run() {
            try {
                val address = "tcp://${config.connectedDeviceIp}:${config.portSub}"
                Timber.i("[${this@Speaker.name}] - Starting subscriber on $address")
                ZContext().use { ctx ->
                    socket = ctx.createSocket(SocketType.SUB)
                    socket.connect(address)
                    socket.subscribe("")
                    while (!interrupt.get()) {
                        try {
                            val recvBytes = socket.recv(ZMQ.DONTWAIT) ?: continue
                            val rawData = GsonUtils.getGson().fromJson<Raw>(
                                String(recvBytes, ZMQ.CHARSET),
                                Raw.type
                            )
                            speak(rawData.data)
                        } catch (e: Exception) {
                            Timber.e(e)
                            return
                        }
                    }
                }
                Timber.i("[${this@Speaker.name}] - Stopping subscriber on $address")
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    class Config : BaseAssetConfig() {

        val language = Field<String>()

        init {
            language.name = "language"
        }

        override fun getFields(): List<Field<*>> {
            return listOf(language/*, voice*/)
        }
    }

    override fun onInit(p0: Int) {
        if (p0 != TextToSpeech.SUCCESS) {
            Timber.e("Speaker Initilization Failed!")
            return
        }
        Timber.i("Speaker initialzed!")
        this._config.language.updateRange(t2s.voices.map { it.locale.language })
    }

    private fun speak(msg: String) {
        t2s.speak(msg, TextToSpeech.QUEUE_ADD, null)
    }
}