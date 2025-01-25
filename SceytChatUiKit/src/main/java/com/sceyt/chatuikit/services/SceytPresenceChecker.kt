package com.sceyt.chatuikit.services

import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytPresence
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.getPresentableName
import com.sceyt.chatuikit.extensions.isAppOnForeground
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import java.util.Collections
import java.util.Timer
import kotlin.concurrent.timer

object SceytPresenceChecker : SceytKoinComponent {
    private val userInteractor: UserInteractor by inject()

    private val onPresenceCheckUsersFlow_: MutableSharedFlow<List<PresenceUser>> = MutableSharedFlow(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onPresenceCheckUsersFlow = onPresenceCheckUsersFlow_.asSharedFlow()

    private const val presenceCheckCapacity = 12
    private var workJob: Job? = null

    @Volatile
    private var presenceCheckTimer: Timer? = null
    private val presenceCheckUsers = Collections.synchronizedMap(object : LinkedHashMap<String, SceytUser>(presenceCheckCapacity) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SceytUser>): Boolean {
            return size > presenceCheckCapacity
        }
    })

    @Synchronized
    fun startPresenceCheck() {
        if (presenceCheckTimer == null) {
            presenceCheckTimer = timer(name = "presence", initialDelay = 4000, period = 4000) {
                runBlocking { getUsers() }
            }
        }
    }

    private suspend fun getUsers() {
        if (presenceCheckUsers.keys.isEmpty() || !isAppOnForeground() || !ConnectionEventManager.isConnected) return
        val result = userInteractor.getUsersByIds(presenceCheckUsers.keys.toList())
        if (result is SceytResponse.Success) {
            result.data?.let { users ->
                users.forEach {
                    updateUserPresence(it)
                }
                onPresenceCheckUsersFlow_.tryEmit(users.map { PresenceUser(it) })
            }
        }
    }

    private fun updateUserPresence(user: SceytUser) {
        val oldUser = presenceCheckUsers[user.id]
        if (oldUser != null) presenceCheckUsers[user.id] = user
    }

    fun addNewUserToPresenceCheck(userId: String?) {
        userId ?: return
        presenceCheckUsers[userId] = SceytUser(userId)
    }

    fun removeFromPresenceCheck(userId: String) {
        presenceCheckUsers.remove(userId)
    }

    fun getUser(id: String): SceytUser? {
        return presenceCheckUsers[id]
    }

    @Synchronized
    fun stopPresenceCheck() {
        if (presenceCheckTimer != null) {
            presenceCheckTimer?.cancel()
            presenceCheckTimer?.purge()
            workJob?.cancel()
            presenceCheckTimer = null
        }
    }

    data class PresenceUser(val user: SceytUser) {

        private fun arePresencesEquals(one: SceytPresence?, two: SceytPresence?): Boolean {
            return one?.status == two?.status && one?.state == two?.state && one?.lastActiveAt == two?.lastActiveAt
        }

        override fun equals(other: Any?): Boolean {
            return other is PresenceUser && other.user.id == user.id
                    && arePresencesEquals(other.user.presence, user.presence)
                    && other.user.getPresentableName() == user.getPresentableName()
                    && other.user.avatarURL == user.avatarURL
        }

        override fun hashCode(): Int {
            return user.hashCode()
        }
    }
}