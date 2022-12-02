package com.flomobility.anx.hermes.other

import com.flomobility.anx.BuildConfig.*
import com.flomobility.anx.FloApplication

object Constants {
    const val ACTION_START_OR_RESUME_SERVICE = "ACTION_START_OR_RESUME_SERVICE"
    const val ACTION_PAUSE_SERVICE = "ACTION_PAUSE_SERVICE"
    const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
    const val ACTION_STOP_AND_EXIT = "ACTION_STOP_AND_EXIT"

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
    const val KEY_ACCEPT_LICENSE = "KEY_ACCEPT_LICENSE"
    const val KEY_EMAIL = "KEY_EMAIL"

    const val FILES_SYSTEM_FILE_NAME = "ubuntu-latest.tar.gz"

    const val EULA_URL = "https://raw.githubusercontent.com/flomobility/anx_docs/master/eula.md"

    val BASE_URL: String = when (BUILD_TYPE) {
        FloApplication.BuildType.DEV -> FLO_DEVELOPMENT_URL
        FloApplication.BuildType.STAGING -> FLO_STAGING_URL
        FloApplication.BuildType.RELEASE -> FLO_PRODUCTION_URL
        else -> FLO_DEVELOPMENT_URL
    }
}
