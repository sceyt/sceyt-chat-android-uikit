package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings

import android.text.Annotation
import android.text.Editable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.message.Message
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.persistence.extensions.getChannelType
import com.sceyt.chatuikit.persistence.extensions.isPublic
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.mappers.isDeleted
import com.sceyt.chatuikit.presentation.common.SceytDialog
import com.sceyt.chatuikit.presentation.components.channel.input.MessageInputView
import com.sceyt.chatuikit.presentation.components.channel.input.data.InputUserActivity
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyleRange
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.MessageInputActionCallback
import com.sceyt.chatuikit.presentation.components.channel.input.mention.Mention
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MentionUserHelper.getValueData
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MentionValidatorWatcher
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.presentation.root.PageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@JvmName("bind")
fun MessageListViewModel.bind(
        messageInputView: MessageInputView,
        replyInThreadMessage: SceytMessage?,
        lifecycleOwner: LifecycleOwner
) {

    messageActionBridge.setInputView(messageInputView)

    if (placeToSavePathsList.isNotEmpty())
        messageInputView.addAttachment(*placeToSavePathsList.toTypedArray())

    var loadedMembers = listOf<SceytMember>()

    messageInputView.setReplyInThreadMessageId(replyInThreadMessage?.id)
    messageInputView.checkIsParticipant(channel)
    messageInputView.setSaveUrlsPlace(placeToSavePathsList)
    messageInputView.setMentionValidator(object : MentionValidatorWatcher.MentionValidator {
        override fun getInvalidMentionAnnotations(mentionAnnotations: List<Annotation>?): List<Annotation>? {
            return runBlocking {
                val ids = mentionAnnotations?.mapNotNull { it.getValueData()?.userId }
                        ?: return@runBlocking null

                val existUsersIds = if (loadedMembers.isEmpty())
                    channelMemberInteractor.filterOnlyMembersByIds(channel.id, ids)
                else loadedMembers.map { it.id }

                return@runBlocking mentionAnnotations.filter { annotation ->
                    existUsersIds.none { it == annotation.getValueData()?.userId }
                }
            }
        }
    })

    viewModelScope.launch(Dispatchers.IO) {
        channelInteractor.getChannelFromDb(channel.id)?.let {
            withContext(Dispatchers.Main) {
                messageInputView.setDraftMessage(it.draftMessage)
            }
        }
    }

    loadChannelMembersIfNeeded()

    onChannelUpdatedEventFlow.onEach {
        messageInputView.checkIsParticipant(it)
    }.launchIn(lifecycleOwner.lifecycleScope)

    searchResult.observe(lifecycleOwner) {
        messageInputView.onSearchMessagesResult(it)
    }

    pageStateLiveData.observe(lifecycleOwner) {
        if (it is PageState.StateError && it.showMessage)
            customToastSnackBar(messageInputView, it.errorMessage.toString())
    }

    channelLiveData.observe(lifecycleOwner) {
        if (it is SceytResponse.Success) {
            messageInputView.checkIsParticipant(channel)
        }
    }

    joinLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> messageInputView.joinSuccess()
            is SceytResponse.Error -> customToastSnackBar(messageInputView, it.message.toString())
        }
    }

    onEditMessageCommandLiveData.observe(lifecycleOwner) {
        messageInputView.editMessage(it, false)
    }

    onReplyMessageCommandLiveData.observe(lifecycleOwner) {
        messageInputView.replyMessage(it, false)
    }

    onChannelMemberAddedOrKickedLiveData.observe(lifecycleOwner) {
        messageInputView.checkIsParticipant(channel)
    }

    onChannelEventFlow.onEach { event ->
        when (event) {
            is ChannelActionEvent.Left -> {
                if (channel.isPublic()) {
                    event.leftMembers.forEach { member ->
                        if (member.id == SceytChatUIKit.chatUIFacade.myId)
                            messageInputView.onChannelLeft()
                    }
                }
            }

            is ChannelActionEvent.Joined -> {
                if (channel.isPublic()) {
                    event.joinedMembers.forEach { member ->
                        if (member.id == SceytChatUIKit.chatUIFacade.myId)
                            messageInputView.joinSuccess()
                    }
                }
            }

            else -> return@onEach
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    ChannelsCache.channelUpdatedFlow
        .filter { it.channel.id == channel.id }
        .onEach {
            val channel = it.channel
            val wasJoined = channel.userRole.isNotNullOrBlank()
            if (channel.userRole.isNotNullOrBlank()) {
                if (!wasJoined)
                    messageInputView.joinSuccess()
            } else messageInputView.onChannelLeft()
        }
        .launchIn(lifecycleOwner.lifecycleScope)

    var mentionJob: Job? = null

    fun upsertLinkPreviewData(linkDetails: LinkPreviewDetails?) {
        if (linkDetails != null) {
            viewModelScope.launch {
                attachmentInteractor.upsertLinkPreviewData(linkDetails)
            }
        }
    }

    messageInputView.setInputActionsCallback(object : MessageInputActionCallback {
        override fun sendMessage(message: Message, linkDetails: LinkPreviewDetails?) {
            this@bind.sendMessage(message)
            upsertLinkPreviewData(linkDetails)
        }

        override fun sendMessages(message: List<Message>, linkDetails: LinkPreviewDetails?) {
            placeToSavePathsList.clear()
            this@bind.sendMessages(message)
            upsertLinkPreviewData(linkDetails)
        }

        override fun sendEditMessage(message: SceytMessage, linkDetails: LinkPreviewDetails?) {
            this@bind.editMessage(message)
            upsertLinkPreviewData(linkDetails)
        }

        override fun sendUserActivity(state: InputUserActivity) {
            sendUserActivityStateEvent(state)
        }

        override fun updateDraftMessage(
                text: Editable?,
                mentionUserIds: List<Mention>,
                styling: List<BodyStyleRange>?,
                replyOrEditMessage: SceytMessage?,
                isReply: Boolean
        ) {
            this@bind.updateDraftMessage(text, mentionUserIds, styling, replyOrEditMessage, isReply)
        }

        override fun mention(query: String) {
            mentionJob?.cancel()
            if (messageInputView.getComposedMessage().isNullOrBlank()) {
                messageInputView.setMentionList(emptyList())
                return
            }
            mentionJob = viewModelScope.launch(Dispatchers.IO) {
                val result = channelMemberInteractor.loadChannelMembersByDisplayName(channel.id, query)
                if (query.isEmpty())
                    loadedMembers = result

                withContext(Dispatchers.Main) {
                    messageInputView.setMentionList(result.filter { it.id != SceytChatUIKit.chatUIFacade.myId && !it.user.isDeleted() })
                }
            }
        }

        override fun join() {
            this@bind.join()
        }

        override fun clearChat() {
            val descId: Int = when (channel.getChannelType()) {
                ChannelTypeEnum.Direct -> R.string.sceyt_clear_direct_history_desc
                ChannelTypeEnum.Group -> R.string.sceyt_clear_private_chat_history_desc
                ChannelTypeEnum.Public -> R.string.sceyt_clear_public_chat_history_desc
            }
            SceytDialog.showDialog(messageInputView.context, R.string.sceyt_clear_history_title, descId, R.string.sceyt_clear, positiveCb = {
                clearHistory(channel.isPublic())
                messageActionBridge.cancelMultiSelectMode()
                selectedMessagesMap.clear()
            })
        }

        override fun scrollToNext() {
            scrollToSearchMessage(false)
        }

        override fun scrollToPrev() {
            scrollToSearchMessage(true)
        }
    })
}