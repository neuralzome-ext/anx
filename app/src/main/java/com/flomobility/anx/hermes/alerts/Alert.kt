package com.flomobility.anx.hermes.alerts

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Alert(
    val title: String,
    val message: String,
    val priority: Priority
): Parcelable {

    enum class Priority {
        LOW, MEDIUM, HIGH, SEVERE
    }

}
