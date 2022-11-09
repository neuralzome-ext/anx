package com.flomobility.anx.hermes.other

import com.flomobility.anx.hermes.network.responses.FloApiError

sealed class Resource<out T>(val data: T? = null, val message: String? = null, val errorData: FloApiError? = null) {
    class Success<T>(data: T?) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null, errorData: FloApiError? = null) : Resource<T>(data, message, errorData)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}