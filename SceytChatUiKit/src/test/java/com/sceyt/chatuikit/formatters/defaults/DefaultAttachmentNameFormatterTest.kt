package com.sceyt.chatuikit.formatters.defaults

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DefaultAttachmentNameFormatterTest {

    @Test
    fun `file attachment shows file name`() {
        val context = RuntimeEnvironment.getApplication()
        val attachment = attachment("video.mp4")

        assertThat(DefaultAttachmentNameFormatter().format(context, attachment).toString())
            .isEqualTo("video.mp4")
    }

    private fun attachment(name: String) = SceytAttachment(
        id = 1L,
        messageId = 1L,
        messageTid = 1L,
        userId = null,
        name = name,
        type = AttachmentTypeEnum.File.value,
        metadata = null,
        fileSize = 100L,
        createdAt = 1_000L,
        url = null,
        filePath = null,
        transferState = null,
        progressPercent = null,
        originalFilePath = null,
        linkPreviewDetails = null,
    )
}
