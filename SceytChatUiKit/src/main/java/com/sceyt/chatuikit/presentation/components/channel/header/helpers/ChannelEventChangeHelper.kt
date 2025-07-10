package com.sceyt.chatuikit.presentation.components.channel.header.helpers

import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.presentation.common.ConcurrentHashSet
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import com.sceyt.chatuikit.presentation.components.channel.input.data.ChannelEventEnum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

enum class ChannelEventState {
    Typing, Recording, None
}

class ChannelEventChangeHelper(
        private val scope: CoroutineScope,
        private val activeUsersUpdated: (List<ChannelEventData>) -> Unit,
        private val showChannelEventsInSequence: Boolean,
) {
    private val channelEventCancelHelper by lazy { ChannelEventCancelHelper() }
    private val _channelEventData by lazy { ConcurrentHashSet<ChannelEventData>() }
    private val debounceHelpers = ConcurrentHashMap<String, DebounceHelper>()
    private var updateEventsJob: Job? = null

    private fun updateChannelEventText() {
        if (!showChannelEventsInSequence) {
            notifyUpdatesWithChannelEvents(channelEventData)
            return
        }

        if (_channelEventData.isEmpty() || _channelEventData.size == 1) {
            updateEventsJob?.cancel()
            notifyUpdatesWithChannelEvents(channelEventData)
        } else {
            updateChannelEventTitleEveryTwoSecond()
        }
    }

    private fun updateChannelEventTitleEveryTwoSecond() {
        if (updateEventsJob?.isActive == true) return
        updateEventsJob = scope.launch {
            var index = 0
            while (isActive) {
                val users = _channelEventData.toList()

                if (users.isEmpty())
                    break

                if (index >= users.size)
                    index = 0

                val currentUser = users.getOrNull(index)
                currentUser?.let {
                    notifyUpdatesWithChannelEvents(listOf(it))
                }

                index++
                delay(2000)
            }
        }
    }

    private fun notifyUpdatesWithChannelEvents(channelEvents: List<ChannelEventData>) {
        activeUsersUpdated.invoke(channelEvents)
    }

    private fun handleActivity(event: ChannelMemberActivityEvent) {
        val debounceHelper = debounceHelpers.computeIfAbsent(event.userId) {
            DebounceHelper(200, scope)
        }
        debounceHelper.submit {
            val channelEventData = ChannelEventData(event.user, event.activity)

            // Remove last active user
            _channelEventData.remove(channelEventData)

            if (event.active) {
                _channelEventData.add(channelEventData)
            }
            updateChannelEventText()
        }
    }

    fun onActivityEvent(event: ChannelMemberActivityEvent) {
        channelEventCancelHelper.await(event) {
            handleActivity(it)
        }
        handleActivity(event)
    }

    val channelEventData: List<ChannelEventData>
        get() = _channelEventData.toList()

    val haveUserAction: Boolean
        get() = _channelEventData.isNotEmpty()


    companion object {

        fun getChannelEventState(events: List<ChannelEventData>): ChannelEventState {
            return when {
                events.isEmpty() -> ChannelEventState.None
                events.any { it.activity == ChannelEventEnum.Typing } -> ChannelEventState.Typing
                else -> ChannelEventState.Recording
            }
        }
    }
}