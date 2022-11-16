package com.flomobility.anx.hermes.ui.license

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.flomobility.anx.databinding.ActivityLicenseBinding
import com.flomobility.anx.hermes.other.Resource
import com.flomobility.anx.hermes.other.setAcceptLicense
import com.flomobility.anx.hermes.ui.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LicenseActivity : ComponentActivity() {

    private var binding: ActivityLicenseBinding? = null
    private val bind get() = binding!!

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    companion object {
        fun navigateToLicense(context: Context) {
            context.startActivity(
                Intent(context, LicenseActivity::class.java)
            )
        }
    }

    lateinit var viewModel: LicenseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLicenseBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[LicenseViewModel::class.java]
        setContentView(bind.root)
        subscribeToObservers()
        setListeners()
        fetchLicense()
    }

    private fun setListeners() {
        with(bind) {
            agreeCheckbox.setOnCheckedChangeListener { _, isChecked ->
                btnAgree.isEnabled = isChecked
            }
            btnAgree.setOnClickListener {
                sharedPreferences.setAcceptLicense(true)
                LoginActivity.navigateToLogin(this@LicenseActivity)
                finish()
            }
            backBtn.setOnClickListener {
                finish()
            }
            btnTryAgain.setOnClickListener {
                fetchLicense()
            }
        }
    }

    private fun subscribeToObservers() {
        viewModel.license.observe(this) {
            it.getContentIfNotHandled()?.let { res ->
                when (res) {
                    is Resource.Error -> {
                        Timber.e("Error in fetching license agreement ${res.message}")
                        bind.progress.isVisible = false
                        bind.tvError.text = "Couldn't fetch license agreement"
                        bind.containerError.isVisible = true
                    }
                    is Resource.Loading -> {
                        // show progress bar
                        bind.containerLicense.isVisible = false
                        bind.containerError.isVisible = false
                        bind.progress.isVisible = true
                    }
                    is Resource.Success -> {
                        bind.progress.isVisible = false
                        bind.containerLicense.isVisible = true
                        bind.tvEula.text = res.data
                    }
                }
            }
        }
    }

    private fun fetchLicense() {
        viewModel.getLicense()
    }
}
