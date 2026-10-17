package com.sceyt.chatuikit.extensions

import com.bumptech.glide.load.engine.GlideException
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

class GlideExtensionsTest {

    @Test
    fun `file not found root cause is detected`() {
        val exception = GlideException("Failed to load", listOf(FileNotFoundException()))

        assertThat(exception.isFileNotFound()).isTrue()
    }

    @Test
    fun `nested file not found root cause is detected`() {
        val nested = GlideException("Fetch failed", listOf(FileNotFoundException()))
        val exception = GlideException("Failed to load", listOf(nested))

        assertThat(exception.isFileNotFound()).isTrue()
    }

    @Test
    fun `other root cause is not file not found`() {
        val exception = GlideException("Failed to load", listOf(IOException("decode failed")))

        assertThat(exception.isFileNotFound()).isFalse()
    }

    @Test
    fun `null exception is not file not found`() {
        val exception: GlideException? = null

        assertThat(exception.isFileNotFound()).isFalse()
    }
}
