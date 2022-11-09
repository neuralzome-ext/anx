package com.flomobility.anx.hermes.ui.home

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.downloader.PRDownloader
import com.flomobility.anx.hermes.adapter.IpAdapter
import com.flomobility.anx.hermes.adapter.AssetAdapter
import com.flomobility.anx.hermes.assets.AssetManager
import com.flomobility.anx.hermes.daemon.EndlessService
import com.flomobility.anx.hermes.other.*
import com.flomobility.anx.hermes.network.requests.InfoRequest
import com.flomobility.anx.hermes.other.viewutils.AlertDialog
import com.flomobility.anx.hermes.ui.login.LoginActivity
import com.flomobility.anx.hermes.ui.settings.SettingsActivity
import com.flomobility.anx.databinding.ActivityHomeBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    companion object {
        fun navigateToHome(context: Context) {
            context.startActivity(
                Intent(context, HomeActivity::class.java)//,
//                ActivityOptions.makeSceneTransitionAnimation(context as Activity).toBundle()
            )
        }
    }

    private var binding: ActivityHomeBinding? = null
    private val bind get() = binding!!
    private lateinit var viewModel: HomeViewModel

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var assetManager: AssetManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this@HomeActivity)[HomeViewModel::class.java]
        setContentView(binding?.root)
        if (sharedPreferences.getDeviceID() == null) {
            showSnack("Login Again")
            logout()
            return
        }
        bind.deviceId.text = "DEVICE ID: ${sharedPreferences.getDeviceID()}"
        setupRecyclers()
        subscribeToObservers()
        setEventListeners()
        viewModel.sendInfoRequest(InfoRequest(sharedPreferences.getDeviceID()!!))
    }

    private fun setEventListeners() {
        bind.apply {
            exit.setOnClickListener {
                AlertDialog.getInstance(
                    "Confirm Logout",
                    "Are you sure you want to logout?",
                    "Logout",
                    "Cancel",
                    yesListener = {
                        PRDownloader.cancelAll()
                        logout()
                    }
                ).show(supportFragmentManager, AlertDialog.TAG)
            }
            settings.setOnClickListener {
                SettingsActivity.navigateToSetting(this@HomeActivity)
                finish()
            }
        }
    }

    private fun subscribeToObservers() {
        viewModel.info.observe(this@HomeActivity) {
            when (it.getContentIfNotHandled()) {
                is Resource.Loading -> {

                }
                is Resource.Success -> {
                    if (it.peekContent().data?.access == false || isExpired(it.peekContent().data?.expiry)) {
                        showSnack("Your access has been revoked")
                        logout()
                        return@observe
                    }
                    sharedPreferences.putDeviceExpiry(it.peekContent().data?.expiry)
                    sendCommandToService(
                        Constants.ACTION_START_OR_RESUME_SERVICE,
                        EndlessService::class.java
                    )
                }
                is Resource.Error -> {
                    when (it.peekContent().errorData?.code) {
                        null -> {
                            var error = it.peekContent().message
                            if (error?.contains("Failed to connect to") == true)
                                error = "Failed to connect to server"
                            showSnack(error)
                            return@observe
                        }
                        400 -> {
                            showSnack("Server Unreachable!! (400)")
                        }
                        401 -> {
                            showSnack("Login Again")
                            logout()
                        }
                        else -> {
                            val error = it.peekContent().errorData?.message
                            showSnack(error)
                        }
                    }
                }
                null -> TODO()
            }
        }
        assetManager.getAssetsLiveData().observe(this@HomeActivity) { assets ->
            (bind.assetRecycler.adapter as AssetAdapter).refreshAssets(assets)
        }

        InternetConnectionCheck(this).observe(this) { isConnected ->
            if(isConnected) {
                bind.ipRecycler.isVisible = true
                bind.connectToNetworkError.isVisible = false
                (bind.ipRecycler.adapter as IpAdapter).updateIpList(getIPAddressList(true))
            } else {
                bind.ipRecycler.isVisible = false
                bind.connectToNetworkError.isVisible = true
            }
        }
    }

    private fun setupRecyclers() {
        bind.ipRecycler.layoutManager = LinearLayoutManager(this@HomeActivity)
        bind.ipRecycler.adapter = IpAdapter(this@HomeActivity, getIPAddressList(true))
        bind.assetRecycler.layoutManager = GridLayoutManager(this@HomeActivity, 4)
        bind.assetRecycler.adapter = AssetAdapter(
            this@HomeActivity,
            this@HomeActivity
        )
        (bind.assetRecycler.adapter as AssetAdapter).setupAssetsList()
    }

    private fun sendCommandToService(action: String, serviceClass: Class<*>) {
        handleExceptions {
            Intent(this, serviceClass).also {
                it.action = action
                startService(it)
            }
        }
    }

    private fun showSnack(msg: String?) {
        runOnUiThread {
            if (msg != null)
                Snackbar.make(bind.root, msg, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun logout() {
        sharedPreferences.clear()
        sendCommandToService(Constants.ACTION_STOP_SERVICE, EndlessService::class.java)
        LoginActivity.navigateToLogin(this@HomeActivity)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}