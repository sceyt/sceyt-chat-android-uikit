package com.sceyt.chatuikit.presentation.components.global_search.files

import android.content.Context
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentKind
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentResult
import com.sceyt.chatuikit.databinding.SceytItemChannelFileBinding
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.persistence.di.CoroutineContextType
import com.sceyt.chatuikit.persistence.file_transfer.AttachmentTransferStateStore
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.components.global_search.files.adapter.holders.FilesSearchItemViewHolder
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.channel_info.files.ChannelInfoFileItemStyle
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.Colors
import java.util.Date
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class FilesSearchItemViewHolderTest {

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var binding: SceytItemChannelFileBinding
    private lateinit var holder: FilesSearchItemViewHolder
    private lateinit var originalDateFormatter: Formatter<Date>
    private lateinit var originalColors: Colors
    private val downloadRequests = mutableListOf<NeedMediaInfoData>()

    @Before
    fun setUp() {
        // FileTransferHelper, observed by BaseFileViewHolder, resolves its dependencies on init.
        stopKoin()
        SceytKoinApp.koinApp = startKoin {
            modules(module {
                single<CoroutineContext>(named(CoroutineContextType.SingleThreaded)) {
                    Dispatchers.Unconfined
                }
            })
        }
        AttachmentTransferStateStore.clear()
        originalDateFormatter = SceytChatUIKit.formatters.channelInfoAttachmentDateFormatter
        SceytChatUIKit.formatters.channelInfoAttachmentDateFormatter = Formatter { _, _ -> DATE }
        // The holder tints its icon with the theme accent color, the one library color resource it
        // resolves. A framework color keeps that working without android resources in this task.
        originalColors = SceytChatUIKit.theme.colors
        SceytChatUIKit.theme.colors = originalColors.copy(
            accentColor = android.R.color.transparent
        )
        activityController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        binding = SceytItemChannelFileBinding.bind(row(activityController.get()))
        holder = FilesSearchItemViewHolder(
            style = style(),
            binding = binding,
            needMediaDataCallback = downloadRequests::add,
            onAttachmentClickListener = null,
        )
    }

    @After
    fun tearDown() {
        SceytChatUIKit.formatters.channelInfoAttachmentDateFormatter = originalDateFormatter
        SceytChatUIKit.theme.colors = originalColors
        AttachmentTransferStateStore.clear()
        activityController.destroy()
        SceytKoinApp.koinApp = null
        stopKoin()
    }

    @Test
    fun `idle attachment shows the style subtitle`() {
        holder.bind(item())

        assertThat(binding.tvFileSizeAndDate.text.toString()).isEqualTo(IDLE_SUBTITLE)
    }

    @Test
    fun `transferring states show loaded and total size with the date`() {
        val states = listOf(
            TransferState.Downloading,
            TransferState.Uploading,
            TransferState.Preparing,
            TransferState.WaitingToUpload,
        )
        holder.bind(item())

        states.forEach { state ->
            holder.updateState(transfer(state))

            assertThat(binding.tvFileSizeAndDate.text.toString())
                .isEqualTo("$LOADED_SIZE / $TOTAL_SIZE • $DATE")
        }
    }

    @Test
    fun `paused transfer keeps reporting how far it got`() {
        holder.bind(item())

        holder.updateState(transfer(TransferState.Downloading))
        holder.updateState(transfer(TransferState.PauseDownload))

        assertThat(binding.tvFileSizeAndDate.text.toString())
            .isEqualTo("$LOADED_SIZE / $TOTAL_SIZE • $DATE")
    }

    @Test
    fun `failed transfer keeps reporting how far it got`() {
        holder.bind(item())

        holder.updateState(transfer(TransferState.Downloading))
        holder.updateState(transfer(TransferState.ErrorDownload))

        assertThat(binding.tvFileSizeAndDate.text.toString())
            .isEqualTo("$LOADED_SIZE / $TOTAL_SIZE • $DATE")
    }

    @Test
    fun `completed transfer restores the style subtitle`() {
        holder.bind(item())

        holder.updateState(transfer(TransferState.Downloading))
        holder.updateState(transfer(TransferState.Downloaded))

        assertThat(binding.tvFileSizeAndDate.text.toString()).isEqualTo(IDLE_SUBTITLE)
    }

    @Test
    fun `transferring without calculated sizes falls back to the style subtitle`() {
        holder.bind(item())

        holder.updateState(
            transfer(TransferState.Downloading, loadedSize = null, totalSize = null)
        )

        assertThat(binding.tvFileSizeAndDate.text.toString()).isEqualTo(IDLE_SUBTITLE)
    }

    @Test
    fun `row bound while downloading shows progress without waiting for the next update`() {
        AttachmentTransferStateStore.put(transfer(TransferState.Downloading))

        holder.bind(item())

        assertThat(binding.tvFileSizeAndDate.text.toString())
            .isEqualTo("$LOADED_SIZE / $TOTAL_SIZE • $DATE")
    }

    @Test
    fun `recycled row does not inherit the previous progress text`() {
        AttachmentTransferStateStore.put(transfer(TransferState.Downloading))
        holder.bind(item())

        AttachmentTransferStateStore.clear()
        holder.bind(item(messageTid = OTHER_MESSAGE_TID))

        assertThat(binding.tvFileSizeAndDate.text.toString()).isEqualTo(IDLE_SUBTITLE)
    }

    /**
     * The row is assembled in code rather than inflated from [R.layout.sceyt_item_channel_file],
     * so the test needs no android resources in the unit test task. View binding resolves its
     * fields by id, so only the ids and the view types have to match the layout.
     */
    private fun row(context: Context) = ConstraintLayout(context).apply {
        addView(AppCompatImageView(context).apply { id = R.id.icFile })
        addView(CircularProgressView(context).apply { id = R.id.loadProgress })
        addView(AppCompatTextView(context).apply { id = R.id.tvFileName })
        addView(AppCompatTextView(context).apply { id = R.id.tvFileSizeAndDate })
    }

    private fun style() = ChannelInfoFileItemStyle(
        backgroundColor = Color.WHITE,
        fileNameTextStyle = TextStyle(),
        subtitleTextStyle = TextStyle(),
        mediaLoaderStyle = MediaLoaderStyle(),
        fileNameFormatter = { _, attachment -> attachment.name.orEmpty() },
        subtitleFormatter = { _, _ -> IDLE_SUBTITLE },
        iconProvider = { _, _ -> null },
    )

    private fun item(messageTid: Long = MESSAGE_TID) = GlobalSearchListItem.AttachmentItem(
        result = GlobalSearchAttachmentResult(
            attachment = attachment(messageTid),
            message = mock(),
            channel = mock(),
            sender = null,
            kind = GlobalSearchAttachmentKind.File,
        ),
        query = "",
    )

    private fun attachment(messageTid: Long = MESSAGE_TID) = SceytAttachment(
        id = messageTid,
        messageId = messageTid,
        messageTid = messageTid,
        userId = null,
        name = "file.pdf",
        type = AttachmentTypeEnum.File.value,
        metadata = null,
        fileSize = 165_000L,
        createdAt = 1_000L,
        url = "https://cdn.test/file.pdf",
        filePath = null,
        transferState = TransferState.PendingDownload,
        progressPercent = 0f,
        originalFilePath = null,
        linkPreviewDetails = null,
    )

    private fun transfer(
        state: TransferState,
        loadedSize: String? = LOADED_SIZE,
        totalSize: String? = TOTAL_SIZE,
    ) = TransferData(
        messageTid = MESSAGE_TID,
        progressPercent = 8f,
        state = state,
        filePath = null,
        url = "https://cdn.test/file.pdf",
        fileLoadedSize = loadedSize,
        fileTotalSize = totalSize,
    )

    private companion object {
        const val MESSAGE_TID = 10L
        const val OTHER_MESSAGE_TID = 11L
        const val DATE = "Jan 5"
        const val LOADED_SIZE = "14.00KB"
        const val TOTAL_SIZE = "165.00KB"
        const val IDLE_SUBTITLE = "165.00KB • Jan 5"
    }
}
