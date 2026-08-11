package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files

import androidx.recyclerview.widget.RecyclerView
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.FilesViewHolderFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MessageFilesAdapterTest {

    @Test
    fun `update preserves thumbs by attachment and keeps newer incoming thumb`() {
        val first = file(id = 1, thumbPath = "/thumbs/first.jpg")
        val second = file(id = 2, thumbPath = "/thumbs/second.jpg")
        val adapter = adapter(listOf(first, second))

        adapter.notifyUpdate(listOf(
            file(id = 2, thumbPath = null),
            file(id = 1, thumbPath = "/thumbs/new-first.jpg"),
        ))

        assertThat(adapter.getData().map { it.thumbPath }).containsExactly(
            "/thumbs/second.jpg",
            "/thumbs/new-first.jpg",
        ).inOrder()
    }

    @Test
    fun `pending attachment keeps thumb when server id arrives`() {
        val url = "https://cdn.test/pending-file.jpg"
        val adapter = adapter(listOf(
            file(id = null, url = url, thumbPath = "/thumbs/pending.jpg")
        ))

        adapter.notifyUpdate(listOf(file(id = 10, url = url)))

        assertThat(adapter.getData().single().thumbPath).isEqualTo("/thumbs/pending.jpg")
    }

    @Test
    fun `update exposes new backing list before dispatching notifications`() {
        val adapter = adapter(listOf(file(id = 1)))
        var itemCountDuringNotification: Int? = null
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                itemCountDuringNotification = adapter.itemCount
            }
        })

        adapter.notifyUpdate(listOf(file(id = 1), file(id = 2)))

        assertThat(itemCountDuringNotification).isEqualTo(2)
    }

    @Test
    fun `constructor detaches mutable file input`() {
        val input = file(id = 1, thumbPath = "/thumbs/input.jpg")
        val adapter = adapter(listOf(input))
        val stored = adapter.getData().single()

        assertThat(stored).isNotSameInstanceAs(input)
        assertThat(stored.transferData).isNotSameInstanceAs(input.transferData)

        stored.updateThumbPath("/thumbs/adapter.jpg")
        stored.transferData?.state = TransferState.ErrorDownload

        assertThat(input.thumbPath).isEqualTo("/thumbs/input.jpg")
        assertThat(input.transferData?.state).isEqualTo(TransferState.PendingDownload)
    }

    @Test
    fun `update detaches mutable file input`() {
        val adapter = adapter(listOf(file(id = 1)))
        val input = file(id = 1, thumbPath = "/thumbs/input.jpg")

        adapter.notifyUpdate(listOf(input))
        val stored = adapter.getData().single()

        assertThat(stored).isNotSameInstanceAs(input)
        assertThat(stored.transferData).isNotSameInstanceAs(input.transferData)

        stored.updateThumbPath("/thumbs/adapter.jpg")
        stored.transferData?.state = TransferState.ErrorDownload

        assertThat(input.thumbPath).isEqualTo("/thumbs/input.jpg")
        assertThat(input.transferData?.state).isEqualTo(TransferState.PendingDownload)
    }

    private fun adapter(files: List<FileListItem>) = MessageFilesAdapter(
        message = createMessage(createdAt = 1, id = 1, tid = 1),
        files = files,
        viewHolderFactory = mock<FilesViewHolderFactory>(),
    )

    private fun file(
        id: Long?,
        url: String = "https://cdn.test/file-$id.jpg",
        thumbPath: String? = null,
    ): FileListItem {
        val attachment = SceytAttachment(
            id = id,
            messageId = 1,
            messageTid = 1,
            userId = null,
            name = "file-$id.jpg",
            type = AttachmentTypeEnum.Image.value,
            metadata = null,
            fileSize = 1_000,
            createdAt = 1_000,
            url = url,
            filePath = null,
            transferState = TransferState.PendingDownload,
            progressPercent = 0f,
            originalFilePath = null,
            linkPreviewDetails = null,
        )
        return FileListItem(
            _attachment = attachment,
            _metadataPayload = AttachmentMetadataPayload(),
            _thumbPath = thumbPath,
            _transferData = TransferData(
                messageTid = attachment.messageTid,
                progressPercent = 0f,
                state = TransferState.PendingDownload,
                filePath = null,
                url = attachment.url,
            ),
            type = AttachmentTypeEnum.Image,
        )
    }
}
