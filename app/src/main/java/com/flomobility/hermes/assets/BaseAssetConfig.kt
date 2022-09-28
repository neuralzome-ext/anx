package com.flomobility.hermes.assets

import com.flomobility.hermes.common.Result
import com.flomobility.hermes.other.GsonUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import timber.log.Timber

abstract class BaseAssetConfig {

    open var portPub = 10100

    open var portSub = 10101

    open var connectedDeviceIp = ""

    abstract fun getFields(): List<Field<*>>

    fun getFieldNames() = getFields().map { it.name }

    fun findField(fieldName: String) = getFields().find { it.name == fieldName }

    open class Field<T : Any> {

        open var range: List<T> = listOf()

        open var name: String = ""

        open var value = Any() as T

        fun <S> inRange(value: S): Result {
            if (value is JSONObject) {
                val obj = fromJson(value)
                return Result(success = obj in range)
            }
            return Result(success = range.contains(value as T))
        }

        open fun fromJson(value: JSONObject): T {
            return Gson().fromJson<T>(
                value.toString(),
                object : TypeToken<T>() {}.type
            )
        }

        fun updateValue(value: Any) {
            if(value is JSONObject) {
                this.value = fromJson(value)
                return
            }
            this.value = value as T
        }

        fun updateRange(range: List<T>) {
            this.range = range
        }

    }
}
