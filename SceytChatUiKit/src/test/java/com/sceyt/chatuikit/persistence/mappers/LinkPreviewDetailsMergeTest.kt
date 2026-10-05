package com.sceyt.chatuikit.persistence.mappers

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import org.junit.Test

class LinkPreviewDetailsMergeTest {

    private val link = "https://example.com/article"

    private val full = LinkPreviewDetails(
        link = link,
        title = "Title",
        url = "https://example.com/canonical",
        description = "Description",
        siteName = "Example",
        faviconUrl = "https://example.com/favicon.ico",
        imageUrl = "https://example.com/image.png",
        imageWidth = 1200,
        imageHeight = 630,
        thumb = "thumb",
        hideDetails = false
    )

    private val partialFromMetadata = LinkPreviewDetails(
        link = link,
        title = "",
        url = link,
        description = null,
        siteName = "",
        faviconUrl = null,
        imageUrl = null,
        imageWidth = null,
        imageHeight = null,
        thumb = null,
        hideDetails = false
    )

    @Test
    fun `partial old and full newer gives full`() {
        assertThat(partialFromMetadata.mergeWith(full)).isEqualTo(full)
    }

    @Test
    fun `full old and partial newer keeps old values`() {
        val merged = full.mergeWith(partialFromMetadata)

        assertThat(merged.title).isEqualTo(full.title)
        assertThat(merged.description).isEqualTo(full.description)
        assertThat(merged.siteName).isEqualTo(full.siteName)
        assertThat(merged.faviconUrl).isEqualTo(full.faviconUrl)
        assertThat(merged.imageUrl).isEqualTo(full.imageUrl)
        assertThat(merged.imageWidth).isEqualTo(full.imageWidth)
        assertThat(merged.imageHeight).isEqualTo(full.imageHeight)
        assertThat(merged.thumb).isEqualTo(full.thumb)
        assertThat(merged.url).isEqualTo(partialFromMetadata.url)
    }

    @Test
    fun `full old and hidden link newer keeps old values`() {
        val merged = full.mergeWith(LinkPreviewDetails.hiddenLink(link))

        assertThat(merged).isEqualTo(full.copy(hideDetails = true))
    }

    @Test
    fun `newer zero size keeps old size`() {
        val merged = full.mergeWith(full.copy(imageWidth = 0, imageHeight = 0))

        assertThat(merged.imageWidth).isEqualTo(full.imageWidth)
        assertThat(merged.imageHeight).isEqualTo(full.imageHeight)
    }

    @Test
    fun `newer size with one side missing keeps old size`() {
        val merged = full.mergeWith(full.copy(imageWidth = 800, imageHeight = null))

        assertThat(merged.imageWidth).isEqualTo(full.imageWidth)
        assertThat(merged.imageHeight).isEqualTo(full.imageHeight)
    }

    @Test
    fun `newer negative size keeps old size`() {
        val merged = full.mergeWith(full.copy(imageWidth = -1, imageHeight = 630))

        assertThat(merged.imageWidth).isEqualTo(full.imageWidth)
        assertThat(merged.imageHeight).isEqualTo(full.imageHeight)
    }

    @Test
    fun `newer whitespace values keep old values`() {
        val merged = full.mergeWith(full.copy(title = "   ", description = "\t", imageUrl = " "))

        assertThat(merged).isEqualTo(full)
    }

    @Test
    fun `newer valid size overrides old size`() {
        val merged = full.mergeWith(full.copy(imageWidth = 800, imageHeight = 400))

        assertThat(merged.imageWidth).isEqualTo(800)
        assertThat(merged.imageHeight).isEqualTo(400)
    }

    @Test
    fun `newer non blank values override old values`() {
        val newer = full.copy(
            title = "New title",
            imageUrl = "https://example.com/new.png",
            thumb = "newThumb"
        )

        assertThat(full.mergeWith(newer)).isEqualTo(newer)
    }

    @Test
    fun `newer image url without size keeps old size and thumb`() {
        val newer = partialFromMetadata.copy(imageUrl = "https://example.com/new.png")
        val merged = full.mergeWith(newer)

        assertThat(merged.imageUrl).isEqualTo(newer.imageUrl)
        assertThat(merged.imageWidth).isEqualTo(full.imageWidth)
        assertThat(merged.imageHeight).isEqualTo(full.imageHeight)
        assertThat(merged.thumb).isEqualTo(full.thumb)
    }

    @Test
    fun `hide details taken from newer`() {
        assertThat(full.mergeWith(full.copy(hideDetails = true)).hideDetails).isTrue()
        assertThat(full.copy(hideDetails = true).mergeWith(full).hideDetails).isFalse()
    }

    @Test
    fun `entity merge keeps full row against partial row`() {
        val merged = full.toLinkDetailsEntity().mergeWith(partialFromMetadata.toLinkDetailsEntity())

        assertThat(merged).isEqualTo(full.toLinkDetailsEntity().copy(url = link))
    }

    @Test
    fun `server row replaces stored text and keeps measured image data for same image`() {
        val stored = full.toLinkDetailsEntity()
        val server = full.toLinkDetailsEntity().copy(
            title = "Server title",
            description = null,
            imageWidth = null,
            imageHeight = null,
            thumb = null
        )

        val result = server.keepImageDataFrom(stored)

        assertThat(result).isEqualTo(server.copy(imageWidth = 1200, imageHeight = 630, thumb = "thumb"))
    }

    @Test
    fun `server row with different image drops stored image data`() {
        val stored = full.toLinkDetailsEntity()
        val server = stored.copy(imageUrl = "https://example.com/new.png", imageWidth = null, imageHeight = null, thumb = null)

        assertThat(server.keepImageDataFrom(stored)).isEqualTo(server)
    }

    @Test
    fun `server row without image clears stored image`() {
        val stored = full.toLinkDetailsEntity()
        val server = stored.copy(imageUrl = null, imageWidth = null, imageHeight = null, thumb = null)

        assertThat(server.keepImageDataFrom(stored)).isEqualTo(server)
    }

    @Test
    fun `server row with own size keeps server size`() {
        val stored = full.toLinkDetailsEntity()
        val server = stored.copy(imageWidth = 800, imageHeight = 400, thumb = null)

        assertThat(server.keepImageDataFrom(stored)).isEqualTo(server.copy(thumb = "thumb"))
    }

    @Test
    fun `server row without stored row stays unchanged`() {
        val server = full.toLinkDetailsEntity()

        assertThat(server.keepImageDataFrom(null)).isEqualTo(server)
    }

    @Test
    fun `entity merge fills partial row from full row`() {
        val merged = partialFromMetadata.toLinkDetailsEntity().mergeWith(full.toLinkDetailsEntity())

        assertThat(merged).isEqualTo(full.toLinkDetailsEntity())
    }
}
