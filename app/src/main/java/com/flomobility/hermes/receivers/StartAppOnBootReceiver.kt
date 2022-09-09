package com.flomobility.hermes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flomobility.hermes.other.exec

class StartAppOnBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
            exec(cmd = "am start com.flomobility.hermes/com.flomobility.hermes.MainActivity")
        }
    }
}