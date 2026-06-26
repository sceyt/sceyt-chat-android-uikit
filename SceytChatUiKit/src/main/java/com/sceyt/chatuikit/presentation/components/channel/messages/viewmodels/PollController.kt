package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.interactor.MessagePollInteractor
import com.sceyt.chatuikit.presentation.components.channel.messages.events.PollEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Owns poll voting (toggle / retract / end) that used to live inside [MessageListViewModel].
 * A single in-flight [toggleVoteJob] is kept so overlapping vote actions are ignored.
 * Page-state reporting is delegated back through [notifyResponse].
 */
internal class PollController(
    private val scope: CoroutineScope,
    private val pollInteractor: MessagePollInteractor,
    private val channelId: () -> Long,
    private val notifyResponse: (SceytResponse<*>, showError: Boolean) -> Unit,
) {
    private var toggleVoteJob: Job? = null

    fun onEvent(event: PollEvent) {
        if (toggleVoteJob?.isActive == true) return
        toggleVoteJob = when (event) {
            is PollEvent.ToggleVote -> toggleVote(event.message, event.option)
            is PollEvent.RetractVote -> retract(event.message)
            is PollEvent.EndVote -> end(event.message)
        }
    }

    private fun toggleVote(
        message: SceytMessage,
        option: PollOption,
    ) = scope.launch {
        val poll = message.poll ?: return@launch
        val response = pollInteractor.toggleVote(
            channelId = channelId(),
            messageTid = message.tid,
            pollId = poll.id,
            optionId = option.id
        )
        notifyResponse(response, false)
    }

    private fun retract(
        message: SceytMessage,
    ) = scope.launch {
        val poll = message.poll ?: return@launch
        if (!poll.allowVoteRetract || poll.ownVotes.isEmpty()) return@launch

        val response = pollInteractor.retractVote(
            channelId = channelId(),
            messageTid = message.tid,
            pollId = poll.id
        )
        notifyResponse(response, true)
    }

    private fun end(
        message: SceytMessage,
    ) = scope.launch {
        val poll = message.poll ?: return@launch
        if (poll.closed) return@launch

        val response = pollInteractor.endPoll(
            channelId = channelId(),
            messageTid = message.tid,
            pollId = poll.id
        )
        notifyResponse(response, true)
    }
}