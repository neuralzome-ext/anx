package com.flomobility.anx.hermes.alerts

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun sendAlert(alert: Alert) {
        val intent = Intent(ANX_ALERTS_BROADCAST_INTENT)
        intent.putExtra(KEY_ANX_ALERT, alert)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    companion object {
        const val ANX_ALERTS_BROADCAST_INTENT = "com.flomobility.anx.broadcast.alerts"
        const val KEY_ANX_ALERT = "KEY_ANX_ALERTS"
    }

}
