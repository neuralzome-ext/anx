package com.flomobility.hermes.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.flomobility.hermes.R
import com.flomobility.hermes.assets.AssetState
import com.flomobility.hermes.databinding.SensorStatusSingleItemBinding
import com.flomobility.hermes.model.AssetsStatusModel

class AssetStatusAdapter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val sensorStatusList: ArrayList<AssetsStatusModel>,
) :
    RecyclerView.Adapter<AssetStatusAdapter.SensorStatusViewHolder>() {
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
        val sensorStatus = sensorStatusList[position]
        observeStatus(sensorStatus)
        bind.status.setImageDrawable(
            AppCompatResources.getDrawable(
                context,
                if (sensorStatus.active) R.drawable.ic_circle_green else R.drawable.ic_circle_red
            )
        )
    }

    private fun observeStatus(sensorStatus: AssetsStatusModel) {
        sensorStatus.stateLiveData.observe(context as LifecycleOwner) {
            sensorStatus.active = it == AssetState.STREAMING
            notifyItemChanged(sensorStatusList.indexOf(sensorStatus))
        }
    }

    override fun getItemCount(): Int {
        return sensorStatusList.size
    }
}