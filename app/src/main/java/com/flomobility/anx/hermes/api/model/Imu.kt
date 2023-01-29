package com.flomobility.anx.hermes.api.model

import com.flomobility.anx.hermes.other.GsonUtils

data class Imu(
    val filtered: Filtered,
    val raw: Raw
): BaseData {

    companion object {
        fun new(
            filtered: Filtered,
            raw: Raw
        ): Imu {
            return Imu(
                filtered = filtered,
                raw = raw
            )
        }
    }

    override fun toJson(): String {
        val imuMap = hashMapOf(
            "filtered" to hashMapOf(
                "a" to listOf(filtered.acceleration.x, filtered.acceleration.y, filtered.acceleration.z),
                "w" to listOf(filtered.angularVelocity.x, filtered.angularVelocity.y, filtered.angularVelocity.z),
                "q" to listOf(filtered.orientation.x, filtered.orientation.y, filtered.orientation.z, filtered.orientation.w)
            ),
            "raw" to hashMapOf(
                "a" to listOf(raw.acceleration.x, raw.acceleration.y, raw.acceleration.z),
                "w" to listOf(raw.angularVelocity.x, raw.angularVelocity.y, raw.angularVelocity.z),
                "mu" to listOf(raw.magneticField.x, raw.magneticField.y, raw.magneticField.z)
            )
        )
        return GsonUtils.getGson().toJson(imuMap)
    }

    data class Filtered(
        val acceleration: Vector3d,
        val angularVelocity: Vector3d,
        val orientation: Vector4d
    )

    data class Raw(
        val acceleration: Vector3d,
        val angularVelocity: Vector3d,
        val magneticField: Vector3d
    )
}
