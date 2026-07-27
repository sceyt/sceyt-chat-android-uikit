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
class DeferredTransferUpdateBufferTest {

    @Test
    fun `drain keeps transfer and thumb updates for same message`() {
        val buffer = DeferredTransferUpdateBuffer(ThumbFor.MessagesLisView)
        val transfer = transfer(TransferState.Downloaded, filePath = "/downloads/file.jpg")
        val thumb = transfer(
            state = TransferState.ThumbLoaded,
            filePath = "/thumbs/file.jpg",
            thumbData = ThumbData(
                key = ThumbFor.MessagesLisView.value,
                filePath = "/downloads/file.jpg",
                size = Size(100, 100)
            )
        )

        buffer.add(transfer)
        buffer.add(thumb)

        val result = buffer.drain()
        assertThat(result).containsExactly(transfer, thumb).inOrder()
        assertThat(buffer.isNotEmpty()).isFalse()
    }

    @Test
    fun `drain keeps latest transfer and latest thumb update`() {
        val buffer = DeferredTransferUpdateBuffer(ThumbFor.MessagesLisView)
        val error = transfer(TransferState.ErrorDownload, progress = 40f)
        val downloaded = transfer(TransferState.Downloaded, progress = 100f, filePath = "/downloads/file.jpg")
        val oldThumb = transfer(
            state = TransferState.ThumbLoaded,
            filePath = "/thumbs/old.jpg",
            thumbData = ThumbData(ThumbFor.MessagesLisView.value, "/downloads/file.jpg", Size(80, 80))
        )
        val latestThumb = oldThumb.copy(filePath = "/thumbs/latest.jpg")

        buffer.add(error)
        buffer.add(oldThumb)
        buffer.add(downloaded)
        buffer.add(latestThumb)

        assertThat(buffer.drain()).containsExactly(downloaded, latestThumb).inOrder()
    }

    @Test
    fun `add ignores thumb updates for another surface`() {
        val buffer = DeferredTransferUpdateBuffer(ThumbFor.ChannelInfo)
        val messageListThumb = transfer(
            state = TransferState.ThumbLoaded,
            filePath = "/thumbs/message-list.jpg",
            thumbData = ThumbData(ThumbFor.MessagesLisView.value, "/downloads/file.jpg", Size(80, 80))
        )
        val channelInfoThumb = messageListThumb.copy(
            filePath = "/thumbs/channel-info.jpg",
            thumbData = ThumbData(ThumbFor.ChannelInfo.value, "/downloads/file.jpg", Size(80, 80))
        )

        buffer.add(messageListThumb)
        buffer.add(channelInfoThumb)

        assertThat(buffer.drain()).containsExactly(channelInfoThumb)
    }

    private fun transfer(
        state: TransferState,
        progress: Float = 100f,
        filePath: String? = null,
        thumbData: ThumbData? = null,
    ) = TransferData(
        messageTid = MESSAGE_TID,
        progressPercent = progress,
        state = state,
        filePath = filePath,
        url = "https://cdn.test/file.jpg",
        thumbData = thumbData
    )

    private companion object {
        const val MESSAGE_TID = 10L
    }
}
