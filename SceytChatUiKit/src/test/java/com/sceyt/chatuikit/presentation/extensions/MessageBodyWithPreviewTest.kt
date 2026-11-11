package com.sceyt.chatuikit.presentation.extensions

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytMessageType
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.persistence.logicimpl.attachment
import com.sceyt.chatuikit.styles.common.TextStyle
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MessageBodyWithPreviewTest {
    private val context = mock<Context>().also {
        whenever(it.getString(R.string.sceyt_file)).thenReturn("File")
    }

    @Test
    fun `video with preview shows its caption`() {
        assertThat(format(videoMessage(body = "caption"))).isEqualTo("caption")
    }

    @Test
    fun `video with preview and no caption shows the video name`() {
        assertThat(format(videoMessage(body = ""))).isEqualTo("video.mp4")
    }

    private fun videoMessage(body: String): SceytMessage {
        val video = attachment(name = "video.mp4", type = AttachmentTypeEnum.Video.value)
        val preview = attachment(name = "video_preview.jpg", type = AttachmentTypeEnum.Preview.value)
        return createMessage(createdAt = 1L, tid = 1L).copy(
            body = body,
            type = SceytMessageType.Media.value,
            attachments = listOf(video, preview),
        )
    }

    private fun format(message: SceytMessage): String = message.getFormattedBodyWithAttachments(
        context = context,
        mentionTextStyle = TextStyle(),
        attachmentNameFormatter = Formatter { _, attachment -> attachment.name },
        mentionUserNameFormatter = Formatter { _, user -> user.id },
        unsupportedMessageBodyFormatter = Formatter { _, _ -> "unsupported" },
        mentionClickListener = null,
    ).toString()
}
