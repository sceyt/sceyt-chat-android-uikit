package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.bindings

import android.text.Annotation
import android.text.Editable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.message.Message
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.persistence.mappers.isDeleted
import com.sceyt.sceytchatuikit.presentation.common.SceytDialog
import com.sceyt.sceytchatuikit.presentation.common.getChannelType
import com.sceyt.sceytchatuikit.presentation.common.isPublic
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.MessageInputView
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.Mention
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionUserHelper.getValueData
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionValidatorWatcher
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.style.BodyStyleRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun MessageListViewModel.bind(messageInputView: MessageInputView,
                              replyInThreadMessage: SceytMessage?,
                              lifecycleOwner: LifecycleOwner) {

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
                    persistenceMembersMiddleWare.filterOnlyMembersByIds(channel.id, ids)
                else loadedMembers.map { it.id }

                return@runBlocking mentionAnnotations.filter { annotation ->
                    existUsersIds.none { it == annotation.getValueData()?.userId }
                }
            }
        }
    })

    viewModelScope.launch(Dispatchers.IO) {
        persistenceChanelMiddleWare.getChannelFromDb(channel.id)?.let {
            withContext(Dispatchers.Main) { messageInputView.setDraftMessage(it.draftMessage) }
        }
    }

    loadChannelMembersIfNeeded()

    onChannelUpdatedEventFlow.onEach {
        messageInputView.checkIsParticipant(it)
    }.launchIn(lifecycleOwner.lifecycleScope)

    pageStateLiveData.observe(lifecycleOwner) {
        if (it is PageState.StateError && it.showMessage)
            customToastSnackBar(messageInputView, it.errorMessage.toString())
    }

    channelLiveData.observe(lifecycleOwner) {
        if (it is SceytResponse.Success) {
            channel = it.data ?: return@observe
            messageInputView.checkIsParticipant(channel)
        }
    }

    joinLiveData.observe(lifecycleOwner) {
        when (it) {
            is SceytResponse.Success -> {
                messageInputView.joinSuccess()
                channel.members = it.data?.members
            }

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

    onChannelEventFlow.onEach {
        when (val event = it.eventType) {
            is ChannelEventEnum.Left -> {
                if (channel.isPublic()) {
                    event.leftMembers.forEach { member ->
                        if (member.id == SceytKitClient.myId)
                            messageInputView.onChannelLeft()
                    }
                }
            }

            is ChannelEventEnum.Joined -> {
                if (channel.isPublic()) {
                    event.joinedMembers.forEach { member ->
                        if (member.id == SceytKitClient.myId)
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
            channel = it.channel.clone()
            if (channel.userRole.isNotNullOrBlank())
                messageInputView.joinSuccess()
            else messageInputView.onChannelLeft()
        }
        .launchIn(lifecycleOwner.lifecycleScope)

    var mentionJob: Job? = null

    fun upsertLinkPreviewData(linkDetails: LinkPreviewDetails?) {
        if (linkDetails != null) {
            viewModelScope.launch {
                persistenceAttachmentMiddleWare.upsertLinkPreviewData(linkDetails)
            }
        }
    }

    messageInputView.setInputActionCallback(object : MessageInputView.MessageInputActionCallback {
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

        override fun typing(typing: Boolean) {
            sendTypingEvent(typing)
        }

        override fun updateDraftMessage(text: Editable?, mentionUserIds: List<Mention>, styling: List<BodyStyleRange>?, replyOrEditMessage: SceytMessage?, isReply: Boolean) {
            this@bind.updateDraftMessage(text, mentionUserIds, styling, replyOrEditMessage, isReply)
        }

        override fun mention(query: String) {
            mentionJob?.cancel()
            if (messageInputView.getComposedMessage().isNullOrBlank()) {
                messageInputView.setMentionList(emptyList())
                return
            }
            mentionJob = viewModelScope.launch(Dispatchers.IO) {
                val result = persistenceMembersMiddleWare.loadChannelMembersByDisplayName(channel.id, query)
                if (query.isEmpty())
                    loadedMembers = result

                withContext(Dispatchers.Main) {
                    messageInputView.setMentionList(result.filter { it.id != SceytKitClient.myId && !it.user.isDeleted() })
                }
            }
        }

        override fun join() {
            this@bind.join()
        }

        override fun clearChat() {
            val descId: Int = when (channel.getChannelType()) {
                ChannelTypeEnum.Direct -> R.string.sceyt_clear_direct_history_desc
                ChannelTypeEnum.Private, ChannelTypeEnum.Group -> R.string.sceyt_clear_private_chat_history_desc
                ChannelTypeEnum.Public, ChannelTypeEnum.Broadcast -> R.string.sceyt_clear_public_chat_history_desc
            }
            SceytDialog.showSceytDialog(messageInputView.context, R.string.sceyt_clear_history_title, descId, R.string.sceyt_clear, positiveCb = {
                clearHistory(channel.isPublic())
                messageActionBridge.cancelMultiSelectMode()
                selectedMessagesMap.clear()
            })
        }
    })
}

@Suppress("unused")
fun bindViewFromJava(viewModel: MessageListViewModel, replyInThreadMessage: SceytMessage?, messagesInputView: MessageInputView, lifecycleOwner: LifecycleOwner) {
    viewModel.bind(messagesInputView, replyInThreadMessage, lifecycleOwner)
}