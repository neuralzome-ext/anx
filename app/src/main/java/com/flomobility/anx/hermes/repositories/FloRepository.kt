package com.flomobility.anx.hermes.repositories

import com.flomobility.anx.hermes.network.requests.InfoRequest
import com.flomobility.anx.hermes.network.requests.LoginRequest
import com.flomobility.anx.hermes.network.responses.InfoResponse
import com.flomobility.anx.hermes.network.responses.LoginResponse
import com.flomobility.anx.hermes.other.Resource

interface FloRepository {

    suspend fun login(loginRequest: LoginRequest): Resource<LoginResponse>

    suspend fun getInfo(infoRequest: InfoRequest): Resource<InfoResponse>

    suspend fun getEula(url: String): Resource<String>
}
