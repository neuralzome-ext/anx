package com.flomobility.anx.assets.gnss

import android.content.Context
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.annotation.RequiresApi
import com.flomobility.anx.assets.Asset
import com.flomobility.anx.common.Result
import com.flomobility.anx.proto.Assets
import com.flomobility.anx.proto.Common
import dagger.hilt.android.qualifiers.ApplicationContext
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import com.flomobility.anx.common.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
@Singleton
class DeviceGnss @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceGnssManager: DeviceGnssManager
) : Asset<Common.Empty>(), OnNmeaMessageListener {

    companion object {
        const val TAG = "DeviceGNSS"
        private const val MSG_NMEA_DATA = 10
    }

    private val locationManager by lazy {
        context.getSystemService(
            LocationManager::class.java
        ) as LocationManager
    }

    private val gnssData = Assets.GnssData.newBuilder()

    private var nmeaMsgThread: NMEAMessageThread? = null

    lateinit var socket: ZMQ.Socket

    fun getDeviceGnssSelect(): Assets.DeviceGnssSelect {
        if (getGpsProvider()) {
            return Assets.DeviceGnssSelect.newBuilder().apply {
                this.available = true
            }.build()
        } else {
            return Assets.DeviceGnssSelect.newBuilder().apply {
                this.available = false
            }.build()
        }
    }

    private fun getGpsProvider(): Boolean {
        val isGpsProviderEnabled: Boolean =
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (isGpsProviderEnabled) {
            return true
        }
        return false
    }

    override fun onNmeaMessage(nmeadata: String?, timestamp: Long) {
        gnssData.apply {
            nmea = nmeadata.toString()
        }

        nmeaMsgThread?.publishNMEAData(gnssData.build())
    }

    override fun start(options: Common.Empty?): Result {
        try {
            GlobalScope.launch(Dispatchers.Main) {
                deviceGnssManager.init(this@DeviceGnss)
            }
            nmeaMsgThread = NMEAMessageThread()
            nmeaMsgThread?.start()
            return Result(success = true)
        } catch (e: Exception) {
            Timber.e(e)
            return Result(success = false, message = "Gnss initialization failed")
        }
    }


    override fun stop(): Result {
        GlobalScope.launch(Dispatchers.Main) {
            deviceGnssManager.stop(this@DeviceGnss)
        }
        nmeaMsgThread?.kill()
        nmeaMsgThread = null
        return Result(success = true, message = "Gnss thread stopped")
    }


    inner class NMEAMessageThread : Thread() {

        init {
            name = "device-gnss-NMEAMessage-thread"
        }

        private var handler: NMEAMessageThreadHandler? = null

        private lateinit var socket: ZMQ.Socket

        private var address = "tcp://localhost:10004"


        override fun run() {
            while (address.isEmpty()) {
                continue
            }
            Timber.tag(TAG).d("Starting Gnss Publisher on $address")
            try {
                ZContext().use { ctx ->
                    socket = ctx.createSocket(SocketType.PUB)
                    socket.bind(address)
                    // wait to bind
                    Thread.sleep(500L)
                    Looper.prepare()
                    handler = NMEAMessageThreadHandler(Looper.myLooper() ?: return)
                    Looper.loop()
                }
                socket.unbind(address)
                Thread.sleep(500L)
                Timber.tag(TAG).d("Stopping Gnss Publisher on $address")
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        fun publishNMEAData(nmea: Assets.GnssData) {
            handler?.sendMsg(MSG_NMEA_DATA, nmea)
        }

        fun kill() {
            handler?.sendMsg(Constants.SIG_KILL_THREAD)
        }

        inner class NMEAMessageThreadHandler(private val myLooper: Looper) : Handler(myLooper) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_NMEA_DATA -> {
                        val gnssData = msg.obj as Assets.GnssData
                        gnssData.let {
                            socket.send(gnssData.toByteArray(), 0)
                        }
                    }
                    Constants.SIG_KILL_THREAD -> {
                        myLooper.quitSafely()
                    }
                }
            }

            fun sendMsg(what: Int, obj: Any? = null) {
                sendMessage(obtainMessage(what, obj))
            }
        }
    }

}
