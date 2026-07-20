package com.vvf.smartmanager.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FileSizeFormatterTest {

    @Test
    fun `zero bytes formats as 0 B`() {
        assertEquals("0 B", FileSizeFormatter.format(0))
    }

    @Test
    fun `bytes under 1024 stay in B`() {
        assertEquals("512.0 B", FileSizeFormatter.format(512))
    }

    @Test
    fun `exactly 1024 bytes formats as KB`() {
        assertEquals("1.0 KB", FileSizeFormatter.format(1024))
    }

    @Test
    fun `megabyte range formats correctly`() {
        assertEquals("1.5 MB", FileSizeFormatter.format((1.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun `gigabyte range formats correctly`() {
        assertEquals("2.0 GB", FileSizeFormatter.format(2L * 1024 * 1024 * 1024))
    }
}
