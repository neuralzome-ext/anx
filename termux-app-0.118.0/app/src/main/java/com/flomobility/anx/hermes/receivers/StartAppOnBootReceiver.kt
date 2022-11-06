package com.flomobility.anx.hermes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flomobility.anx.hermes.other.runAsRoot

class StartAppOnBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
            runAsRoot(cmd = "am start com.flomobility.anx.hermes/com.flomobility.anx.hermes.MainActivity")
        }
    }
}
