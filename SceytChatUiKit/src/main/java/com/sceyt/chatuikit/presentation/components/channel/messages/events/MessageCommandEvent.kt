package com.sceyt.chatuikit.presentation.components.channel.messages.events

import android.view.View
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.messages.components.ScrollToDownView
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem

sealed interface MessageCommandEvent {

    data class DeleteMessage(
            val messages: List<SceytMessage>,
            val onlyForMe: Boolean
    ) : MessageCommandEvent

    data class EditMessage(
            val message: SceytMessage,
    ) : MessageCommandEvent

    data class ShowHideMessageActions(
            val message: SceytMessage,
            val show: Boolean
    ) : MessageCommandEvent

    data class SearchMessages(
            val show: Boolean
    ) : MessageCommandEvent

    data class OnMultiselectEvent(
            val message: SceytMessage,
    ) : MessageCommandEvent

    data object OnCancelMultiselectEvent : MessageCommandEvent

    data class Reply(
            val message: SceytMessage,
    ) : MessageCommandEvent

    data class ReplyInThread(
            val message: SceytMessage,
    ) : MessageCommandEvent

    data class ScrollToDown(
            val view: ScrollToDownView
    ) : MessageCommandEvent

    data class ScrollToReplyMessage(
            val message: SceytMessage
    ) : MessageCommandEvent

    data class AttachmentLoaderClick(
            val message: SceytMessage,
            val item: FileListItem
    ) : MessageCommandEvent

    data class UserClick(
            val view: View,
            val userId: String
    ) : MessageCommandEvent
}