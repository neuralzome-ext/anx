package com.flomobility.anx.hermes.common

data class Rate(val hz: Int) {

    fun toMicros(): Long {
        return (1_000_000L / hz)
    }

    fun toMillis(): Long {
        return (1_000L / hz)
    }

    private var _start = System.currentTimeMillis()
    private var _expected_cycle_time = 1000 / hz

    private var actual_cycle_time = 0L

    fun sleep() {
/*        val sleepDuration = (1000 / hz) - (System.currentTimeMillis() - time)
        if (sleepDuration >= 1) {
            Thread.sleep(sleepDuration)
        }
        time = System.currentTimeMillis()*/
        var end = _start + _expected_cycle_time
        val actualEnd = System.currentTimeMillis()

        if (actualEnd < _start) {
            end = actualEnd + _expected_cycle_time
        }

        val sleepDuration = end - actualEnd
        actual_cycle_time = actualEnd - _start

        _start = end
        if(sleepDuration <= 0) {
            if (actualEnd > end + _expected_cycle_time)
            {
                _start = actualEnd
            }
            return
        }
        Thread.sleep(sleepDuration)
    }
}
