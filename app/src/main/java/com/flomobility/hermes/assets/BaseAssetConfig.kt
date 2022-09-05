package com.flomobility.hermes.assets

import com.flomobility.hermes.common.Result
import com.flomobility.hermes.other.Constants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import kotlin.reflect.KClass

abstract class BaseAssetConfig {

    open var portPub = 10100

    open var portSub = 10101

    open var connectedDeviceIp = ""

//    abstract fun <T: Any> getFields(): List<Field<T>>

    abstract fun getFields(): List<Field<*>>

    fun getFieldNames() = getFields().map { it.name }

    fun findField(fieldName: String) = getFields().find { it.name == fieldName }

    open class Field<T : Any>(val cls: Class<T>) {

        open var range: List<T> = listOf()

        open var name: String = ""

        open var value = Any() as T

        inline fun <reified S : Any> inRange(value: S/*, fieldType: KClass<*>*/): Result {
            if (value is JSONObject) {
                val obj = Gson().fromJson<T>(
                    value.toString(),
                    object : TypeToken<T>() {}.type)
                return Result(
                    success = obj in range
                )
            }
//            if (S::class.java == this.cls) {
                return Result(success = range.contains(value as T))
//            }
//            return Result(success = false, message = Constants.UNKNOWN_ERROR_MSG)
        }

        fun updateValue(value: Any) {
            this.value = value as T
        }
    }
}
