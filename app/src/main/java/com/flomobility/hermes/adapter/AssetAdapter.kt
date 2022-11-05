package com.flomobility.hermes.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
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

    private val diffCallback = object : DiffUtil.ItemCallback<AssetsModel>() {
        override fun areItemsTheSame(oldItem: AssetsModel, newItem: AssetsModel): Boolean {
            return false//oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: AssetsModel, newItem: AssetsModel): Boolean {
            return false//oldItem.assetStatuses.hashCode() == newItem.assetStatuses.hashCode()
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    private var assetList: List<AssetsModel>
        set(value) = differ.submitList(value)
        get() = differ.currentList

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
                asset.assetImage
            )
        )
        bind.sensorAvailability.visibility = View.GONE
        bind.assetStatusRecycler.visibility = View.GONE
        bind.sensorName.text = asset.assetName
        if (asset.isAvailable) {
            bind.assetStatusRecycler.visibility = View.VISIBLE
            bind.assetStatusRecycler.adapter =
                AssetStatusAdapter(context, lifecycleOwner)
            (bind.assetStatusRecycler.adapter as AssetStatusAdapter).setAssetStatusList(asset.assetStatuses)
            bind.assetStatusRecycler.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        } else {
            bind.sensorAvailability.visibility = View.VISIBLE
        }
    }

    fun addAssetState(asset: BaseAsset) {
        assetList.single {
            it.assetName == asset.type.alias
        }.apply {
            this.assetStatuses.add(
                AssetsStatusModel(
                    asset.state == AssetState.STREAMING,
                    asset.getStateLiveData()
                )
            )
        }
        assetList = assetList
    }

    fun setAssetList() {
        assetList = arrayListOf(
            AssetsModel(AssetType.IMU.image, AssetType.IMU.alias, arrayListOf()),
            AssetsModel(AssetType.GNSS.image, AssetType.GNSS.alias, arrayListOf()),
            AssetsModel(AssetType.CAM.image, AssetType.CAM.alias, arrayListOf()),
            AssetsModel(AssetType.MIC.image, AssetType.MIC.alias, isAvailable = false),
            AssetsModel(AssetType.USB_SERIAL.image, AssetType.USB_SERIAL.alias, arrayListOf()),
            AssetsModel(AssetType.SPEAKER.image, AssetType.SPEAKER.alias, arrayListOf()),
            AssetsModel(
                AssetType.CLASSIC_BT.image,
                AssetType.CLASSIC_BT.alias,
                isAvailable = false
            ),
            AssetsModel(AssetType.BLE.image, AssetType.BLE.alias, isAvailable = false),
            AssetsModel(AssetType.PHONE.image, AssetType.PHONE.alias, arrayListOf())
        ).toList()
    }

    fun resetAssetList() {
        assetList.forEach {
            it.assetStatuses = arrayListOf()
        }
    }

    override fun getItemCount(): Int {
        return assetList.size
    }
}