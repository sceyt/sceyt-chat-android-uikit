package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.navigation.Destination
import com.sceyt.chatuikit.navigation.navigate
import com.sceyt.chatuikit.navigation.navigateForResult
import com.sceyt.chatuikit.presentation.components.channel.messages.components.PinnedMessagesView
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle
import kotlinx.coroutines.launch

/**
 * @param messagesListStyle the conversation's own list style, passed on to the pinned-messages
 * screen so its cells are styled identically; without it that screen falls back to defaults.
 * @param pinnedMessagesListLauncher receives the picked message id from the pinned-messages
 * screen. Result parsing stays with the caller that owns the launcher, per
 * [navigateForResult]; without one the list opens read-only and picking a row does nothing.
 */
fun MessageListViewModel.bind(
    pinnedMessagesView: PinnedMessagesView,
    lifecycleOwner: LifecycleOwner,
    pinnedMessagesListLauncher: ActivityResultLauncher<Intent>? = null,
    messagesListStyle: MessagesListViewStyle? = null,
) {
    pinnedMessagesView.onAction = { action ->
        when (action) {
            is PinnedMessagesView.Action.Jump ->
                prepareToScrollToPinnedMessage(action.pinnedMessage.messageId)

            is PinnedMessagesView.Action.ShowList -> {
                val destination = Destination.PinnedMessages(channel, messagesListStyle)
                if (pinnedMessagesListLauncher != null)
                    SceytChatUIKit.navigator.navigateForResult(
                        context = pinnedMessagesView.context,
                        launcher = pinnedMessagesListLauncher,
                        destination = destination
                    )
                else SceytChatUIKit.navigator.navigate(pinnedMessagesView.context, destination)
            }
        }
    }

    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            pinnedMessages.collect { pinnedMessagesView.setPinnedMessages(it) }
        }
    }
}