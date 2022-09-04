package com.flomobility.hermes.assets.types.camera

import com.flomobility.hermes.assets.BaseAsset
import com.flomobility.hermes.assets.BaseAssetConfig

abstract class Camera: BaseAsset {

    class Config: BaseAssetConfig() {

        val stream = Field<Stream>()

        val compressionQuality = Field<Int>()

        init {
            stream.range = listOf(
                Stream(
                    fps = 30,
                    width = 1920,
                    height = 1080,
                    pixelFormat = Stream.PixelFormat.MJPEG
                )
            )
            stream.name = "stream"
            stream.value = Stream.DEFAULT

            compressionQuality.name = "compression_quality"
            compressionQuality.value = DEFAULT_COMPRESSION_QUALITY
            compressionQuality.range = listOf(
                10, 25, 50, 75, 80, 85, 90, 95, 100
            )
        }

        data class Stream(
            val fps: Int,
            val width: Int,
            val height: Int,
            val pixelFormat: PixelFormat
        ) {
            enum class PixelFormat(val alias: String) {
                MJPEG("mjpeg"), YUYV("yuyv")
            }

            companion object {
                val DEFAULT = Stream(
                    fps = 30,
                    width = 1920,
                    height = 1080,
                    pixelFormat = Stream.PixelFormat.MJPEG
                )
            }
        }

        override fun getFields(): List<Field<*>> {
            return listOf(stream)
        }

        companion object {
            private const val DEFAULT_COMPRESSION_QUALITY = 90
        }

    }

}