package com.flomobility.anx.hermes.ui.asset_debug

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.flomobility.anx.hermes.assets.AssetManager
import com.flomobility.anx.hermes.assets.AssetType
import com.flomobility.anx.hermes.assets.BaseAsset
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AssetDebugViewModel @Inject constructor(
    private val assetManager: AssetManager
) : ViewModel() {

//    private val _assets = MutableLiveData<List<BaseAsset>>()
    val assets: LiveData<List<BaseAsset>> = assetManager.getAssetsLiveData()

    private val _currentAsset = MutableLiveData<BaseAsset>()
    val currentAsset: LiveData<BaseAsset> = _currentAsset

    /*fun getAssets(type: AssetType) {
        val assets = assetManager.assets.filter { it.type == type }
        _assets.postValue(assets)
        setCurrentAsset(assets.first())
    }*/

    fun setCurrentAsset(asset: BaseAsset) {
        _currentAsset.value?.debug = false
        _currentAsset.postValue(asset)
    }

    fun setCurrentAssetById(id: String, type: AssetType) {
        if(_currentAsset.value?.id == id) return
        val asset = assets.value?.filter { it.id == id && it.type == type}?.first() ?: return
        setCurrentAsset(asset)
    }

    fun setDebug(debug: Boolean) {
        _currentAsset.value?.debug = debug
    }

}
