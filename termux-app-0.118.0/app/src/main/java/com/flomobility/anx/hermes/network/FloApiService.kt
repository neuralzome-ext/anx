package com.flomobility.anx.hermes.network

import com.flomobility.anx.hermes.network.requests.InfoRequest
import com.flomobility.anx.hermes.network.requests.LoginRequest
import com.flomobility.anx.hermes.network.responses.InfoResponse
import com.flomobility.anx.hermes.network.responses.LoginResponse
import retrofit2.Response
import retrofit2.http.*

interface FloApiService {
    @POST("api/v1/anx/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("api/v1/anx/me")
    suspend fun getInfo(@Header("authorization") token: String?, @Body infoRequest: InfoRequest): Response<InfoResponse>

    @GET
    suspend fun getEula(@Url url: String): Response<String>
}
