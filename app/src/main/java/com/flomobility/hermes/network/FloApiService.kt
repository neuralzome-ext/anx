package com.flomobility.hermes.network

import com.flomobility.hermes.network.requests.InfoRequest
import com.flomobility.hermes.network.requests.LoginRequest
import com.flomobility.hermes.network.responses.InfoResponse
import com.flomobility.hermes.network.responses.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface FloApiService {
    @POST("api/v1/anx/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("api/v1/anx/me")
    suspend fun getInfo(@Header("authorization") token: String?, @Body infoRequest: InfoRequest): Response<InfoResponse>
}