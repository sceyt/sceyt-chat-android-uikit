package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.persistence.file_transfer.AttachmentTransferStateStore
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.DateSeparatorItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import org.junit.After
import org.junit.Test

class MessageListItemMapperTest {
    private val mapper = MessageListItemMapper()

    @After
    fun tearDown() {
        AttachmentTransferStateStore.clear()
    }

    @Test
    fun `map adds date separator and loading items`() {
        val first = createMessage(createdAt = 1_000, id = 1, tid = 1)
        val second = createMessage(createdAt = 86_401_000, id = 2, tid = 2)

        val result = mapper.map(
            data = listOf(first, second),
            hasNext = true,
            hasPrev = true,
            enableDateSeparator = true,
            context = context()
        )

        assertThat(result[0]).isEqualTo(MessageListItem.LoadingPrevItem)
        assertThat(result[1]).isInstanceOf(DateSeparatorItem::class.java)
        assertThat(result[2]).isEqualTo(MessageItem(first))
        assertThat(result[3]).isInstanceOf(DateSeparatorItem::class.java)
        assertThat(result[4]).isEqualTo(MessageItem(second))
        assertThat(result[5]).isEqualTo(MessageListItem.LoadingNextItem)
    }

    @Test
    fun `map preserves selected and expanded message state`() {
        val message = createMessage(createdAt = 1_000, id = 1, tid = 10)

        val result = mapper.map(
            data = listOf(message),
            hasNext = false,
            hasPrev = false,
            enableDateSeparator = false,
            context = context(selected = setOf(10), expanded = setOf(10))
        )

        val item = result.single() as MessageItem
        assertThat(item.message.isSelected).isTrue()
        assertThat(item.message.isBodyExpanded).isTrue()
    }

    @Test
    fun `map inserts unread separator after pinned last read message`() {
        val pinnedMessage = createMessage(createdAt = 1_000, id = 1, tid = 1)
        val nextMessage = createMessage(createdAt = 2_000, id = 2, tid = 2)
        val channel = createChannel(id = 1, pinnedAt = 0, createdAt = 1)
            .copy(lastMessage = createMessage(createdAt = 3_000, id = 3, tid = 3).copy(incoming = true))

        val result = mapper.map(
            data = listOf(nextMessage),
            hasNext = false,
            hasPrev = false,
            compareMessage = pinnedMessage,
            enableDateSeparator = false,
            context = context(channel = channel, pinnedLastReadMessageId = pinnedMessage.id)
        )

        assertThat(result).hasSize(2)
        assertThat(result[0]).isEqualTo(
            MessageListItem.UnreadMessagesSeparatorItem(
                createdAt = nextMessage.createdAt,
                msgId = pinnedMessage.id
            )
        )
        assertThat(result[1]).isEqualTo(MessageItem(nextMessage))
    }

    @Test
    fun `init message info sets group avatar state`() {
        val channel = createChannel(id = 1, pinnedAt = 0, createdAt = 1).copy(type = "group")
        val message = createMessage(createdAt = 1_000, id = 1, tid = 1).copy(incoming = true)

        val result = mapper.initMessageInfoData(
            sceytMessage = message,
            initNameAndAvatar = true,
            context = context(channel = channel)
        )

        assertThat(result.isGroup).isTrue()
        assertThat(result.shouldShowAvatarAndName).isTrue()
        assertThat(result.disabledShowAvatarAndName).isFalse()
    }

    @Test
    fun `init message info disables avatar when setting is off`() {
        val channel = createChannel(id = 1, pinnedAt = 0, createdAt = 1).copy(type = "group")
        val message = createMessage(createdAt = 1_000, id = 1, tid = 1).copy(incoming = true)

        val result = mapper.initMessageInfoData(
            sceytMessage = message,
            initNameAndAvatar = true,
            context = context(channel = channel, showSenderAvatarAndName = false)
        )

        assertThat(result.shouldShowAvatarAndName).isFalse()
        assertThat(result.disabledShowAvatarAndName).isTrue()
    }

    @Test
    fun `init message info overlays latest transfer data from store`() {
        val attachment = attachment(
            messageTid = 10,
            url = "https://cdn.test/file.jpg",
            state = TransferState.PendingDownload,
            progress = 0f
        )
        AttachmentTransferStateStore.put(
            TransferData(
                messageTid = attachment.messageTid,
                progressPercent = 65f,
                state = TransferState.Downloading,
                filePath = null,
                url = attachment.url
            )
        )
        val message = createMessage(createdAt = 1_000, id = 1, tid = attachment.messageTid)
            .copy(attachments = listOf(attachment))

        val result = mapper.initMessageInfoData(
            sceytMessage = message,
            context = context()
        )

        val file = result.files?.single()
        assertThat(file?.transferData?.state).isEqualTo(TransferState.Downloading)
        assertThat(file?.transferData?.progressPercent).isEqualTo(65f)
        assertThat(file?.attachment?.transferState).isEqualTo(TransferState.Downloading)
    }

    private fun context(
        channel: SceytChannel = createChannel(id = 1, pinnedAt = 0, createdAt = 1),
        myId: String? = null,
        pinnedLastReadMessageId: Long = 0,
        showSenderAvatarAndName: Boolean = true,
        selected: Set<Long> = emptySet(),
        expanded: Set<Long> = emptySet(),
    ) = MessageListItemMappingContext(
        channel = channel,
        myIdProvider = { myId },
        pinnedLastReadMessageId = pinnedLastReadMessageId,
        showSenderAvatarAndName = showSenderAvatarAndName,
        selectedMessageTids = selected,
        expandedMessageTids = expanded,
    )

    private fun attachment(
        messageTid: Long,
        filePath: String? = null,
        url: String? = null,
        state: TransferState? = null,
        progress: Float? = null,
    ) = SceytAttachment(
        id = messageTid,
        messageId = messageTid,
        messageTid = messageTid,
        userId = null,
        name = "file-$messageTid",
        type = AttachmentTypeEnum.Image.value,
        metadata = null,
        fileSize = 100L,
        createdAt = 1_000L,
        url = url,
        filePath = filePath,
        transferState = state,
        progressPercent = progress,
        originalFilePath = null,
        linkPreviewDetails = null,
    )
}
