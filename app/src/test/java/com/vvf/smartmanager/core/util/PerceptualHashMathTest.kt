package com.vvf.smartmanager.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PerceptualHashMathTest {

    @Test
    fun `identical hashes have zero hamming distance`() {
        val hash = 0b1010101010101010L
        assertEquals(0, PerceptualHash.hammingDistance(hash, hash))
    }

    @Test
    fun `completely different hashes have max hamming distance for set bits`() {
        val a = 0L
        val b = -1L // all 64 bits set
        assertEquals(64, PerceptualHash.hammingDistance(a, b))
    }

    @Test
    fun `similarity percent is 100 for zero distance`() {
        assertEquals(100, PerceptualHash.similarityPercent(0))
    }

    @Test
    fun `similarity percent is 0 for max distance`() {
        assertEquals(0, PerceptualHash.similarityPercent(64))
    }

    @Test
    fun `similarity percent is roughly half for half distance`() {
        val result = PerceptualHash.similarityPercent(32)
        assertEquals(50, result)
    }
}
