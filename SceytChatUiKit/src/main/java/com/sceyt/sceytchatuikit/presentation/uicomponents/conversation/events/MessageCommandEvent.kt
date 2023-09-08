package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events

import android.view.View
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.ScrollToDownView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem

sealed class MessageCommandEvent {

    data class DeleteMessage(
            val message: List<SceytMessage>,
            val onlyForMe: Boolean
    ) : MessageCommandEvent()

    data class EditMessage(
            val message: SceytMessage,
    ) : MessageCommandEvent()

    data class ShowHideMessageActions(
            val message: SceytMessage,
            val show: Boolean
    ) : MessageCommandEvent()

    data class OnMultiselectEvent(
            val message: SceytMessage,
    ) : MessageCommandEvent()

    object OnCancelMultiselectEvent : MessageCommandEvent()

    data class Reply(
            val message: SceytMessage,
    ) : MessageCommandEvent()

    data class ScrollToDown(
            val view: ScrollToDownView
    ) : MessageCommandEvent()

    data class ScrollToReplyMessage(
            val message: SceytMessage
    ) : MessageCommandEvent()

    data class AttachmentLoaderClick(
            val item: FileListItem
    ) : MessageCommandEvent()

    data class UserClick(
            val view: View,
            val userId: String
    ) : MessageCommandEvent()
}