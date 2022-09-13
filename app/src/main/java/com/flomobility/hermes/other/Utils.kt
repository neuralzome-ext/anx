package com.flomobility.hermes.other

import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
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
 * Executes any command inputted
 * @param cmd command to execute
 * */
fun runAsRoot(cmd: String) {
    val process = Runtime.getRuntime().exec("su -c $cmd")
    process.waitFor()
    Timber.d("${BufferedReader(InputStreamReader(process.inputStream)).readLines()}")
}