package com.flomobility.anx.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.flomobility.anx.activity.MainActivity

class BootReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Received on boot complete")
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Starting MainActivity")
            val i = Intent(context, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context?.startActivity(i)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
