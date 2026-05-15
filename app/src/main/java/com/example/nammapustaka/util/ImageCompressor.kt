package com.example.nammapustaka.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object ImageCompressor {
    private const val MAX_BYTES = 200 * 1024

    fun saveJpegUnderMaxBytes(source: Bitmap, dest: File): Boolean {
        dest.parentFile?.mkdirs()
        var quality = 92
        var bytes: ByteArray
        do {
            val baos = java.io.ByteArrayOutputStream()
            if (!source.compress(Bitmap.CompressFormat.JPEG, quality, baos)) return false
            bytes = baos.toByteArray()
            quality -= 7
            if (quality < 30) {
                val scaled = Bitmap.createScaledBitmap(
                    source,
                    (source.width * 0.85f).roundToInt().coerceAtLeast(1),
                    (source.height * 0.85f).roundToInt().coerceAtLeast(1),
                    true,
                )
                if (scaled != source) source.recycle()
                return saveJpegUnderMaxBytes(scaled, dest)
            }
        } while (bytes.size > MAX_BYTES && quality > 28)
        FileOutputStream(dest).use { it.write(bytes) }
        return true
    }

    fun decodeSampled(path: String, maxSide: Int = 1280): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        var sample = 1
        while (opts.outWidth / sample > maxSide || opts.outHeight / sample > maxSide) {
            sample *= 2
        }
        return BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inJustDecodeBounds = false
            },
        )
    }
}
