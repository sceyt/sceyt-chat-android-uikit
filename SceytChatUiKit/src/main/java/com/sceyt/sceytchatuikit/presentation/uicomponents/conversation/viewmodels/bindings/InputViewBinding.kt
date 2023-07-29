package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.bindings

import android.text.Annotation
import android.text.Editable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.models.message.Message
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.persistence.mappers.isDeleted
import com.sceyt.sceytchatuikit.persistence.mappers.toMessage
import com.sceyt.sceytchatuikit.presentation.common.isPublic
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.MessageInputView
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.Mention
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionUserHelper.getValueData
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionValidatorWatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun MessageListViewModel.bind(messageInputView: MessageInputView,
                              replyInThreadMessage: SceytMessage?,
                              lifecycleOwner: LifecycleOwner) {

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
        SceytKitClient.getChannelsMiddleWare().getChannelFromDb(channel.id)?.let {
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
        messageInputView.editMessage(it.toMessage())
    }

    onReplyMessageCommandLiveData.observe(lifecycleOwner) {
        messageInputView.replyMessage(it.toMessage())
    }

    onChannelMemberAddedOrKickedLiveData.observe(lifecycleOwner) {
        messageInputView.checkIsParticipant(channel)
    }

    onChannelEventFlow.onEach {
        when (it.eventType) {
            ChannelEventEnum.Left -> {
                if (channel.isPublic()) {
                    val leftUser = channel.members?.getOrNull(0)?.id
                    if (leftUser == SceytKitClient.myId)
                        messageInputView.onChannelLeft()
                }
            }

            ChannelEventEnum.Joined -> {
                if (channel.isPublic()) {
                    val leftUser = channel.members?.getOrNull(0)?.id
                    if (leftUser == SceytKitClient.myId)
                        messageInputView.joinSuccess()
                }
            }

            else -> return@onEach
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    var mentionJog: Job? = null

    messageInputView.messageInputActionCallback = object : MessageInputView.MessageInputActionCallback {
        override fun sendMessage(message: Message) {
            this@bind.sendMessage(message)
        }

        override fun sendMessages(message: List<Message>) {
            placeToSavePathsList.clear()
            this@bind.sendMessages(message)
        }

        override fun sendEditMessage(message: SceytMessage) {
            this@bind.editMessage(message)
        }

        override fun typing(typing: Boolean) {
            sendTypingEvent(typing)
        }

        override fun updateDraftMessage(text: Editable?, mentionUserIds: List<Mention>) {
            this@bind.updateDraftMessage(text, mentionUserIds)
        }

        override fun mention(query: String) {
            mentionJog?.cancel()
            if (messageInputView.getComposedMessage().isNullOrBlank()) {
                messageInputView.setMentionList(emptyList())
                return
            }
            mentionJog = viewModelScope.launch(Dispatchers.IO) {
                val result = SceytKitClient.getMembersMiddleWare().loadChannelMembersByDisplayName(channel.id, query)
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
    }
}

@Suppress("unused")
fun bindViewFromJava(viewModel: MessageListViewModel, replyInThreadMessage: SceytMessage?, messagesInputView: MessageInputView, lifecycleOwner: LifecycleOwner) {
    viewModel.bind(messagesInputView, replyInThreadMessage, lifecycleOwner)
}