package com.flomobility.hermes.api.model

import com.google.gson.annotations.SerializedName

/*Model class for GNSS data transfer over ZMQ*/
data class GNSSData (
    @SerializedName("nmea")
    var nmea: String = "",
    /*@SerializedName("timestamp")// If required then we will enabled
    var timestamp: Long = 0L*/
)
