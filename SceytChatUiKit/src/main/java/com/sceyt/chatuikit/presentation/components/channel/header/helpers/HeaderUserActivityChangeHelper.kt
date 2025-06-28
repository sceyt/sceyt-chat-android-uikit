package com.sceyt.chatuikit.presentation.components.channel.header.helpers

import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.asComponentActivity
import com.sceyt.chatuikit.presentation.common.ConcurrentHashSet
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class ActiveUser(
        val user: SceytUser,
        val activity: UserActivityState
) {
    override fun equals(other: Any?): Boolean {
        return other is ActiveUser && other.user.id == user.id
    }

    override fun hashCode(): Int {
        return user.id.hashCode()
    }
}

enum class UserActivityState {
    Typing, Recording
}

enum class UsersActivityState {
    Typing, Recording, None
}

class HeaderUserActivityChangeHelper(
        private val context: Context,
        private val activeUsersUpdated: (List<ActiveUser>) -> Unit,
        private val showActiveUsersInSequence: Boolean
) {
    private val userActivityCancelHelper by lazy { UserActivityCancelHelper() }
    private val _activeUsers by lazy { ConcurrentHashSet<ActiveUser>() }
    private val debounceHelpers = ConcurrentHashMap<String, DebounceHelper>()
    private var updateActiveUsersJob: Job? = null

    private fun updateUserActivityText() {
        if (!showActiveUsersInSequence) {
            val activeUsers = this.activeUsers
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
        updateActiveUsersJob = context.asComponentActivity().lifecycleScope.launch {
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
        if (event.userId == SceytChatUIKit.currentUserId) return
        val debounceHelper = debounceHelpers.computeIfAbsent(event.userId) {
            DebounceHelper(200, context.asComponentActivity().lifecycleScope)
        }
        val activeUser = when (event) {
            is ChannelMemberActivityEvent.Recording -> {
                ActiveUser(event.user, UserActivityState.Recording)
            }

            is ChannelMemberActivityEvent.Typing -> {
                ActiveUser(event.user, UserActivityState.Typing)
            }
        }
        debounceHelper.submit {
            if (event.active) {
                _activeUsers.add(activeUser)
            } else
                _activeUsers.remove(activeUser)

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