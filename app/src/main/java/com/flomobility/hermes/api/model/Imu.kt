package com.flomobility.hermes.api.model

import org.json.JSONObject

data class Imu(
    val linearAcceleration: LinearAcceleration,
    val angularVelocity: AngularVelocity,
    val quaternion: Quaternion
): BaseData {

    companion object {
        fun new(linearAcceleration: LinearAcceleration?,
                angularVelocity: AngularVelocity?,
                quaternion: Quaternion?): Imu {
            return Imu(
                linearAcceleration ?: LinearAcceleration(0.0, 0.0, 0.0),
                angularVelocity ?: AngularVelocity(0.0, 0.0, 0.0),
                quaternion ?: Quaternion(0.0, 0.0, 0.0, 1.0)
            )
        }
    }

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
