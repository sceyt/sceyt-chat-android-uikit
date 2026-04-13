package com.sceyt.chatuikit.presentation.components.global_search

import android.os.Bundle
import com.sceyt.chatuikit.data.models.messages.SceytUser
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GlobalSearchSessionState(
    val query: String = "",
    val selectedMember: SceytUser? = null,
    val activeTab: GlobalSearchTab = GlobalSearchTab.Chats,
) {
    fun isCurrent(tab: GlobalSearchTab) = activeTab == tab

    fun isQueryChanged(newQuery: String) = query != newQuery
}

interface GlobalSearchSession {
    val state: StateFlow<GlobalSearchSessionState>
}

object GlobalSearchSessionResolver {
    fun require(arguments: Bundle?): GlobalSearchSession {
        val sessionId = arguments?.getString(GlobalSearchActivity.SESSION_ID_KEY)
            ?: error("GlobalSearch sessionId argument is required.")
        return GlobalSearchSessionRegistry.require(sessionId)
    }
}

internal class GlobalSearchSessionStore(
    initialState: GlobalSearchSessionState,
) : GlobalSearchSession {
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<GlobalSearchSessionState> = _state.asStateFlow()

    fun update(transform: (GlobalSearchSessionState) -> GlobalSearchSessionState) {
        _state.update(transform)
    }
}

internal object GlobalSearchSessionRegistry {
    private val sessions = ConcurrentHashMap<String, GlobalSearchSessionStore>()

    fun newSessionId(): String = UUID.randomUUID().toString()

    fun register(sessionId: String, session: GlobalSearchSessionStore) {
        sessions[sessionId] = session
    }

    fun unregister(sessionId: String) {
        sessions.remove(sessionId)
    }

    fun require(sessionId: String): GlobalSearchSessionStore {
        return sessions[sessionId]
            ?: error("GlobalSearch session is no longer available for id: $sessionId")
    }

    fun contains(sessionId: String): Boolean = sessions.containsKey(sessionId)
}
