package com.flomobility.hermes.assets

abstract class BaseAssetConfig {

    open var portPub = 10100

    open var portSub = 10101

    abstract fun getFields(): Map<String, Any>
}