package com.flomobility.anx.hermes.ui.depth

import android.annotation.SuppressLint
import android.graphics.*
import android.media.Image
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.flomobility.anx.R
import com.flomobility.anx.databinding.ActivityDepthImageBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream

@SuppressLint("UnsafeOptInUsageError")
@AndroidEntryPoint
class DepthImageActivity : ComponentActivity() {

    lateinit var cameraHI: CameraHI

    private var _binding: ActivityDepthImageBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityDepthImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cameraHI = CameraHI(this)
        cameraHI.onImage { image ->
            binding.imgOriginal

            var bitmap = rotate(toBitmap(image.image!!), image.imageInfo.rotationDegrees.toFloat())
            /*if (resizeSwitch.isChecked){
                var desBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
                resize(bitmap, desBitmap, 500, 500)
                bitmap = desBitmap
            }
            if (blurSwitch.isChecked){
                blur(bitmap, bitmap, 5.0)
            }
            if(bwSwitch.isChecked){
                val begin = System.currentTimeMillis()
                depthMidas(midasAddr, bitmap, bitmap)
                val end = System.currentTimeMillis()
                runOnUiThread{
                    latencyView.text = "${end-begin}ms"
                }
//            bw(bitmap, bitmap)
            }*/
            runOnUiThread{
                binding.imgOriginal.setImageBitmap(bitmap)
            }
        }
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
