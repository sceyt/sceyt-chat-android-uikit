package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageCommandEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class MessageActionBridge {
    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 16)
    val effects = _effects.asSharedFlow()

    private val _menuEvents = MutableSharedFlow<MenuEvent>(extraBufferCapacity = 16)
    val menuEvents = _menuEvents.asSharedFlow()

    fun showMessageActions(vararg selectedMessages: SceytMessage) {
        _effects.tryEmit(Effect.MessageActionsShown(selectedMessages.toList()))
    }

    fun hideMessageActions() {
        _effects.tryEmit(Effect.MessageActionsHidden)
    }

    fun cancelMultiSelectMode() {
        _effects.tryEmit(Effect.MultiSelectCanceled)
    }

    fun showSearchMessage(event: MessageCommandEvent.SearchMessages) {
        _effects.tryEmit(Effect.SearchRequested(event))
    }

    fun searchModeChanged(enabled: Boolean) {
        _effects.tryEmit(Effect.SearchModeChanged(enabled))
    }

    fun exitSearchMode() {
        _effects.tryEmit(Effect.ExitSearchRequested)
    }

    fun dispatchMenuEvent(event: MenuEvent) {
        _menuEvents.tryEmit(event)
    }

    sealed interface Effect {
        data class MessageActionsShown(val messages: List<SceytMessage>) : Effect
        data object MessageActionsHidden : Effect
        data object MultiSelectCanceled : Effect
        data class SearchRequested(val event: MessageCommandEvent.SearchMessages) : Effect
        data class SearchModeChanged(val enabled: Boolean) : Effect
        data object ExitSearchRequested : Effect
    }

    sealed interface MenuEvent {
        data class Copy(val messages: List<SceytMessage>) : MenuEvent
        data class Delete(
            val messages: List<SceytMessage>,
            val requireForMe: Boolean,
            val actionFinish: () -> Unit,
        ) : MenuEvent

        data class Edit(val message: SceytMessage) : MenuEvent
        data class MessageInfo(val message: SceytMessage) : MenuEvent
        data class Forward(val messages: List<SceytMessage>) : MenuEvent
        data class Reply(val message: SceytMessage) : MenuEvent
        data class ReplyInThread(val message: SceytMessage) : MenuEvent
        data class RetractVote(val message: SceytMessage) : MenuEvent
        data class EndVote(val message: SceytMessage) : MenuEvent
    }
}
