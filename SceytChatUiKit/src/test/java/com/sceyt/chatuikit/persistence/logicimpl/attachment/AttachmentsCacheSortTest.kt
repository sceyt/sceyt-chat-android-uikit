package com.sceyt.chatuikit.persistence.logicimpl.attachment

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AttachmentsCacheSortTest {

    private val imageType = AttachmentTypeEnum.Image.value
    private val videoType = AttachmentTypeEnum.Video.value

    private fun attachment(id: Long, createdAt: Long, type: String = imageType) = SceytAttachment(
        id = id,
        messageId = id,
        messageTid = id,
        userId = null,
        name = "attachment-$id",
        type = type,
        metadata = null,
        fileSize = 0L,
        createdAt = createdAt,
        url = "https://cdn.example/$id",
        filePath = null,
        transferState = null,
        progressPercent = 0f,
        originalFilePath = null,
        linkPreviewDetails = null,
    )

    @Test
    fun `getSorted orders by createdAt before id`() = runTest {
        val cache = AttachmentsCache()
        cache.addAll(
            listOf(
                attachment(id = 1L, createdAt = 300L),
                attachment(id = 2L, createdAt = 100L),
                attachment(id = 3L, createdAt = 200L),
            ),
            checkDifference = false,
        )

        assertThat(cache.getSorted(listOf(imageType), desc = false).map { it.id })
            .containsExactly(2L, 3L, 1L).inOrder()
        assertThat(cache.getSorted(listOf(imageType), desc = true).map { it.id })
            .containsExactly(1L, 3L, 2L).inOrder()
    }

    @Test
    fun `getSorted breaks createdAt ties by id`() = runTest {
        val cache = AttachmentsCache()
        cache.addAll(
            listOf(
                attachment(id = 20L, createdAt = 100L),
                attachment(id = 10L, createdAt = 100L),
                attachment(id = 30L, createdAt = 50L),
            ),
            checkDifference = false,
        )

        assertThat(cache.getSorted(listOf(imageType), desc = false).map { it.id })
            .containsExactly(30L, 10L, 20L).inOrder()
        assertThat(cache.getSorted(listOf(imageType), desc = true).map { it.id })
            .containsExactly(20L, 10L, 30L).inOrder()
    }

    @Test
    fun `getSorted merges requested types into one ordered list`() = runTest {
        val cache = AttachmentsCache()
        cache.addAll(
            listOf(
                attachment(id = 1L, createdAt = 100L, type = imageType),
                attachment(id = 2L, createdAt = 300L, type = videoType),
                attachment(id = 3L, createdAt = 200L, type = imageType),
            ),
            checkDifference = false,
        )

        assertThat(cache.getSorted(listOf(imageType, videoType), desc = false).map { it.id })
            .containsExactly(1L, 3L, 2L).inOrder()
        assertThat(cache.getSorted(listOf(imageType), desc = false).map { it.id })
            .containsExactly(1L, 3L).inOrder()
    }
}
