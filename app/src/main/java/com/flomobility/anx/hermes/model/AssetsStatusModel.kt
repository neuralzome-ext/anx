package com.flomobility.anx.hermes.model

import androidx.lifecycle.LiveData
import com.flomobility.anx.hermes.assets.AssetState

data class AssetsStatusModel(
    var active: Boolean,
    val stateLiveData: LiveData<AssetState>
)
