package com.sceyt.chatuikit.persistence.logicimpl

import com.google.common.truth.Truth.assertThat
import okhttp3.Call
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class DownloadCallRegistryTest {

    @Test
    fun `main and video thumb calls are tracked independently`() {
        val registry = DownloadCallRegistry()
        val mainKey = DownloadKey(MESSAGE_TID, VIDEO_URL)
        val thumbKey = DownloadKey(MESSAGE_TID, THUMB_URL)
        val mainCall = mock<Call>()
        val thumbCall = mock<Call>()

        registry.track(mainKey, mainCall)
        registry.track(thumbKey, thumbCall)

        assertThat(registry.contains(mainKey)).isTrue()
        assertThat(registry.contains(thumbKey)).isTrue()
        assertThat(registry.size).isEqualTo(2)
    }

    @Test
    fun `completing one call does not remove the other`() {
        val registry = DownloadCallRegistry()
        val mainKey = DownloadKey(MESSAGE_TID, VIDEO_URL)
        val thumbKey = DownloadKey(MESSAGE_TID, THUMB_URL)
        val mainCall = mock<Call>()
        val thumbCall = mock<Call>()
        registry.track(mainKey, mainCall)
        registry.track(thumbKey, thumbCall)

        assertThat(registry.remove(mainKey, mainCall)).isTrue()

        assertThat(registry.contains(mainKey)).isFalse()
        assertThat(registry.contains(thumbKey)).isTrue()
    }

    @Test
    fun `replacing the same file cancels the previous call`() {
        val registry = DownloadCallRegistry()
        val key = DownloadKey(MESSAGE_TID, VIDEO_URL)
        val previousCall = mock<Call>()
        val replacementCall = mock<Call>()
        registry.track(key, previousCall)

        registry.track(key, replacementCall)

        verify(previousCall).cancel()
        verify(replacementCall, never()).cancel()
        assertThat(registry.contains(key)).isTrue()
    }

    @Test
    fun `late callback cannot remove the replacement call`() {
        val registry = DownloadCallRegistry()
        val key = DownloadKey(MESSAGE_TID, VIDEO_URL)
        val previousCall = mock<Call>()
        val replacementCall = mock<Call>()
        registry.track(key, previousCall)
        registry.track(key, replacementCall)

        assertThat(registry.remove(key, previousCall)).isFalse()

        assertThat(registry.contains(key)).isTrue()
        assertThat(registry.remove(key, replacementCall)).isTrue()
        assertThat(registry.contains(key)).isFalse()
    }

    @Test
    fun `cancel by key cancels only that file`() {
        val registry = DownloadCallRegistry()
        val mainKey = DownloadKey(MESSAGE_TID, VIDEO_URL)
        val thumbKey = DownloadKey(MESSAGE_TID, THUMB_URL)
        val mainCall = mock<Call>()
        val thumbCall = mock<Call>()
        registry.track(mainKey, mainCall)
        registry.track(thumbKey, thumbCall)

        registry.cancel(mainKey)

        verify(mainCall).cancel()
        verify(thumbCall, never()).cancel()
        assertThat(registry.contains(mainKey)).isFalse()
        assertThat(registry.contains(thumbKey)).isTrue()
    }

    @Test
    fun `cancel by message cancels main and thumb calls`() {
        val registry = DownloadCallRegistry()
        val mainKey = DownloadKey(MESSAGE_TID, VIDEO_URL)
        val thumbKey = DownloadKey(MESSAGE_TID, THUMB_URL)
        val otherKey = DownloadKey(OTHER_MESSAGE_TID, VIDEO_URL)
        val mainCall = mock<Call>()
        val thumbCall = mock<Call>()
        val otherCall = mock<Call>()
        registry.track(mainKey, mainCall)
        registry.track(thumbKey, thumbCall)
        registry.track(otherKey, otherCall)

        registry.cancelMessage(MESSAGE_TID)

        verify(mainCall).cancel()
        verify(thumbCall).cancel()
        verify(otherCall, never()).cancel()
        assertThat(registry.containsMessage(MESSAGE_TID)).isFalse()
        assertThat(registry.contains(otherKey)).isTrue()
    }

    private companion object {
        const val MESSAGE_TID = 42L
        const val OTHER_MESSAGE_TID = 43L
        const val VIDEO_URL = "https://example.com/video.mp4"
        const val THUMB_URL = "https://example.com/video-thumb.jpg"
    }
}
