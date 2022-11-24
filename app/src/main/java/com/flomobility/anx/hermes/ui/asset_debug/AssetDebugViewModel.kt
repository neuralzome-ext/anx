package com.flomobility.anx.hermes.ui.asset_debug

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.flomobility.anx.hermes.assets.AssetManager
import com.flomobility.anx.hermes.assets.AssetType
import com.flomobility.anx.hermes.assets.BaseAsset
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AssetDebugViewModel @Inject constructor(
    private val assetManager: AssetManager
): ViewModel() {

    private val _assets = MutableLiveData<List<BaseAsset>>()
    val assets: LiveData<List<BaseAsset>> = _assets

    private val _currentAsset = MutableLiveData<BaseAsset>()
    val currentAsset: LiveData<BaseAsset> = _currentAsset

    fun getAssets(type: AssetType) {
        val assets = assetManager.assets.filter { it.type == type }
        _assets.postValue(assets)
        setCurrentAsset(assets.first())
    }

    fun setCurrentAsset(asset: BaseAsset) {
        _currentAsset.postValue(asset)
    }

}
