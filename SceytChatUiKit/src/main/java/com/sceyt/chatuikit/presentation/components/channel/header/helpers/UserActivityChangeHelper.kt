package com.sceyt.chatuikit.presentation.components.channel.header.helpers

import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.presentation.common.ConcurrentHashSet
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

enum class UserActivityState {
    Typing, Recording
}

enum class UsersActivityState {
    Typing, Recording, None
}

class UserActivityChangeHelper(
        private val scope: CoroutineScope,
        private val activeUsersUpdated: (List<ActiveUser>) -> Unit,
        private val showActiveUsersInSequence: Boolean
) {
    private val userActivityCancelHelper by lazy { UserActivityCancelHelper() }
    private val _activeUsers by lazy { ConcurrentHashSet<ActiveUser>() }
    private val debounceHelpers = ConcurrentHashMap<String, DebounceHelper>()
    private var updateActiveUsersJob: Job? = null

    private fun updateUserActivityText() {
        if (!showActiveUsersInSequence) {
            notifyUpdatesWithActiveUsers(activeUsers)
            return
        }

        if (_activeUsers.isEmpty() || _activeUsers.size == 1) {
            updateActiveUsersJob?.cancel()
            notifyUpdatesWithActiveUsers(activeUsers)
        } else {
            updateUserActivityTitleEveryTwoSecond()
        }
    }

    private fun updateUserActivityTitleEveryTwoSecond() {
        if (updateActiveUsersJob?.isActive == true) return
        updateActiveUsersJob = scope.launch {
            var index = 0
            while (isActive) {
                val users = _activeUsers.toList()

                if (users.isEmpty())
                    break

                if (index >= users.size)
                    index = 0

                val currentUser = users.getOrNull(index)
                currentUser?.let {
                    notifyUpdatesWithActiveUsers(listOf(it))
                }

                index++
                delay(2000)
            }
        }
    }

    private fun notifyUpdatesWithActiveUsers(activeUsers: List<ActiveUser>) {
        activeUsersUpdated.invoke(activeUsers)
    }

    private fun handleActivity(event: ChannelMemberActivityEvent) {
        val debounceHelper = debounceHelpers.computeIfAbsent(event.userId) {
            DebounceHelper(200, scope)
        }
        debounceHelper.submit {
            val activeUser = when (event) {
                is ChannelMemberActivityEvent.Recording -> {
                    ActiveUser(event.user, UserActivityState.Recording)
                }

                is ChannelMemberActivityEvent.Typing -> {
                    ActiveUser(event.user, UserActivityState.Typing)
                }
            }
            // Remove last active user
            _activeUsers.remove(activeUser)

            if (event.active) {
                _activeUsers.add(activeUser)
            }
            updateUserActivityText()
        }
    }

    fun onActivityEvent(event: ChannelMemberActivityEvent) {
        userActivityCancelHelper.await(event) {
            handleActivity(it)
        }
        handleActivity(event)
    }

    fun getActivityState(activeUsers: List<ActiveUser>): UsersActivityState {
        return when {
            activeUsers.isEmpty() -> UsersActivityState.None
            activeUsers.any { it.activity == UserActivityState.Typing } -> UsersActivityState.Typing
            else -> UsersActivityState.Recording
        }
    }

    val activeUsers: List<ActiveUser>
        get() = _activeUsers.toList()

    val haveUserAction: Boolean
        get() = _activeUsers.isNotEmpty()
}