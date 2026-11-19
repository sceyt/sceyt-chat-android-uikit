package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.databinding.SceytItemIncVoiceMessageBinding
import com.sceyt.chatuikit.formatters.defaults.DefaultVoiceDurationFormatter
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.media.audio.AudioPlayerState
import com.sceyt.chatuikit.media.audio.AudioPlayerStatus
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.AttachmentMetadataPayload
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.Answers.RETURNS_DEEP_STUBS
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
class IncVoiceMessageViewHolderTest {
    @Test
    fun `voice bubble keeps the attachment duration when playback completes`() {
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<CoroutineContext>(named(CoroutineContextType.SingleThreaded)) {
                    Dispatchers.Unconfined
                }
            })
        }
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            val context = activity.get()
            context.setTheme(androidx.appcompat.R.style.Theme_AppCompat)
            val binding = SceytItemIncVoiceMessageBinding.inflate(context.layoutInflater)
            val style = mock<MessageItemStyle>(defaultAnswer = RETURNS_DEEP_STUBS) {
                on { voiceDurationFormatter } doReturn DefaultVoiceDurationFormatter()
            }
            val holder = IncVoiceMessageViewHolder(
                binding = binding,
                viewPoolReactions = RecyclerView.RecycledViewPool(),
                style = style,
                isViewOnce = false,
                messageListeners = mock(),
                displayedListener = null,
                needMediaDataCallback = {},
                voicePlayPauseListener = null,
            )
            val attachment = SceytAttachment(
                id = 1, messageId = 2, messageTid = 3, userId = null,
                name = "voice", type = AttachmentTypeEnum.Voice.value, metadata = null,
                fileSize = 1, createdAt = 1, url = null, filePath = "/voice.m4a",
                transferState = TransferState.Downloaded, progressPercent = 100f,
                originalFilePath = null, linkPreviewDetails = null,
            )
            val file = FileListItem(
                attachment, AttachmentMetadataPayload(duration = 6), null, null, AttachmentTypeEnum.Voice
            )
            val message = createMessage(createdAt = 1, id = 2, tid = 3)
                .copy(incoming = true, files = listOf(file))
            holder.bind(MessageListItem.MessageItem(message), MessageDiff.DEFAULT_FALSE)
            assertThat(binding.voiceDuration.text.toString()).isEqualTo("0:06")

            val playing = AudioPlayerState(
                filePath = attachment.filePath, messageTid = attachment.messageTid,
                status = AudioPlayerStatus.Playing, position = 1_020, duration = 7_040,
            )
            updatePlayback(holder, playing)
            assertThat(binding.voiceDuration.text.toString()).isEqualTo("0:01")

            updatePlayback(holder, playing.copy(status = AudioPlayerStatus.Completed, position = 0))
            assertThat(binding.voiceDuration.text.toString()).isEqualTo("0:06")
        } finally {
            activity.destroy()
            SceytKoinApp.koinApp = null
            stopKoin()
        }
    }

    private fun updatePlayback(holder: IncVoiceMessageViewHolder, state: AudioPlayerState) {
        ReflectionHelpers.callInstanceMethod<Any?>(
            holder, "onPlaybackStateChanged", ClassParameter.from(AudioPlayerState::class.java, state)
        )
    }
}
