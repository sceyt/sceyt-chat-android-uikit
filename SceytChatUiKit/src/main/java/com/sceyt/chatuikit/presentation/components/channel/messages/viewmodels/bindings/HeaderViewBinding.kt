package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.managers.connection.event.ConnectionStateData
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageActionBridge
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@JvmName("bind")
fun MessageListViewModel.bind(
    headerView: MessagesListHeaderView,
    replyInThreadMessage: SceytMessage?,
    lifecycleOwner: LifecycleOwner
) {

    fun showMessageActions(messages: List<SceytMessage>) {
        headerView.uiElementsListeners.onShowMessageActionsMenu(
            messages = messages.toTypedArray(),
            menuStyle = headerView.style.messageActionsMenuStyle
        ) { menuItem, actionFinish ->
            val firstMessage = messages.firstOrNull()
            when (menuItem.itemId) {
                R.id.sceyt_edit_message -> firstMessage?.let {
                    actionFinish()
                    messageActionBridge.dispatchMenuEvent(MessageActionBridge.MenuEvent.Edit(it))
                }

                R.id.sceyt_message_info -> firstMessage?.let {
                    actionFinish()
                    messageActionBridge.dispatchMenuEvent(
                        MessageActionBridge.MenuEvent.MessageInfo(
                            it
                        )
                    )
                }

                R.id.sceyt_forward -> {
                    actionFinish()
                    messageActionBridge.dispatchMenuEvent(
                        MessageActionBridge.MenuEvent.Forward(
                            messages
                        )
                    )
                }

                R.id.sceyt_reply -> firstMessage?.let {
                    actionFinish()
                    messageActionBridge.dispatchMenuEvent(MessageActionBridge.MenuEvent.Reply(it))
                }

                R.id.sceyt_reply_in_thread -> firstMessage?.let {
                    actionFinish()
                    messageActionBridge.dispatchMenuEvent(
                        MessageActionBridge.MenuEvent.ReplyInThread(
                            it
                        )
                    )
                }

                R.id.sceyt_copy_message -> {
                    actionFinish()
                    messageActionBridge.dispatchMenuEvent(
                        MessageActionBridge.MenuEvent.Copy(
                            messages
                        )
                    )
                }

                R.id.sceyt_delete_message -> {
                    messageActionBridge.dispatchMenuEvent(
                        MessageActionBridge.MenuEvent.Delete(
                            messages = messages,
                            requireForMe = messages.any { it.incoming },
                            actionFinish = actionFinish
                        )
                    )
                }

                R.id.sceyt_retract_vote -> firstMessage?.let {
                    actionFinish()
                    messageActionBridge.dispatchMenuEvent(
                        MessageActionBridge.MenuEvent.RetractVote(
                            it
                        )
                    )
                }

                R.id.sceyt_end_vote -> firstMessage?.let {
                    actionFinish()
                    messageActionBridge.dispatchMenuEvent(MessageActionBridge.MenuEvent.EndVote(it))
                }
            }
        }
    }

    messageActionBridge.effects.onEach { effect ->
        when (effect) {
            is MessageActionBridge.Effect.MessageActionsShown -> showMessageActions(effect.messages)
            is MessageActionBridge.Effect.MessageActionsHidden,
            is MessageActionBridge.Effect.MultiSelectCanceled -> {
                headerView.uiElementsListeners.onHideMessageActionsMenu()
            }

            is MessageActionBridge.Effect.SearchRequested -> {
                headerView.uiElementsListeners.showSearchMessagesBar(effect.event)
            }

            is MessageActionBridge.Effect.SearchModeChanged -> Unit

            is MessageActionBridge.Effect.ExitSearchRequested -> {
                headerView.cancelSearchMessagesMode()
            }
        }
    }.launchIn(lifecycleOwner.lifecycleScope)

    if (selectedMessagesMap.isNotEmpty())
        showMessageActions(selectedMessagesMap.values.toList())

    headerView.setToolbarActionHiddenCallback {
        messageActionBridge.cancelMultiSelectMode()
    }

    headerView.setSearchModeChangeListener {
        messageActionBridge.searchModeChanged(it)
    }

    headerView.setSearchQueryChangeListener {
        searchMessages(it)
    }

    if (replyInThread)
        headerView.setReplyMessage(channel, replyInThreadMessage)
    else
        headerView.setChannel(channel, true)

    peerPresenceUpdatedFlow.observe(lifecycleOwner, headerView::onPresenceUpdate)

    ConnectionEventManager.onChangedConnectStatusFlow
        .stateIn(
            lifecycleOwner.lifecycleScope,
            started = SharingStarted.Lazily,
            initialValue = ConnectionStateData(ConnectionEventManager.connectionState)
        )
        .onEach { state ->
            state.state?.let { headerView.onConnectionStateUpdate(it, channel) }
        }
        .launchIn(lifecycleOwner.lifecycleScope)

    onChannelUpdatedEventFlow.onEach {
        headerView.setChannel(it, true)
    }.launchIn(lifecycleOwner.lifecycleScope)

    if (channel.isDirect() && !channel.isSelf)
        lifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay((1000 * 60).milliseconds)

                // Relative labels like "last seen 1 min ago" change with time even without presence events.
                headerView.refreshSubTitle(channel)
            }
        }

    onChannelMemberActivityEventFlow
        .onEach(headerView::handleMemberActivityEvent)
        .flowOn(Dispatchers.Main)
        .launchIn(lifecycleOwner.lifecycleScope)
}
