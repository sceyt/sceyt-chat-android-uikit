package com.sceyt.chatuikit.formatters.defaults

import android.content.Context
import android.graphics.Color
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.DraftMessage
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.formatters.attributes.ChannelItemSubtitleFormatterAttributes
import com.sceyt.chatuikit.styles.channel.ChannelItemStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultChannelListSubtitleFormatterDraftTest {

    private val formatter = DefaultChannelListSubtitleFormatter()

    /**
     * Unit tests of this module run without android resources, so the used strings are stubbed.
     */
    private val context = mock<Context>().also {
        whenever(it.getString(R.string.sceyt_draft)).thenReturn("Draft")
        whenever(it.getString(R.string.sceyt_reply)).thenReturn("Reply")
    }

    @Test
    fun `draft with body shows draft prefix with body`() {
        val (hasDraft, subtitle) = formatDraft(draft(body = "hello"))

        assertThat(hasDraft).isTrue()
        assertThat(subtitle.toString()).isEqualTo("Draft: hello")
    }

    @Test
    fun `reply draft without body shows draft prefix with reply`() {
        val (hasDraft, subtitle) = formatDraft(
            draft(body = null, replyOrEditMessage = mock(), isReply = true)
        )

        assertThat(hasDraft).isTrue()
        assertThat(subtitle.toString()).isEqualTo("Draft: Reply")
    }

    @Test
    fun `reply draft with blank body shows draft prefix with reply`() {
        val (_, subtitle) = formatDraft(
            draft(body = "   ", replyOrEditMessage = mock(), isReply = true)
        )

        assertThat(subtitle.toString()).isEqualTo("Draft: Reply")
    }

    @Test
    fun `reply draft prefix keeps draft prefix style`() {
        val (_, subtitle) = formatDraft(
            draft(body = null, replyOrEditMessage = mock(), isReply = true)
        )

        assertThat(subtitle.prefixColorSpanEnd()).isEqualTo("Draft:".length)
    }

    @Test
    fun `edit draft without body shows only draft word`() {
        val (hasDraft, subtitle) = formatDraft(
            draft(body = null, replyOrEditMessage = mock(), isReply = false)
        )

        assertThat(hasDraft).isTrue()
        assertThat(subtitle.toString()).isEqualTo("Draft")
    }

    @Test
    fun `empty draft shows only draft word keeping the prefix style`() {
        val (hasDraft, subtitle) = formatDraft(draft(body = null))

        assertThat(hasDraft).isTrue()
        assertThat(subtitle.toString()).isEqualTo("Draft")
        assertThat(subtitle.prefixColorSpanEnd()).isEqualTo("Draft".length)
    }

    @Test
    fun `channel without draft is not handled as draft`() {
        val (hasDraft, subtitle) = formatDraft(null)

        assertThat(hasDraft).isFalse()
        assertThat(subtitle.toString()).isEmpty()
    }

    private fun formatDraft(draftMessage: DraftMessage?): Pair<Boolean, CharSequence> {
        val channel = mock<SceytChannel>()
        whenever(channel.draftMessage).thenReturn(draftMessage)

        return formatter.checkHasDraftMessage(
            context, ChannelItemSubtitleFormatterAttributes(
                channel = channel,
                channelItemStyle = style()
            )
        )
    }

    private fun style() = mock<ChannelItemStyle>().also {
        whenever(it.draftPrefixTextStyle).thenReturn(TextStyle(color = Color.RED))
        whenever(it.mentionTextStyle).thenReturn(TextStyle())
        whenever(it.draftMessageBodyFormatter)
            .thenReturn(DefaultDraftMessageBodyWithAttachmentsFormatter())
        whenever(it.attachmentIconProvider).thenReturn(mock())
    }

    private fun draft(
        body: String?,
        replyOrEditMessage: SceytMessage? = null,
        isReply: Boolean = false,
    ) = DraftMessage(
        channelId = 1L,
        body = body,
        createdAt = 1L,
        mentionUsers = null,
        replyOrEditMessage = replyOrEditMessage,
        isReply = isReply,
        bodyAttributes = null,
        attachments = null,
        voiceAttachment = null,
        viewOnce = false
    )

    /** End index of the draft-prefix color span, or -1 if the prefix lost its style. */
    private fun CharSequence.prefixColorSpanEnd(): Int {
        val spanned = this as? Spanned ?: return -1
        val span = spanned.getSpans(0, spanned.length, ForegroundColorSpan::class.java)
            .firstOrNull { spanned.getSpanStart(it) == 0 } ?: return -1
        return spanned.getSpanEnd(span)
    }
}
