package com.flomobility.hermes.other

import com.flomobility.hermes.BuildConfig.*
import com.flomobility.hermes.HermesApplication

object Constants {
    const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
    const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"
    const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

    const val NOTIFICATION_CHANNEL_ID = "hermes_service_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Hermes Comms"
    const val NOTIFICATION_ID = 1

    const val UNKNOWN_ERROR_MSG = "An unknown error occurred"

    const val SIG_KILL_THREAD = 1011

    const val SOCKET_BIND_DELAY_IN_MS = 500L

    const val RPC_DEFAULT_TIMEOUT_IN_MS = 5000

    const val USER_TOKEN = "token"
    const val DEVICE_ID = "deviceId"
    const val DEVICE_EXPIRY = "deviceExpiry"
    const val INSTALL_STATUS = "installStatus"
    const val ON_BOOT = "onBoot"

    val BASE_URL: String = when (BUILD_TYPE) {
        HermesApplication.BuildType.DEV -> FLO_DEVELOPMENT_URL
        HermesApplication.BuildType.STAGING -> FLO_STAGING_URL
        HermesApplication.BuildType.RELEASE -> FLO_PRODUCTION_URL
        else -> FLO_DEVELOPMENT_URL
    }
}