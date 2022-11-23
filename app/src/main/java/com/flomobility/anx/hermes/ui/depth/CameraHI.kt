package com.flomobility.anx.hermes.ui.depth

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

class CameraHI(private val activity: ComponentActivity) {
    private val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

    private val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    private val previewCamera = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    private val previewExecutor = Executors.newSingleThreadExecutor()

    private val cameraProvider = cameraProviderFuture.get()

    private val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var camera: Camera

    var available = false

    private val activityResultLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()){ isGranted ->
            available = isGranted
        }

    private var imageCallback: ((ImageProxy) -> Unit)? = null

    fun onImage(callback : (ImageProxy) -> Unit) {
        imageCallback = callback
    }

    private var preview: ((ImageProxy) -> Unit)? = null

    fun previewImage(callback: (ImageProxy) -> Unit) {
        preview = callback
    }

    init{
        activityResultLauncher.launch(Manifest.permission.CAMERA)

        cameraProviderFuture.addListener(Runnable {
            imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
                imageCallback?.invoke(image)
                image.close()
            })
            previewCamera.setAnalyzer(previewExecutor, ImageAnalysis.Analyzer { image ->
                preview?.invoke(image)
                image.close()
            })
        }, ContextCompat.getMainExecutor(activity))

        activity.runOnUiThread {
            camera = cameraProvider.bindToLifecycle(activity as LifecycleOwner, cameraSelector, imageAnalysis, previewCamera)
        }
    }
}
