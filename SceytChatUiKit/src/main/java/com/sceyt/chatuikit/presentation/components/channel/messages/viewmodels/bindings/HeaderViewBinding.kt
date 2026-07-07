package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.managers.connection.event.ConnectionStateData
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelUpdatedType
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageActionBridge
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

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

    val peerId = channel.getPeer()?.id
    if (channel.isDirect()) {
        SceytPresenceChecker.addNewUserToPresenceCheck(peerId)
        SceytPresenceChecker.onPresenceCheckUsersFlow.distinctUntilChanged()
            .onEach {
                it.find { user -> user.user.id == peerId }?.let { presenceUser ->
                    headerView.onPresenceUpdate(presenceUser.user)
                }
            }.launchIn(lifecycleOwner.lifecycleScope)
    }

    ConnectionEventManager.onChangedConnectStatusFlow
        .stateIn(
            lifecycleOwner.lifecycleScope,
            started = SharingStarted.Lazily,
            initialValue = ConnectionStateData(ConnectionEventManager.connectionState)
        )
        .onEach { state ->
            state.state?.let { headerView.onConnectionStateUpdate(it) }
        }
        .launchIn(lifecycleOwner.lifecycleScope)

    ChannelsCache.channelUpdatedFlow
        .filter { it.channel.id == channel.id }
        .onEach {
            headerView.setChannel(it.channel, it.eventType != ChannelUpdatedType.Presence)
        }
        .launchIn(lifecycleOwner.lifecycleScope)

    onChannelMemberActivityEventFlow
        .onEach(headerView::handleMemberActivityEvent)
        .flowOn(Dispatchers.Main)
        .launchIn(lifecycleOwner.lifecycleScope)
}
