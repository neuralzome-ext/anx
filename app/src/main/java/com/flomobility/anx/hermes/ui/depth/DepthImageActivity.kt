package com.flomobility.anx.hermes.ui.depth

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.Image
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.flomobility.anx.R
import com.flomobility.anx.databinding.ActivityDepthImageBinding
import com.flomobility.depth.NativeLib
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@SuppressLint("UnsafeOptInUsageError")
@AndroidEntryPoint
class DepthImageActivity : ComponentActivity() {

    companion object {
        fun navigateToDepthImageActivity(context: Context) {
            context.startActivity(Intent(context, DepthImageActivity::class.java))
        }
    }

    lateinit var cameraHI: CameraHI

    private var _binding: ActivityDepthImageBinding? = null
    private val binding get() = _binding!!

    var midasAddr = 0L

    private lateinit var nativeLib: NativeLib

    private var bitmap: Bitmap? = null

    private var dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityDepthImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        nativeLib = NativeLib()
        midasAddr = nativeLib.initMidas(assets, "")
        cameraHI = CameraHI(this)
        cameraHI.onImage { image ->
            val original =
                rotate(toBitmap(image.image!!), image.imageInfo.rotationDegrees.toFloat())
            bitmap = original.copy(Bitmap.Config.ARGB_8888, true)
            bitmap?.let {
                val start = System.currentTimeMillis()
                nativeLib.depthMidas(midasAddr, it, it)
                val end = System.currentTimeMillis()
                runOnUiThread {
                    binding.imgDepth.setImageBitmap(it)
                    binding.tvInferenceTime.text = "Inference time : ${end - start} ms"
                }
            }
        }
        cameraHI.previewImage { image ->
            val original =
                rotate(toBitmap(image.image!!), image.imageInfo.rotationDegrees.toFloat())
            runOnUiThread {
                binding.imgOriginal.setImageBitmap(original)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nativeLib.destroyMidas(midasAddr)
    }

    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun toBitmap(image: Image): Bitmap {
        val yBuffer = image.planes[0].buffer // Y
        val vuBuffer = image.planes[2].buffer // VU

        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()

        val nv21 = ByteArray(ySize + vuSize)

        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}
