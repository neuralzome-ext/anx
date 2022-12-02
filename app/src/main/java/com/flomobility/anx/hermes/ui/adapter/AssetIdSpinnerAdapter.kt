package com.flomobility.anx.hermes.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.LifecycleOwner
import com.flomobility.anx.R
import com.flomobility.anx.databinding.ItemSpinnerAssetBinding
import com.flomobility.anx.databinding.ItemSpinnerAssetViewBinding
import com.flomobility.anx.hermes.assets.AssetState
import com.flomobility.anx.hermes.assets.BaseAsset

class AssetIdSpinnerAdapter(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
) : ArrayAdapter<BaseAsset>(context, 0) {

    private val inflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_spinner_asset_view, parent, false)
        val binding = ItemSpinnerAssetViewBinding.bind(view)
        getItem(position)?.let { asset ->
            setDisplayViewItem(binding, asset)
        }
        return view
    }

    private fun setDisplayViewItem(binding: ItemSpinnerAssetViewBinding, asset: BaseAsset) {
        binding.tvAssetId.text = asset.id
    }

    private fun setItemForAsset(
        binding: ItemSpinnerAssetBinding,
        parent: ViewGroup,
        asset: BaseAsset
    ) {
        with(binding) {
            asset.getStateLiveData().observe(lifecycleOwner) { state ->
                ivStatus.setImageDrawable(
                    AppCompatResources.getDrawable(
                        context,
                        if (state == AssetState.STREAMING) R.drawable.ic_circle_green else R.drawable.ic_circle_red
                    )
                )
            }
            tvAssetId.text = asset.id
        }
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_spinner_asset, parent, false)
        val binding = ItemSpinnerAssetBinding.bind(view)
        getItem(position)?.let { asset ->
            setItemForAsset(binding, parent,  asset)
        }
        return view
    }
}
