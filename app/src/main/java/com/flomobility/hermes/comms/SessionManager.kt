package com.flomobility.hermes.comms

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {

    var connected = false

    var connectedDeviceIp = ""

}