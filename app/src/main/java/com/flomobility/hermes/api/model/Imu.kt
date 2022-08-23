package com.flomobility.hermes.api.model

import org.json.JSONObject

data class Imu(
    val linearAcceleration: LinearAcceleration,
    val angularVelocity: AngularVelocity,
    val quaternion: Quaternion
): BaseData {

    override fun toJson(): String {
        return JSONObject().apply {
            put("a", listOf(linearAcceleration.x, linearAcceleration.y, linearAcceleration.z))
            put("w", listOf(angularVelocity.x, angularVelocity.y, angularVelocity.z))
            put("q", listOf(quaternion.x, quaternion.y, quaternion.z, quaternion.w))
        }.toString()
    }
}

data class LinearAcceleration(
    val x: Double,
    val y: Double,
    val z: Double
)

data class AngularVelocity(
    val x: Double,
    val y: Double,
    val z: Double
)
