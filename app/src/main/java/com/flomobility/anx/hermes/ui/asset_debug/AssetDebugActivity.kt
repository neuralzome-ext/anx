package com.flomobility.anx.hermes.ui.asset_debug

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.flomobility.anx.R
import com.flomobility.anx.databinding.ActivityAssetDebugBinding
import com.flomobility.anx.hermes.assets.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject


@AndroidEntryPoint
class AssetDebugActivity : ComponentActivity() {

    @Inject
    lateinit var assetManager: AssetManager

    @Inject
    lateinit var baseGson: Gson

    lateinit var viewModel: AssetDebugViewModel

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

    private var assetTypeAlias: String? = null
    private var assetType: AssetType = AssetType.UNK

    @DrawableRes
    private var assetImgRes: Int? = null

    private val gson: Gson by lazy {
        baseGson.newBuilder()
            .setPrettyPrinting()
            .create()
    }

    private var outStreamJob: Job? = null
    private var inStreamJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityAssetDebugBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[AssetDebugViewModel::class.java]
        assetTypeAlias = intent.getBundleExtra(KEY_ASSET_DATA)?.getString(KEY_ASSET_TYPE)
        assetType = getAssetTypeFromAlias(assetTypeAlias!!)
        assetImgRes = intent.getBundleExtra(KEY_ASSET_DATA)?.getInt(KEY_ASSET_IMAGE)
        setContentView(binding.root)

        if (assetImgRes == null || assetTypeAlias == null) return

        viewModel.getAssets(assetType)
        subscribeToObservers()
        setUI()
        setupListeners()
    }

    private fun setUI() {
        with(binding) {
            ivAssetIcon.setImageDrawable(
                ContextCompat.getDrawable(
                    this@AssetDebugActivity,
                    assetImgRes!!
                )
            )
            tvAssetType.text = assetTypeAlias
        }
    }

    private fun setupListeners() {
        with(binding) {
            backBtn.setOnClickListener {
                onBackPressed()
            }
            btnViewMeta.setOnClickListener {
                containerAssetMeta.isVisible = true
                overlay.isVisible = true
            }
            btnCloseDialog.setOnClickListener {
                dismissOverlay()
            }
        }
    }

    private fun dismissOverlay() {
        with(binding) {
            containerAssetMeta.isVisible = false
            overlay.isVisible = false
        }
    }

    private fun subscribeToObservers() {
        viewModel.currentAsset.observe(this) { asset ->
            updateAssetUI(asset)
        }
    }

    private fun updateAssetUI(asset: BaseAsset) {
        asset.getStateLiveData().observe(this) { assetState ->
            binding.tvStatus.apply {
                text = assetState.name
                setTextColor(
                    ContextCompat.getColor(
                        this@AssetDebugActivity,
                        if (assetState == AssetState.STREAMING) {
                            R.color.floGreen
                        } else {
                            R.color.floRed
                        }
                    )
                )
            }

            if(assetState == AssetState.STREAMING) {
                binding.containerAssetConfigPresent.isVisible = true
                binding.containerAssetConfigAbsent.isVisible = false
                binding.tvAssetConfig.text = getAssetConfigText(asset)

                binding.tvOut.isVisible = true
                binding.tvOutNotStreaming.isVisible = false

                binding.tvIn.isVisible = true
                binding.tvInNotStreaming.isVisible = false
            } else {
                binding.containerAssetConfigPresent.isVisible = false
                binding.containerAssetConfigAbsent.isVisible = true

                binding.tvOut.isVisible = false
                binding.tvOutNotStreaming.isVisible = true

                binding.tvIn.isVisible = false
                binding.tvInNotStreaming.isVisible = true
            }
        }
        binding.tvMeta.text = gson.toJson(asset.getDesc())

        if(asset.type == AssetType.CAM) {
            binding.containerOutCamera.isVisible = true
            binding.containerOut.isVisible = false
            binding.containerIn.isVisible = false
        } else {
            binding.containerOutCamera.isVisible = false
            binding.containerOut.isVisible = asset.config.portPub != -1
            binding.containerIn.isVisible = asset.config.portSub != -1
        }

        outStreamJob?.cancel()
        outStreamJob = asset.outStream
            .onEach { outTxt ->
                binding.tvOut.text = gson.toJson(JsonParser.parseString(outTxt))
            }.launchIn(lifecycleScope)

        inStreamJob?.cancel()
        inStreamJob = asset.inStream
            .onEach { inTxt ->
                binding.tvIn.text = gson.toJson(JsonParser.parseString(inTxt))
            }.launchIn(lifecycleScope)

    }

    private fun getAssetConfigText(asset: BaseAsset): String {
        var assetConfig = ""
        asset.config.getFields().forEach { field ->
            assetConfig += "${field.name} : ${field.value}"
        }
        return assetConfig
    }

    override fun onBackPressed() {
        // check if asset meta view is visible
        if (binding.overlay.isVisible) {
            dismissOverlay()
            return
        }
        finish()
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        outStreamJob?.cancel()
        outStreamJob = null

        inStreamJob?.cancel()
        inStreamJob = null
    }
}
