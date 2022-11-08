package com.flomobility.hermes.model

import androidx.lifecycle.LiveData
import com.flomobility.hermes.assets.AssetState

data class AssetsStatusModel(
    var active: Boolean,
    val stateLiveData: LiveData<AssetState>
)
