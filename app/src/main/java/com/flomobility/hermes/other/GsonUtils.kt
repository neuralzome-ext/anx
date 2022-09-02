package com.flomobility.hermes.other

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object GsonUtils {

    private var instance: Gson? = null

    fun getGson(): Gson {
        if (instance == null) {
            instance = GsonBuilder()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .create()
        }
        return instance!!
    }

}