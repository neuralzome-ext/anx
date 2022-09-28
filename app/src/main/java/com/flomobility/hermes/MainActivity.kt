package com.flomobility.hermes

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.flomobility.hermes.daemon.EndlessService
import com.flomobility.hermes.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import com.flomobility.hermes.other.Constants

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()

        setOnEventListeners()

        sendCommandToService(Constants.ACTION_START_OR_RESUME_SERVICE, EndlessService::class.java)
    }

    private fun setOnEventListeners() {
        binding.rootLyt.setOnLongClickListener {
            // toggle debug mode
            return@setOnLongClickListener true
        }
    }

    /**
     * A native method that is implemented by the 'hermes' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'hermes' library on application startup.
        init {
            System.loadLibrary("hermes")
        }
    }

    /**
     * Sends the action string to the specified service
     * @param action The action string which the service will refer to, to execute a set of tasks
     * @param serviceClass The ServiceClass to which the action string is to be sent
     * @see Constants.ACTION_START_OR_RESUME_SERVICE
     * @see Constants.ACTION_PAUSE_SERVICE
     * @see Constants.ACTION_STOP_SERVICE
     * */
    private fun sendCommandToService(action: String, serviceClass: Class<*>) {
        Intent(this, serviceClass).also {
            it.action = action
            startService(it)
        }
    }
}