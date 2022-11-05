package com.flomobility.hermes.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flomobility.hermes.assets.AssetState
import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.BaseAsset
import com.flomobility.hermes.databinding.SensorSingleItemBinding
import com.flomobility.hermes.model.AssetsModel
import com.flomobility.hermes.model.AssetsStatusModel

class AssetAdapter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) :
    RecyclerView.Adapter<AssetAdapter.AssetViewHolder>() {

    private val assetList: ArrayList<AssetsModel> = arrayListOf()

    inner class AssetViewHolder(val bind: SensorSingleItemBinding) :
        RecyclerView.ViewHolder(bind.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        return AssetViewHolder(
            SensorSingleItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        val bind = holder.bind
        val asset = assetList[position]
        bind.sensorImage.setImageDrawable(
            AppCompatResources.getDrawable(
                context,
                asset.sensorImage
            )
        )
        bind.sensorName.text = asset.sensorName
        if (asset.isAvailable) {
            bind.assetStatusRecycler.visibility = View.VISIBLE
            bind.assetStatusRecycler.adapter =
                AssetStatusAdapter(context, lifecycleOwner, asset.sensorStatuses)
            bind.assetStatusRecycler.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        } else {
            bind.sensorAvailability.visibility = View.VISIBLE
        }
    }

    fun addAssetState(asset: BaseAsset) {
        assetList.single {
            it.sensorName == asset.type.alias
        }.apply {
            val position = assetList.indexOf(this)
            this.sensorStatuses.add(
                AssetsStatusModel(
                    asset.state == AssetState.STREAMING,
                    asset.getStateLiveData()
                )
            )
            notifyItemChanged(position)
        }
    }

    fun clearAssetList() {
        assetList.clear()
        assetList.addAll(
            arrayListOf(
                AssetsModel(AssetType.IMU.image, AssetType.IMU.alias),
                AssetsModel(AssetType.GNSS.image, AssetType.GNSS.alias),
                AssetsModel(AssetType.CAM.image, AssetType.CAM.alias),
                AssetsModel(AssetType.MIC.image, AssetType.MIC.alias, isAvailable = false),
                AssetsModel(AssetType.USB_SERIAL.image, AssetType.USB_SERIAL.alias),
                AssetsModel(AssetType.SPEAKER.image, AssetType.SPEAKER.alias),
                AssetsModel(
                    AssetType.CLASSIC_BT.image,
                    AssetType.CLASSIC_BT.alias,
                    isAvailable = false
                ),
                AssetsModel(AssetType.BLE.image, AssetType.BLE.alias, isAvailable = false),
                AssetsModel(AssetType.PHONE.image, AssetType.PHONE.alias)
            )
        )
    }

    override fun getItemCount(): Int {
        return assetList.size
    }
}