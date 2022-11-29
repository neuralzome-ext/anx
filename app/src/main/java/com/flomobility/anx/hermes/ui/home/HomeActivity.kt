package com.flomobility.anx.hermes.ui.home

import android.content.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.downloader.PRDownloader
import com.flomobility.anx.app.PluginResultsService
import com.flomobility.anx.app.TerminalCommandExecutor
import com.flomobility.anx.hermes.assets.AssetManager
import com.flomobility.anx.hermes.daemon.EndlessService
import com.flomobility.anx.hermes.network.requests.InfoRequest
import com.flomobility.anx.hermes.other.*
import com.flomobility.anx.hermes.other.viewutils.AlertDialog
import com.flomobility.anx.hermes.ui.adapter.AssetAdapter
import com.flomobility.anx.hermes.ui.adapter.IpAdapter
import com.flomobility.anx.hermes.ui.login.LoginActivity
import com.flomobility.anx.hermes.ui.settings.SettingsActivity
import com.flomobility.anx.databinding.ActivityHomeBinding
import com.flomobility.anx.hermes.ui.asset_debug.AssetDebugActivity
import com.flomobility.anx.shared.terminal.TerminalConstants
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
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

        private const val START_SSHD_EXECUTION_CODE = 10302
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
        setupUI()
        setupRecyclers()
        subscribeToObservers()
        setEventListeners()
        viewModel.sendInfoRequest(InfoRequest(sharedPreferences.getDeviceID()!!))
    }

    private fun setupUI() {
        with(bind) {
            ipSwitch.setOnToggledListener { _, isOn ->
                (bind.ipRecycler.adapter as IpAdapter).updateIpList(getIPAddressList(useIPv4 = isOn))
            }
        }
    }

    // broadcast receiver
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                val executionCode =
                    intent.getIntExtra(PluginResultsService.RESULT_BROADCAST_EXECUTION_CODE_KEY, -1)
                val result = intent.getBundleExtra(PluginResultsService.RESULT_BROADCAST_RESULT_KEY)
                when (executionCode) {
                     START_SSHD_EXECUTION_CODE -> {
                        result?.let {
                            val exitCode =
                                result.getInt(TerminalConstants.TERMUX_APP.TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE)
                            if (exitCode == 0) {
                                Timber.i("SSH server on port 2222 started successfully")
                                return
                            }
                            Timber.e("Starting ssh server failed")
                        }
                    }
                }
            }
        }
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
                    startSshServer()
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
                (bind.ipRecycler.adapter as IpAdapter).updateIpList(getIPAddressList(useIPv4 = bind.ipSwitch.isOn))
            } else {
                bind.ipRecycler.isVisible = false
                bind.connectToNetworkError.isVisible = true
            }
        }
    }

    private fun startSshServer() {
        val terminalCommandExecutor =
            TerminalCommandExecutor.getInstance(this@HomeActivity)
        terminalCommandExecutor.startCommandExecutor(object :
            TerminalCommandExecutor.ITerminalCommandExecutor {
            override fun onEndlessServiceConnected() {
                terminalCommandExecutor.executeCommand(
                    this@HomeActivity,
                    "bash",
                    arrayOf("start.sh"),
                    START_SSHD_EXECUTION_CODE
                )
            }

            override fun onEndlessServiceDisconnected() {}
        })
    }

    private fun setupRecyclers() {
        bind.ipRecycler.layoutManager = LinearLayoutManager(this@HomeActivity)
        bind.ipRecycler.adapter = IpAdapter(this@HomeActivity, getIPAddressList())
        bind.assetRecycler.layoutManager = GridLayoutManager(this@HomeActivity, 4)
        bind.assetRecycler.adapter = AssetAdapter(
            this@HomeActivity,
            this@HomeActivity
        )
        (bind.assetRecycler.adapter as AssetAdapter).apply {
            doOnItemClicked { assetUI ->
                if(assetUI.assets.isEmpty()) return@doOnItemClicked

                AssetDebugActivity.navigateToAssetDebugActivity(
                    this@HomeActivity,
                    Bundle().apply {
                        putString(AssetDebugActivity.KEY_ASSET_TYPE, assetUI.assetType.alias)
                        putInt(AssetDebugActivity.KEY_ASSET_IMAGE, assetUI.assetImage)
                    }
                )
            }
        }.setupAssetsList()
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

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(PluginResultsService.RESULT_BROADCAST_INTENT))
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
