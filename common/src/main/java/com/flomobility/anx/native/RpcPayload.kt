package com.flomobility.anx.native

data class RpcPayload(
    val rpcName: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RpcPayload

        if (rpcName != other.rpcName) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rpcName.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
