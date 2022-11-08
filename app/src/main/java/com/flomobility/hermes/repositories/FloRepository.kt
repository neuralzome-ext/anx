package com.flomobility.hermes.repositories

import com.flomobility.hermes.network.requests.InfoRequest
import com.flomobility.hermes.network.requests.LoginRequest
import com.flomobility.hermes.network.responses.InfoResponse
import com.flomobility.hermes.network.responses.LoginResponse
import com.flomobility.hermes.other.Resource

interface FloRepository {

    suspend fun login(loginRequest: LoginRequest): Resource<LoginResponse>

    suspend fun getInfo(infoRequest: InfoRequest): Resource<InfoResponse>
}