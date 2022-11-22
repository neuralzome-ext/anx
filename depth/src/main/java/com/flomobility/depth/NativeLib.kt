package com.flomobility.depth

import android.content.res.AssetManager
import android.graphics.Bitmap

class NativeLib {

    /**
     * A native method that is implemented by the 'depth' native library,
     * which is packaged with this application.
     */
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun blur(bitmapIn: Bitmap, bitmapOut: Bitmap, sigma: Double)
    external fun bw(bitmapIn: Bitmap, bitmapOut: Bitmap)
    external fun resize(bitmapIn: Bitmap, bitmapOut: Bitmap, sizeX: Int, sizeY: Int)
    external fun initMidas(assetManager: AssetManager?, modelname: String): Long
    external fun destroyMidas(midasAddr: Long)
    external fun depthMidas(midasAddr: Long, bitmapIn: Bitmap, bitmapOut: Bitmap)

    companion object {
        // Used to load the 'depth' library on application startup.
        init {
            System.loadLibrary("depth")
        }
    }
}
