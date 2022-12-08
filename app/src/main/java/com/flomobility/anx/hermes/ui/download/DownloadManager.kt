package com.flomobility.anx.hermes.ui.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
){
    var installFunc: () -> Unit = { Unit }
    var retryFunc: () -> Unit = { Unit }

    fun install() {
        installFunc()
    }

    fun retry() {
        retryFunc()
    }


}
