package com.flomobility.hermes.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flomobility.hermes.databinding.SensorSingleItemBinding
import com.flomobility.hermes.model.SensorModel

class SensorAdapter(
    private val context: Context,
    private val sensorList: ArrayList<SensorModel>,
) :
    RecyclerView.Adapter<SensorAdapter.SensorViewHolder>() {
    inner class SensorViewHolder(val bind: SensorSingleItemBinding) :
        RecyclerView.ViewHolder(bind.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorViewHolder {
        return SensorViewHolder(
            SensorSingleItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SensorViewHolder, position: Int) {
        val bind = holder.bind
        val sensor = sensorList[position]
        bind.sensorImage.setImageDrawable(
            AppCompatResources.getDrawable(
                context,
                sensor.sensorImage
            )
        )
        bind.sensorName.text = sensor.sensorName
        if (sensor.isAvailable && sensor.sensorStatuses != null) {
            bind.sensorStatusRecycler.visibility = View.VISIBLE
            bind.sensorStatusRecycler.adapter = SensorStatusAdapter(context, sensor.sensorStatuses)
            bind.sensorStatusRecycler.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        else{
            bind.sensorAvailability.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return sensorList.size
    }
}