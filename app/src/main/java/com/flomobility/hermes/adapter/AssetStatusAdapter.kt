package com.flomobility.hermes.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.flomobility.hermes.R
import com.flomobility.hermes.assets.AssetState
import com.flomobility.hermes.assets.BaseAsset
import com.flomobility.hermes.databinding.SensorStatusSingleItemBinding
import timber.log.Timber

class AssetStatusAdapter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) :
    RecyclerView.Adapter<AssetStatusAdapter.SensorStatusViewHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<BaseAsset>() {
        override fun areItemsTheSame(
            oldItem: BaseAsset,
            newItem: BaseAsset
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: BaseAsset,
            newItem: BaseAsset
        ): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    var assets: List<BaseAsset>
        set(value) = differ.submitList(value)
        get() = differ.currentList

    inner class SensorStatusViewHolder(val bind: SensorStatusSingleItemBinding) :
        RecyclerView.ViewHolder(bind.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorStatusViewHolder {
        return SensorStatusViewHolder(
            SensorStatusSingleItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SensorStatusViewHolder, position: Int) {
        val bind = holder.bind
        val asset = assets[position]
        observeStatus(bind, asset.getStateLiveData())
    }

    private fun observeStatus(
        bind: SensorStatusSingleItemBinding,
        stateLiveData: LiveData<AssetState>,
    ) {
        stateLiveData.observe(lifecycleOwner) { state ->
            bind.status.setImageDrawable(
                AppCompatResources.getDrawable(
                    context,
                    if (state == AssetState.STREAMING) R.drawable.ic_circle_green else R.drawable.ic_circle_red
                )
            )
        }
    }

    fun updateAssetsList(list: List<BaseAsset>) {
        assets = list
    }

    override fun getItemCount(): Int {
        return assets.size
    }
}