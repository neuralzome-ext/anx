package com.flomobility.hermes.ui.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.appcompat.content.res.AppCompatResources
import com.flomobility.hermes.R
import com.flomobility.hermes.databinding.ActivitySettingsBinding
import com.flomobility.hermes.other.*
import com.flomobility.hermes.other.viewutils.AlertDialog
import com.flomobility.hermes.ui.home.HomeActivity
import com.flomobility.hermes.ui.login.LoginActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    companion object {
        fun navigateToSetting(context: Context) {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }

    private var binding: ActivitySettingsBinding? = null
    private val bind get() = binding!!

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        if (isExpired(sharedPreferences.getDeviceExpiry())) {
            showSnack("Your access has been revoked")
            logout()
            return
        }
        setupEventListeners()
        setExpiry()
    }

    private fun setupEventListeners() {
        bind.apply {
            onBootSwitch.isChecked = sharedPreferences.getIsOnBoot()
            backBtn.setOnClickListener {
                onBackPressed()
            }
            thirdPartyTxt.setOnClickListener {

            }
            termsTxt.setOnClickListener {

            }
            privacyTxt.setOnClickListener {

            }
            userTxt.setOnClickListener {

            }
            onBootSwitch.setOnCheckedChangeListener { _, checked ->
                sharedPreferences.setIsOnBoot(checked)
            }
            logoutBtn.setOnClickListener {
                AlertDialog.getInstance(
                    "Confirm Logout",
                    "Sure to logout?",
                    "Logout",
                    "Cancel",
                    yesListener = {
                        logout()
                    }
                ).show(supportFragmentManager, AlertDialog.TAG)
            }
        }
    }

    private fun setExpiry() {
        val formatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
//        val expiry = formatter.parse(sharedPreferences.getDeviceExpiry()?.split("T")?.get(0))
        val expiry = formatter.parse(
            sharedPreferences.getDeviceExpiry()?.replace("T", " ")?.replace("Z", "")
        )
        val now = Date()
//        Timber.d("Ayustark Date $expiry ${(expiry.time - now.time)/86400000}")
        val validity = ((expiry.time - now.time) / 86400000).toInt()

        bind.validityTxt.setTextColor(
            ContextCompat.getColor(
                this,
                when {
                    validity == 0 -> R.color.red
                    validity < 7 -> R.color.orange
                    else -> R.color.validityColor
                }
            )
        )
        bind.validityTxt.text = "${validity} days left"
    }

    private fun logout() {
        sharedPreferences.clear()
        LoginActivity.navigateToLogin(this@SettingsActivity)
        finish()
    }

    private fun showSnack(msg: String?) {
        runOnUiThread {
            if (msg != null)
                Snackbar.make(bind.root, msg, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
        HomeActivity.navigateToHome(this@SettingsActivity)
        finish()
        super.onBackPressed()
    }
}