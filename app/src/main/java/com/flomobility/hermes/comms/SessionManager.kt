package com.flomobility.hermes.comms

import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {

    private val lock = ReentrantLock(true)

    var connected = false
        get() {
            lock.lock()
            val value = field
            lock.unlock()
            return value
        }
        set(value) {
            lock.lock()
            field = value
            lock.unlock()
        }

    var connectedDeviceIp = ""

}