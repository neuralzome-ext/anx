package com.flomobility.anx.hermes.ui.asset_debug

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.flomobility.anx.databinding.ActivityAssetDebugBinding
import com.flomobility.anx.hermes.assets.AssetManager
import com.flomobility.anx.hermes.assets.AssetType
import com.flomobility.anx.hermes.assets.getAssetTypeFromAlias
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AssetDebugActivity : ComponentActivity() {

    @Inject
    lateinit var assetManager: AssetManager

    companion object {
        fun navigateToAssetDebugActivity(context: Context, assetData: Bundle) {
            context.startActivity(Intent(context, AssetDebugActivity::class.java).apply {
                putExtra(KEY_ASSET_DATA, assetData)
            })
        }

        private const val KEY_ASSET_DATA = "KEY_ASSET_DATA"
        const val KEY_ASSET_TYPE = "KEY_ASSET_TYPE"
        const val KEY_ASSET_IMAGE = "KEY_ASSET_IMAGE"
    }

    private var _binding: ActivityAssetDebugBinding? = null
    private val binding get() = _binding!!

    private var _assetTypeAlias: String? = null
    private var _assetType: AssetType = AssetType.UNK

    @DrawableRes
    private var _assetImgRes: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityAssetDebugBinding.inflate(layoutInflater)
        _assetTypeAlias = intent.getBundleExtra(KEY_ASSET_DATA)?.getString(KEY_ASSET_TYPE)
        _assetType = getAssetTypeFromAlias(_assetTypeAlias!!)
        _assetImgRes = intent.getBundleExtra(KEY_ASSET_DATA)?.getInt(KEY_ASSET_IMAGE)
        setContentView(binding.root)
        if (_assetImgRes == null || _assetTypeAlias == null) return
        setUI()
        setupListeners()
    }

    private fun setupListeners() {
        with(binding) {
            backBtn.setOnClickListener {
                onBackPressed()
            }
        }
    }

    private fun setUI() {
        with(binding) {
            ivAssetIcon.setImageDrawable(
                ContextCompat.getDrawable(
                    this@AssetDebugActivity,
                    _assetImgRes!!
                )
            )
            tvAssetType.text = _assetTypeAlias

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
