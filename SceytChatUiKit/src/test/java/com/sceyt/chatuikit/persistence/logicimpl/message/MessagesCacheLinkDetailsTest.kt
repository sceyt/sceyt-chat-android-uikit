package com.sceyt.chatuikit.persistence.logicimpl.message

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MessagesCacheLinkDetailsTest {

    private val channelId = 1L
    private val link = "https://example.com/article"

    private val full = LinkPreviewDetails(
        link = link,
        title = "Title",
        url = link,
        description = "Description",
        siteName = "Example",
        faviconUrl = "https://example.com/favicon.ico",
        imageUrl = "https://example.com/image.png",
        imageWidth = 1200,
        imageHeight = 630,
        thumb = "thumb",
        hideDetails = false
    )

    private val partial = LinkPreviewDetails(
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
    fun `incoming partial details keep cached details`() = runTest {
        val cache = MessagesCache()
        cache.add(channelId, messageWithLink(full))
        cache.upsertMessages(channelId, messageWithLink(partial.copy(hideDetails = true)))

        assertThat(cachedDetails(cache)).isEqualTo(full.copy(hideDetails = true))
    }

    @Test
    fun `incoming partial details keep cached details matched by link`() = runTest {
        val cache = MessagesCache()
        val canonical = full.copy(url = "https://example.com/canonical")
        cache.add(channelId, messageWithLink(canonical))
        cache.upsertMessages(channelId, messageWithLink(partial))

        assertThat(cachedDetails(cache)).isEqualTo(canonical.copy(url = link))
    }

    @Test
    fun `incoming non blank details override cached details`() = runTest {
        val cache = MessagesCache()
        cache.add(channelId, messageWithLink(full))
        val incoming = full.copy(title = "New title", thumb = "newThumb")
        cache.upsertMessages(channelId, messageWithLink(incoming))

        assertThat(cachedDetails(cache)).isEqualTo(incoming)
    }

    @Test
    fun `same message with changed image does not keep cached image data`() = runTest {
        val cache = MessagesCache()
        cache.add(channelId, messageWithLink(full))
        val incoming = full.copy(
            imageUrl = "https://example.com/new.png",
            imageWidth = null,
            imageHeight = null,
            thumb = null
        )
        cache.upsertMessages(channelId, messageWithLink(incoming))

        assertThat(cachedDetails(cache)).isEqualTo(incoming)
    }

    private suspend fun cachedDetails(cache: MessagesCache): LinkPreviewDetails? {
        return cache.get(channelId, 1L)?.attachments?.single()?.linkPreviewDetails
    }

    private fun messageWithLink(details: LinkPreviewDetails): SceytMessage {
        val attachment = SceytAttachment(
            id = 1L,
            messageId = 1L,
            messageTid = 1L,
            userId = null,
            name = details.title.orEmpty(),
            type = AttachmentTypeEnum.Link.value,
            metadata = null,
            fileSize = 0L,
            createdAt = 1L,
            url = link,
            filePath = null,
            transferState = null,
            progressPercent = 0f,
            originalFilePath = null,
            linkPreviewDetails = details,
        )
        return createMessage(createdAt = 1L, id = 1L, tid = 1L).copy(channelId = channelId, attachments = listOf(attachment))
    }
}
