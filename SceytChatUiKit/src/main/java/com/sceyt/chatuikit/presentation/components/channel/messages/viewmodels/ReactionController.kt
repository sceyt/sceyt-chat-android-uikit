package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.interactor.MessageReactionInteractor
import com.sceyt.chatuikit.presentation.components.channel.messages.events.ReactionEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Owns reaction add/remove that used to live inside [MessageListViewModel].
 * Page-state reporting is delegated back through [notifyResponse].
 */
internal class ReactionController(
    private val scope: CoroutineScope,
    private val reactionInteractor: MessageReactionInteractor,
    private val channelId: () -> Long,
    private val notifyResponse: (SceytResponse<*>, showError: Boolean) -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun onEvent(event: ReactionEvent) {
        when (event) {
            is ReactionEvent.AddReaction -> add(event.message, event.scoreKey)
            is ReactionEvent.RemoveReaction -> delete(event.message, event.scoreKey)
        }
    }

    fun add(
        message: SceytMessage,
        scoreKey: String,
        score: Int = 1,
        reason: String = "",
        enforceUnique: Boolean = false,
    ) {
        scope.launch(ioDispatcher) {
            val response = reactionInteractor.addReaction(
                channelId = channelId(),
                messageId = message.id,
                key = scoreKey,
                score = score,
                reason = reason,
                enforceUnique = enforceUnique
            )
            notifyResponse(response, false)
        }
    }

    fun delete(message: SceytMessage, scoreKey: String) {
        scope.launch(ioDispatcher) {
            val response = reactionInteractor.deleteReaction(
                channelId = channelId(),
                messageId = message.id,
                scoreKey = scoreKey
            )
            notifyResponse(response, false)
        }
    }
}