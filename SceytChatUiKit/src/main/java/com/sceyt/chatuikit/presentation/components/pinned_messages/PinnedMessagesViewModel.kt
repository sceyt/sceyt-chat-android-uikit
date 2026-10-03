package com.sceyt.chatuikit.presentation.components.pinned_messages

import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.message.DeleteMessageType
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.domain.usecases.PauseOrResumeTransferUseCase
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.interactor.MessagePinInteractor
import com.sceyt.chatuikit.persistence.interactor.MessagePollInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageReactionInteractor
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.ReactionItem
import com.sceyt.chatuikit.presentation.components.channel.messages.events.PollEvent
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListItemMapper
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListItemMappingContext
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.PinController
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.PollController
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.ReactionController
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

class PinnedMessagesViewModel(
    private val channel: SceytChannel,
) : BaseViewModel(), SceytKoinComponent {

    private val pinInteractor: MessagePinInteractor by inject()
    private val messageInteractor: MessageInteractor by inject()
    private val pollInteractor: MessagePollInteractor by inject()
    private val reactionInteractor: MessageReactionInteractor by inject()
    private val fileTransferService: FileTransferService by inject()
    private val pauseOrResumeTransferUseCase: PauseOrResumeTransferUseCase by inject()
    private val userInteractor: UserInteractor by inject()
    private val channelInteractor: ChannelInteractor by inject()
    private val itemMapper = MessageListItemMapper()

    private val reactionController = ReactionController(
        scope = viewModelScope,
        reactionInteractor = reactionInteractor,
        channelId = { channel.id },
        notifyResponse = { response, showError ->
            notifyPageStateWithResponse(response, showError = showError)
        }
    )

    private val pollController = PollController(
        scope = viewModelScope,
        pollInteractor = pollInteractor,
        channelId = { channel.id },
        notifyResponse = { response, showError ->
            notifyPageStateWithResponse(response, showError = showError)
        }
    )

    private val pinController = PinController(
        scope = viewModelScope,
        pinInteractor = pinInteractor,
        channelId = { channel.id },
        notifyResponse = { response, showError ->
            notifyPageStateWithResponse(response, showError = showError)
        }
    )

    private val expandedMessageTids = mutableSetOf<Long>()
    val selectedMessages = mutableMapOf<Long, SceytMessage>()

    // Pins use pin order, so omit timeline separators.
    val pinnedMessages: SharedFlow<List<MessageListItem>> =
        pinInteractor.getPinnedMessagesFlow(channel.id)
            .map { pins ->
                itemMapper.map(
                    data = pins.map { it.message },
                    hasNext = false,
                    hasPrev = false,
                    ignoreUnreadMessagesSeparator = true,
                    enableDateSeparator = false,
                    context = MessageListItemMappingContext(
                        channel = channel,
                        myIdProvider = { SceytChatUIKit.currentUserId },
                        pinnedLastReadMessageId = 0L,
                        showSenderAvatarAndName = channel.isGroup,
                        selectedMessageTids = selectedMessages.keys,
                        expandedMessageTids = expandedMessageTids,
                    )
                )
            }
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    fun expandBody(messageTid: Long) {
        expandedMessageTids += messageTid
    }

    /** @return the message with its new selection state, or null if the cap was reached. */
    fun toggleSelection(message: SceytMessage): SceytMessage? {
        val wasSelected = selectedMessages.containsKey(message.tid)
        if (!wasSelected && selectedMessages.size >= SceytChatUIKit.config.messageMultiselectLimit)
            return null

        val updated = message.copy(isSelected = !wasSelected)
        if (wasSelected) selectedMessages.remove(message.tid)
        else selectedMessages[message.tid] = updated
        return updated
    }

    fun unpin(messageTid: Long) {
        pinController.unpin(messageTid)
    }

    fun delete(messages: List<SceytMessage>, deleteType: DeleteMessageType) {
        viewModelScope.launch(Dispatchers.IO) {
            messages.forEach { message ->
                val response = messageInteractor.deleteMessage(channel.id, message, deleteType)
                notifyPageStateWithResponse(response)
            }
        }
    }

    fun retractVote(message: SceytMessage) {
        pollController.onEvent(PollEvent.RetractVote(message))
    }

    fun endVote(message: SceytMessage) {
        pollController.onEvent(PollEvent.EndVote(message))
    }

    fun toggleVote(message: SceytMessage, option: PollOption) {
        pollController.onEvent(PollEvent.ToggleVote(message, option))
    }

    fun addOrRemoveReaction(message: SceytMessage, reaction: ReactionItem.Reaction) {
        if (reaction.reaction.containsSelf)
            reactionController.delete(message, reaction.reaction.key)
        else reactionController.add(message, reaction.reaction.key)
    }

    fun needMediaInfo(data: NeedMediaInfoData) {
        val attachment = data.item
        viewModelScope.launch(Dispatchers.IO) {
            when (data) {
                is NeedMediaInfoData.NeedDownload -> fileTransferService.download(
                    attachment = attachment,
                    transferTask = fileTransferService.findOrCreateTransferTask(attachment)
                )

                is NeedMediaInfoData.NeedThumb -> fileTransferService.getThumb(
                    attachment.messageTid, attachment, data.thumbData
                )
            }
        }
    }

    fun openUser(userId: String, onChannel: (SceytChannel) -> Unit) {
        if (userId.isBlank() || userId == SceytChatUIKit.currentUserId) return
        viewModelScope.launch(Dispatchers.IO) {
            val user = userInteractor.getUserFromDbById(userId) ?: SceytUser(userId)
            channelInteractor.findOrCreatePendingChannelByMembers(
                data = CreateChannelData(
                    type = ChannelTypeEnum.Direct.value,
                    members = listOf(SceytMember(roleName = RoleTypeEnum.Owner.value, user = user)),
                )
            ).onSuccessNotNull { channel ->
                withContext(Dispatchers.Main) { onChannel(channel) }
            }
        }
    }

    fun pauseOrResumeUpload(item: FileListItem) {
        viewModelScope.launch {
            pauseOrResumeTransferUseCase(item.attachment, channel.id)
        }
    }
}