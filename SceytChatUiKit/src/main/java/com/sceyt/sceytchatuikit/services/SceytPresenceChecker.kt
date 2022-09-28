package com.sceyt.sceytchatuikit.services

import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.PersistenceUsersMiddleWare
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.component.inject
import java.util.*
import kotlin.coroutines.CoroutineContext

object SceytPresenceChecker : SceytKoinComponent, CoroutineScope {

    private val persistenceUsersMiddleWare: PersistenceUsersMiddleWare by inject()

    private val onPresenceCheckUsersFlow_: MutableSharedFlow<List<User>> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onPresenceCheckUsersFlow = onPresenceCheckUsersFlow_.asSharedFlow()

    private const val presenceCheckCapacity = 8
    private var workJob: Job? = null
    private var presenceCheckTimer: Timer? = null
    private val presenceCheckUsers = Collections.synchronizedMap(object : LinkedHashMap<String, User>(presenceCheckCapacity) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, User>): Boolean {
            return size > presenceCheckCapacity
        }
    })

    @Synchronized
    fun startPresenceCheck() {
        if (presenceCheckTimer == null) {
            presenceCheckTimer = Timer("presence")
            presenceCheckTimer?.schedule(object : TimerTask() {
                override fun run() {
                    getUsers()
                }
            }, 4000, 4000)
        }
    }

    private fun getUsers() {
        if (presenceCheckUsers.keys.isEmpty()) return
        workJob = launch {
            val result = persistenceUsersMiddleWare.getUsersByIds(presenceCheckUsers.keys.toList())
            if (result is SceytResponse.Success) {
                result.data?.let { users ->
                    users.forEach {
                        updateUserPresence(it)
                    }
                    onPresenceCheckUsersFlow_.tryEmit(users)
                }
            }
        }
    }

    private fun updateUserPresence(user: User) {
        val oldUser = presenceCheckUsers[user.id]
        if (oldUser != null) presenceCheckUsers[user.id] = user
    }

    fun addNewUserToPresenceCheck(userId: String) {
        presenceCheckUsers[userId] = User(userId)
    }

    fun removeFromPresenceCheck(userId: String) {
        presenceCheckUsers.remove(userId)
    }

    fun getUser(id: String): User? {
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

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()
}