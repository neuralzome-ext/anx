package com.flomobility.hermes.other

import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.util.*
import kotlin.reflect.KClass

/**
 * A generic function to handle exceptions
 *
 * */
inline fun <T> handleExceptions(
    vararg exceptions: KClass<out Exception>,
    catchBlock: ((Exception) -> Unit) = { it.printStackTrace() },
    block: () -> T?
): Exception? {
    return try {
        block()
        null
    } catch (e: Exception) {
        val contains = exceptions.find {
            it.isInstance(e)
        }
        contains?.let {
            return it.javaObjectType.cast(e)
        }
        catchBlock(e)
        e
    }
}

/**
 * converts a [ByteBuffer] object to a byte array.
 *
 * If array doesn't exist, empty array is returned
 * @return data from buffer in an array
 * */
fun ByteBuffer.toByteArray(): ByteArray {
    try {
        rewind()
        if (hasArray())
            return this.array()
        val bytes = ByteArray(remaining())
        get(bytes)
        return bytes
    } catch (e: Exception) {
        return ByteArray(0)
    }
}

/**
 * Executes any command inputted
 * @param cmd command to execute
 * */
fun runAsRoot(cmd: String) {
    val process = Runtime.getRuntime().exec("su -c $cmd")
    process.waitFor()
    Timber.d("${BufferedReader(InputStreamReader(process.inputStream)).readLines()}")
}

fun getRootOutput(cmd: String): String {
    return try {
        val command = arrayOf("su", "-c", cmd)
        val process = Runtime.getRuntime().exec(command)
        process.waitFor()
        BufferedReader(InputStreamReader(process.inputStream)).readLines()[0]
    } catch (e: Exception) {
        ""
    }
}

/**
 * Get IP address from first non-localhost interface
 * @param useIPv4   true=return ipv4, false=return ipv6
 * @return  address or empty string
 */
fun getIPAddress(useIPv4: Boolean): String {
    try {
        val interfaces: List<NetworkInterface> =
            Collections.list(NetworkInterface.getNetworkInterfaces())
        for (intf in interfaces) {
            val addrs: List<InetAddress> = Collections.list(intf.inetAddresses)
            for (addr in addrs) {
                if (!addr.isLoopbackAddress) {
                    val sAddr = addr.hostAddress ?: return ""

                    //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                    val isIPv4 = sAddr.indexOf(':') < 0
                    if (useIPv4) {
                        if (isIPv4) return sAddr
                    } else {
                        if (!isIPv4) {
                            val delim = sAddr.indexOf('%') // drop ip6 zone suffix
                            return if (delim < 0) sAddr.uppercase(Locale.getDefault()) else sAddr.substring(
                                0,
                                delim
                            ).uppercase(
                                Locale.getDefault()
                            )
                        }
                    }
                }
            }
        }
    } catch (ignored: java.lang.Exception) {
    } // for now eat exceptions
    return ""
}

/**
 * Get IP address from first non-localhost interface
 * @param useIPv4   true=return ipv4, false=return ipv6
 * @return  address or empty string
 */
fun getIPAddressList(useIPv4: Boolean): List<String> {
    val ipAddresses: ArrayList<String> = ArrayList()
    try {
        val interfaces: List<NetworkInterface> =
            Collections.list(NetworkInterface.getNetworkInterfaces())
        for (intf in interfaces) {
            val addrs: List<InetAddress> = Collections.list(intf.inetAddresses)
            for (addr in addrs) {
                if (!addr.isLoopbackAddress) {
                    val sAddr = addr.hostAddress ?: return arrayListOf()

                    //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                    val isIPv4 = sAddr.indexOf(':') < 0
                    if (useIPv4) {
                        if (isIPv4) ipAddresses.add(sAddr)
                    } else {
                        if (!isIPv4) {
                            val delim = sAddr.indexOf('%') // drop ip6 zone suffix
                            ipAddresses.add(
                                if (delim < 0) sAddr.uppercase(Locale.getDefault()) else sAddr.substring(
                                    0,
                                    delim
                                ).uppercase(
                                    Locale.getDefault()
                                )
                            )
                        }
                    }
                }
            }
        }
    } catch (ignored: java.lang.Exception) {
    } // for now eat exceptions
    return ipAddresses
}