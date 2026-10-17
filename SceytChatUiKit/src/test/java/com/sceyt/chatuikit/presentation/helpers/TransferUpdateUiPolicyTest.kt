package com.sceyt.chatuikit.presentation.helpers

import android.util.Size
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.persistence.file_transfer.ThumbData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TransferUpdateUiPolicyTest {

    @Test
    fun `running and pending states do not reach message list or adapter updates`() {
        val states = listOf(
            TransferState.Downloading,
            TransferState.Uploading,
            TransferState.Preparing,
            TransferState.WaitingToUpload,
            TransferState.PendingUpload,
            TransferState.PendingDownload
        )

        states.forEach { state ->
            val transfer = transfer(state)

            assertThat(TransferUpdateUiPolicy.shouldApplyDeferredUpdate(transfer, ThumbFor.MessagesLisView)).isFalse()
            assertThat(TransferUpdateUiPolicy.shouldUpdateAdapterItem(transfer)).isFalse()
        }
    }

    @Test
    fun `terminal path error and paused states update message list and adapter item`() {
        val states = listOf(
            TransferState.Downloaded,
            TransferState.Uploaded,
            TransferState.FilePathChanged,
            TransferState.ErrorDownload,
            TransferState.ErrorUpload,
            TransferState.PauseDownload,
            TransferState.PauseUpload
        )

        states.forEach { state ->
            val transfer = transfer(state)

            assertThat(TransferUpdateUiPolicy.shouldApplyDeferredUpdate(transfer, ThumbFor.MessagesLisView)).isTrue()
            assertThat(TransferUpdateUiPolicy.shouldUpdateAdapterItem(transfer)).isTrue()
        }
    }

    @Test
    fun `thumb loaded reaches message list only for message list thumbs`() {
        val messageListThumb = transfer(
            state = TransferState.ThumbLoaded,
            thumbData = ThumbData(ThumbFor.MessagesLisView.value, "/downloads/file.jpg", Size(100, 100))
        )
        val globalSearchThumb = messageListThumb.copy(
            thumbData = ThumbData(ThumbFor.GlobalSearch.value, "/downloads/file.jpg", Size(100, 100))
        )

        assertThat(TransferUpdateUiPolicy.isThumbLoadedFor(messageListThumb, ThumbFor.MessagesLisView)).isTrue()
        assertThat(TransferUpdateUiPolicy.shouldApplyDeferredUpdate(messageListThumb, ThumbFor.MessagesLisView)).isTrue()
        assertThat(TransferUpdateUiPolicy.isThumbLoadedFor(globalSearchThumb, ThumbFor.MessagesLisView)).isFalse()
        assertThat(TransferUpdateUiPolicy.shouldApplyDeferredUpdate(globalSearchThumb, ThumbFor.MessagesLisView)).isFalse()
    }

    @Test
    fun `thumb loaded deferred rule uses provided surface`() {
        val channelInfoThumb = transfer(
            state = TransferState.ThumbLoaded,
            thumbData = ThumbData(ThumbFor.ChannelInfo.value, "/downloads/file.jpg", Size(100, 100))
        )

        assertThat(TransferUpdateUiPolicy.shouldApplyDeferredUpdate(channelInfoThumb, ThumbFor.ChannelInfo)).isTrue()
        assertThat(TransferUpdateUiPolicy.shouldApplyDeferredUpdate(channelInfoThumb, ThumbFor.MessagesLisView)).isFalse()
    }

    private fun transfer(
        state: TransferState,
        thumbData: ThumbData? = null,
    ) = TransferData(
        messageTid = 10L,
        progressPercent = 100f,
        state = state,
        filePath = "/downloads/file.jpg",
        url = "https://cdn.test/file.jpg",
        thumbData = thumbData
    )
}
