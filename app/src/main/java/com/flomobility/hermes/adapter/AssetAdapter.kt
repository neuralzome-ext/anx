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
import com.flomobility.hermes.R
import com.flomobility.hermes.assets.AssetType
import com.flomobility.hermes.assets.BaseAsset
import com.flomobility.hermes.databinding.SensorSingleItemBinding
import com.flomobility.hermes.model.AssetUI

class AssetAdapter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) :
    RecyclerView.Adapter<AssetAdapter.AssetViewHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<AssetUI>() {
        override fun areItemsTheSame(oldItem: AssetUI, newItem: AssetUI): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: AssetUI, newItem: AssetUI): Boolean {
            return oldItem.assets.hashCode() == newItem.assets.hashCode()
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    private var assetList: List<AssetUI>
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
        val assetUI = assetList[position]
        bind.sensorImage.setImageDrawable(
            AppCompatResources.getDrawable(
                context,
                assetUI.assetImage
            )
        )
        bind.sensorAvailability.visibility = View.GONE
        bind.assetStatusRecycler.visibility = View.GONE
        bind.sensorName.text = assetUI.assetType.alias
        if (assetUI.isAvailable) {
            bind.assetStatusRecycler.visibility = View.VISIBLE
            bind.assetStatusRecycler.adapter =
                AssetStatusAdapter(context, lifecycleOwner)
            (bind.assetStatusRecycler.adapter as AssetStatusAdapter).updateAssetsList(assetUI.assets)
            bind.assetStatusRecycler.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        } else {
            bind.sensorAvailability.visibility = View.VISIBLE
        }
    }

    fun setupAssetsList() {
        assetList = arrayListOf(
            AssetUI(R.drawable.ic_imu, AssetType.IMU, arrayListOf()),
            AssetUI(R.drawable.ic_gps, AssetType.GNSS, arrayListOf()),
            AssetUI(R.drawable.ic_video, AssetType.CAM, arrayListOf()),
            AssetUI(R.drawable.ic_mic, AssetType.MIC, isAvailable = false),
            AssetUI(R.drawable.ic_usb_serial, AssetType.USB_SERIAL, arrayListOf()),
            AssetUI(R.drawable.ic_speaker, AssetType.SPEAKER, arrayListOf()),
            AssetUI(R.drawable.ic_bluetooth, AssetType.CLASSIC_BT, isAvailable = false),
            AssetUI(R.drawable.ic_bluetooth, AssetType.BLE, isAvailable = false),
            AssetUI(R.drawable.ic_phone, AssetType.PHONE, arrayListOf())
        ).toList()
    }

    override fun getItemCount(): Int {
        return assetList.size
    }

    fun refreshAssets(assets: List<BaseAsset>) {
        assetList.forEach { assetUI ->
            val filteredAssets = assets.filter { it.type == assetUI.assetType }
            assetUI.assets = filteredAssets
        }
        this.notifyDataSetChanged()
    }
}