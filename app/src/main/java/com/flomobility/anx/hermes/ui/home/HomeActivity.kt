package com.flomobility.anx.hermes.ui.home

import android.content.*
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.flomobility.anx.hermes.alerts.Alert
import com.flomobility.anx.hermes.alerts.AlertManager
import com.flomobility.anx.hermes.ui.asset_debug.AssetDebugActivity
import com.flomobility.anx.shared.terminal.TerminalConstants
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.exitProcess

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
    private lateinit var wifiManager: WifiManager

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var assetManager: AssetManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this@HomeActivity)[HomeViewModel::class.java]
        setContentView(binding?.root)
//        enableWifi()

        /**
         * If Build Type Headless than check condition for logout
         */
        if(!isHeadLessBuildType()) {
            if (sharedPreferences.getDeviceID() == null) {
                showSnackBar("Login Again")
                logout()
                return
            }
            if (isExpired(sharedPreferences.getDeviceExpiry())) {
                showSnackBar("Your access has expired.")
                logout()
                return
            }
        }

        bind.deviceId.text = "DEVICE ID: ${sharedPreferences.getDeviceID() ?: "-"}"
        setupUI()
        setupRecyclers()
        subscribeToObservers()
        setEventListeners()
        viewModel.sendInfoRequest(InfoRequest(sharedPreferences.getDeviceID() ?: ""))
        /**
         * If Headless than no need to call API
         * Just start the service
         */
        if(isHeadLessBuildType()){
            sendCommandToService(
                Constants.ACTION_START_OR_RESUME_SERVICE,
                EndlessService::class.java
            )
            startSshServer()
        }
    }

    /*
    method call on enableWifi
     */
    private fun enableWifi(){
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.isWifiEnabled = true
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
                when(intent.action) {
                    PluginResultsService.RESULT_BROADCAST_INTENT -> {
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
                    AlertManager.ANX_ALERTS_BROADCAST_INTENT -> {
                        val alert = intent.getParcelableExtra<Alert>(AlertManager.KEY_ANX_ALERT) ?: return
                        when(alert.priority) {
                            Alert.Priority.LOW -> {
                                // TODO
                            }
                            Alert.Priority.MEDIUM -> {
                                // TODO
                            }
                            Alert.Priority.HIGH -> {
                                AlertDialog.getInstance(
                                    alert.title,
                                    alert.message,
                                    "Ok"
                                ).show(supportFragmentManager, AlertDialog.TAG)
                            }
                            Alert.Priority.SEVERE -> {
                                // TODO
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setEventListeners() {
        bind.apply {
            exit.setOnClickListener {
                if(!isHeadLessBuildType()) {
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
            }
            settings.setOnClickListener {
                //TODO need to manage setting screen aswell
                if(!isHeadLessBuildType())
                    SettingsActivity.navigateToSetting(this@HomeActivity)
            }
        }
    }

    private fun subscribeToObservers() {
        viewModel.info.observe(this@HomeActivity) {
            when (it.getContentIfNotHandled()) {
                is Resource.Loading -> {
                    // TODO show progress bar
                }
                is Resource.Success -> {
                    if (it.peekContent().data?.access == false || isExpired(it.peekContent().data?.expiry)) {
                        showSnackBar("Your access has expired.")
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
                    sendCommandToService(
                        Constants.ACTION_STOP_SERVICE,
                        EndlessService::class.java
                    )
                    when (it.peekContent().errorData?.code) {
                        null -> {
                            var error = it.peekContent().message
                            if (error?.contains("Failed to connect to") == true)
                                error = "Failed to connect to server."
//                            showSnackBar(error)
                            AlertDialog.getInstance(
                                "Uh Oh!",
                                "Failed to connect to server.",
                                "Try again",
                                noText = "Exit",
                                yesListener = {
                                    viewModel.sendInfoRequest(InfoRequest(sharedPreferences.getDeviceID()!!))
                                },
                                noListener = {
                                    exitProcess(0)
                                }
                            ).show(supportFragmentManager, AlertDialog.TAG)
                            return@observe
                        }
                        400 -> {
                            AlertDialog.getInstance(
                                "Uh Oh!",
                                "Server Unreachable! (400)",
                                "Try again",
                                noText = "Exit",
                                yesListener = {
                                    viewModel.sendInfoRequest(InfoRequest(sharedPreferences.getDeviceID()!!))
                                },
                                noListener = {
                                    exitProcess(0)
                                }
                            ).show(supportFragmentManager, AlertDialog.TAG)
                        }
                        401 -> {
                            showSnackBar("Session expired.")
                            logout()
                        }
                        else -> {
                            val error = it.peekContent().errorData?.message
                            AlertDialog.getInstance(
                                "Uh Oh!",
                                "$error ",
                                "Try again",
                                noText = "Exit",
                                yesListener = {
                                    viewModel.sendInfoRequest(InfoRequest(sharedPreferences.getDeviceID()!!))
                                },
                                noListener = {
                                    exitProcess(0)
                                }
                            ).show(supportFragmentManager, AlertDialog.TAG)
                        }
                    }
                }
                null -> Unit
            }
        }
        assetManager.getAssetsLiveData().observe(this@HomeActivity) { assets ->
            (bind.assetRecycler.adapter as AssetAdapter).refreshAssets(assets)
        }

        InternetConnectionCheck(this).observe(this) { isConnected ->
            if (isConnected) {
                bind.ipRecycler.isVisible = true
                bind.connectToNetworkError.isVisible = false
                (bind.ipRecycler.adapter as IpAdapter).updateIpList(getIPAddressList(useIPv4 = bind.ipSwitch.isOn))
            } else {
                bind.ipRecycler.isVisible = false
                bind.connectToNetworkError.isVisible = true
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                EndlessService.eventsFlow.collect { event ->
                    when(event) {
                        is EndlessService.Companion.Events.Nothing -> Unit
                        is EndlessService.Companion.Events.Logout -> {
                            logout()
                        }
                    }
                }
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
        bind.ipRecycler.adapter = IpAdapter(this@HomeActivity, getIPAddressList()).apply {
            doOnLongClick { ipAddress ->
                if(this@HomeActivity.setClipboard("ip_address", ipAddress.address)) {
                    showSnackBar("Copied Ip Address to clipboard")
                }
            }
        }
        bind.assetRecycler.layoutManager = GridLayoutManager(this@HomeActivity, 4)
        bind.assetRecycler.adapter = AssetAdapter(
            this@HomeActivity,
            this@HomeActivity
        )
        (bind.assetRecycler.adapter as AssetAdapter).apply {
            doOnItemClicked { assetUI ->
                if (assetUI.assets.isEmpty()) return@doOnItemClicked

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

    private fun showSnackBar(
        msg: String?,
        indefinite: Boolean = false
    ) {
        runOnUiThread {
            if (msg != null) {
                Snackbar.make(
                    bind.root,
                    msg,
                    if (indefinite) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG
                ).show()
            }
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
            .registerReceiver(receiver,
                IntentFilter(PluginResultsService.RESULT_BROADCAST_INTENT).apply {
                    addAction(AlertManager.ANX_ALERTS_BROADCAST_INTENT)
                })
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
