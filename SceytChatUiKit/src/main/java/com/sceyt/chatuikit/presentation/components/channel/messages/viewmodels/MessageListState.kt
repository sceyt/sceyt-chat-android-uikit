package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem

data class MessageListState(
    val items: List<MessageListItem> = emptyList(),
    val revision: Long = 0,
    val hasLoadedInitialMessages: Boolean = false,
)

sealed interface MessageListRenderEffect {
    data class Replace(
        val items: List<MessageListItem>,
        val force: Boolean,
    ) : MessageListRenderEffect

    data class PrependPage(
        val resultItems: List<MessageListItem>,
    ) : MessageListRenderEffect

    data class AppendPage(
        val resultItems: List<MessageListItem>,
    ) : MessageListRenderEffect

    data class AppendRealtime(
        val items: List<MessageListItem>,
        val scroll: AppendRealtimeScroll,
    ) : MessageListRenderEffect

    data class UpdateItem(
        val index: Int,
        val item: MessageListItem.MessageItem,
        val diff: MessageDiff?,
        val notifyVisibleOnly: Boolean = false,
        val notify: Boolean = true,
    ) : MessageListRenderEffect

    data class DeleteTids(
        val tids: List<Long>,
    ) : MessageListRenderEffect

    data object Clear : MessageListRenderEffect
    data object HideLoadingPrev : MessageListRenderEffect
    data object HideLoadingNext : MessageListRenderEffect

    data class ScrollToMessage(
        val messageId: Long,
        val highlight: Boolean,
        val offset: Int = 0,
        val loadOnMissing: ScrollLoadOnMissing? = null,
    ) : MessageListRenderEffect

    data object ScrollToUnreadMessage : MessageListRenderEffect
    data object ScrollToLastMessage : MessageListRenderEffect

    /**
     * Scroll to the channel's newest message, loading the previous page first if it is not
     * currently in the list. Unlike [ScrollToLastMessage] (which assumes the message is already
     * loaded and just scrolls), this handles the "scroll to bottom" command from the UI.
     */
    data class ScrollToNewMessage(
        val lastMessage: SceytMessage?,
    ) : MessageListRenderEffect

    data class Sort(
        val resultItems: List<MessageListItem>,
    ) : MessageListRenderEffect

    /** Merge messages fetched by a centered window sync into the list, if still applicable. */
    data class ApplyCenteredSync(
        val result: CenteredSyncMessagesResult,
    ) : MessageListRenderEffect
}

data class ScrollLoadOnMissing(
    val loadKey: Long,
    val ignoreServer: Boolean,
)

enum class AppendRealtimeScroll {
    Always,
    IfAtEnd,
}
