package com.flomobility.anx.hermes.model

data class IpAddress(
    val address: String,
    val type: Type,
    val networkInterface: String
) {
    enum class Type {
        IPv6, IPv4
    }

    override fun toString(): String {
        return "$networkInterface - $address"
    }
}
