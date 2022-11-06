package com.flomobility.anx.hermes.other

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

@SuppressLint("UnsafeOptInUsageError")
fun ImageProxy.toYUV(): YuvImage? {
    val image = this.image ?: return null
    val imageBuffer = image.planes?.toNV21(this.width, this.height)
    val yuvImage = YuvImage(
        imageBuffer?.toByteArray(),
        ImageFormat.NV21,
        this.width,
        this.height, null
    )
    image.close()
    return yuvImage
}

fun ImageProxy.toJpeg(compressionQuality: Int = 80): ByteBuffer?{
    val yuv = this.toYUV() ?: return null
    val stream = ByteArrayOutputStream()
    yuv.compressToJpeg(Rect(0, 0, this.width, this.height), compressionQuality, stream)
    return ByteBuffer.wrap(stream.toByteArray())
}

fun Array<Image.Plane>.toNV21(width: Int, height: Int): ByteBuffer {

    val imageSize = width * height
    val out = ByteArray(imageSize + 2 * (imageSize / 4))

    if (BufferUtils.areUVPlanesNV21(this, width, height)) {
        // Copy the Y values.
        this[0].buffer.get(out, 0, imageSize)
        val uBuffer: ByteBuffer = this[1].buffer
        val vBuffer: ByteBuffer = this[2].buffer
        // Get the first V value from the V buffer, since the U buffer does not contain it.
        vBuffer[out, imageSize, 1]
        // Copy the first U value and the remaining VU values from the U buffer.
        uBuffer[out, imageSize + 1, 2 * imageSize / 4 - 1]
    } else {
        // Fallback to copying the UV values one by one, which is slower but also works.
        // Unpack Y.
        BufferUtils.unpackPlane(this[0], width, height, out, 0, 1)
        // Unpack U.
        BufferUtils.unpackPlane(this[1], width, height, out, imageSize + 1, 2)
        // Unpack V.
        BufferUtils.unpackPlane(this[2], width, height, out, imageSize, 2)
    }

    return ByteBuffer.wrap(out)
}

fun ImageProxy.toByteArray(): ByteArray {
    val imageSize = width * height
    val out = ByteArray(imageSize + 2 * (imageSize / 4))

    this.planes[0].buffer.get(out, 0, imageSize)
    // Get the first V value from the V buffer, since the U buffer does not contain it.
    this.planes[2].buffer[out, imageSize, 1]
    // Copy the first U value and the remaining VU values from the U buffer.
    this.planes[1].buffer[out, imageSize + 1, 2 * imageSize / 4 - 1]
    this.close()
    return out
}
