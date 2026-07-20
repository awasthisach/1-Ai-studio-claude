package com.vvf.smartmanager.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FileCategoryTest {

    @Test
    fun `jpg maps to image`() {
        assertEquals(FileCategory.IMAGE, FileCategory.fromExtension("jpg"))
    }

    @Test
    fun `mp4 maps to video`() {
        assertEquals(FileCategory.VIDEO, FileCategory.fromExtension("mp4"))
    }

    @Test
    fun `pdf maps to pdf category`() {
        assertEquals(FileCategory.PDF, FileCategory.fromExtension("pdf"))
    }

    @Test
    fun `unknown extension maps to other`() {
        assertEquals(FileCategory.OTHER, FileCategory.fromExtension("xyz123"))
    }

    @Test
    fun `extension matching is case insensitive`() {
        assertEquals(FileCategory.IMAGE, FileCategory.fromExtension("JPG"))
    }
}
