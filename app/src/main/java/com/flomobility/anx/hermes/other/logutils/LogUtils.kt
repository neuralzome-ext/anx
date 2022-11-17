package com.flomobility.anx.hermes.other.logutils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashReportingTree : Timber.Tree() {

    companion object {
        private const val CRASHLYTICS_KEY_PRIORITY = "priority"
        private const val CRASHLYTICS_KEY_TAG = "tag"
        private const val CRASHLYTICS_KEY_MESSAGE = "message"
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }

        val throwable = t ?: Throwable(message)

        FirebaseCrashlytics.getInstance().apply {
            setCustomKey(CRASHLYTICS_KEY_PRIORITY, priority)
            setCustomKey(CRASHLYTICS_KEY_TAG, tag ?: "NO_TAG");
            setCustomKey(CRASHLYTICS_KEY_MESSAGE, message);
            recordException(throwable)
            log("$tag $message")
            if (didCrashOnPreviousExecution()) {
                sendUnsentReports()
            }
        }


    }

}
