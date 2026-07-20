package com.vvf.smartmanager.core.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Level 3 duplicate detection (Master Specification v2.0, Section 9): visual similarity
 * for resized/re-compressed/cropped variants of the same image.
 *
 * Implementation: difference hash (dHash). The image is downscaled to a 9x8 grayscale
 * grid; each of the 64 bits records whether a pixel is brighter than its right neighbour.
 * This needs no bundled AI model — pure bitmap math — and is resistant to resizing and
 * moderate JPEG re-compression, unlike a naive byte-for-byte hash.
 *
 * Two images are "visually similar" when the Hamming distance between their hashes is
 * below a threshold (UI exposes this as the 70%-95% similarity slider from the Blueprint).
 */
object PerceptualHash {

    private const val HASH_WIDTH = 9
    private const val HASH_HEIGHT = 8

    /** Returns a 64-bit dHash, or null if the file couldn't be decoded as an image. */
    fun compute(imagePath: String): Long? {
        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
        val original = BitmapFactory.decodeFile(imagePath, options) ?: return null
        val scaled = Bitmap.createScaledBitmap(original, HASH_WIDTH, HASH_HEIGHT, true)
        if (scaled !== original) original.recycle()

        var hash = 0L
        var bitIndex = 0
        for (y in 0 until HASH_HEIGHT) {
            for (x in 0 until HASH_WIDTH - 1) {
                val leftGray = grayscale(scaled.getPixel(x, y))
                val rightGray = grayscale(scaled.getPixel(x + 1, y))
                if (leftGray > rightGray) {
                    hash = hash or (1L shl bitIndex)
                }
                bitIndex++
            }
        }
        scaled.recycle()
        return hash
    }

    /** 0 = identical, 64 = completely different. A distance under ~10 is "visually similar". */
    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    /** Converts a 0-63 Hamming distance into an approximate 0-100% similarity score for the UI slider. */
    fun similarityPercent(distance: Int): Int = (100 - (distance * 100 / 64)).coerceIn(0, 100)

    private fun grayscale(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }
}
