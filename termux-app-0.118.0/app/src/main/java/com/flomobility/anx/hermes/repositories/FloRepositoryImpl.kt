package com.flomobility.anx.hermes.repositories

import android.content.SharedPreferences
import com.flomobility.anx.hermes.network.FloApiService
import com.flomobility.anx.hermes.network.requests.InfoRequest
import com.flomobility.anx.hermes.network.requests.LoginRequest
import com.flomobility.anx.hermes.network.responses.FloApiError
import com.flomobility.anx.hermes.other.Resource
import com.flomobility.anx.hermes.other.getToken
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject

class FloRepositoryImpl @Inject constructor(
    private val floApi: FloApiService,
    private val sharedPreferences: SharedPreferences
//    private val sharedPreferences: SharedPreferences,
//    private val tokenManager: TokenManager,
//    private val floConnectivityManager: FloConnectivityManager
) : FloRepository {

    override suspend fun login(loginRequest: LoginRequest) = handleApiResponse {
        floApi.login(loginRequest)
    }

    override suspend fun getInfo(infoRequest: InfoRequest) = handleApiResponse {
        val authToken = "Bearer ${sharedPreferences.getToken()}"
        floApi.getInfo(authToken, infoRequest)
    }


}

suspend inline fun <reified T> handleApiResponse(
    crossinline apiCall: suspend () -> Response<T>
): Resource<T> {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiCall.invoke()
            when {
                response.isSuccessful -> {
                    Timber.d("Response obtained is ${response.body().toString()}")
                    Resource.Success(response.body())
                }
                /*response.code() == HTTP_CODE_UNAUTHORIZED -> {
                    Resource.UnAuthenticated("Session Expired. Please Login")
                }*/
                else -> {
                    Timber.e("Error found")
                    handleApiError(response)
                }
            }
        } catch (e: Exception) {
            when {
                e.message != null -> {
                    Timber.e(e, "Exception is ${e.message}")
                    Resource.Error(e.message!!, null)
                }
                else -> {
                    Timber.e("An unknown error occurred")
                    Resource.Error("An unknown error occurred", null)
                }
            }
        }
    }
}

fun <T> handleApiError(response: Response<T>): Resource.Error<T> {
    val type = object : TypeToken<FloApiError>() {}.type
    val reader = response.errorBody()!!.charStream()
//    Timber.e(jso)
    val floApiError: FloApiError? = Gson().fromJson(reader, type)
    floApiError?.code = response.code()
    Timber.d("Passed $floApiError")
    val formattedMessage =
        if (floApiError == null)
            response.message()
                ?: "An Unknown Error occurred (100)."
        else "${floApiError.message} (${floApiError.code})."
    Timber.e("Error $floApiError $formattedMessage")
    return Resource.Error(formattedMessage, errorData = floApiError)
}
