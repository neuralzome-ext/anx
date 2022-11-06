package com.flomobility.anx.hermes.common

data class Rate(val hz: Int) {

    fun toMicros(): Long {
        return (1_000_000L / hz)
    }

    fun toMillis(): Long {
        return (1_000L / hz)
    }

    private var time = System.currentTimeMillis()

    fun sleep() {
        val sleepDuration = (1000 / hz) - (System.currentTimeMillis() - time)
        if (sleepDuration >= 1) {
            Thread.sleep(sleepDuration)
        }
        time = System.currentTimeMillis()
    }

}
