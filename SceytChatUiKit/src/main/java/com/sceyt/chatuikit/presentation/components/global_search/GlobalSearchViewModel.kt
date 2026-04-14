package com.sceyt.chatuikit.presentation.components.global_search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.persistence.interactor.GlobalSearchUserSuggestionsProvider
import com.sceyt.chatuikit.presentation.components.global_search.defaults.DefaultUserSuggestionsProvider
import com.sceyt.chatuikit.presentation.helpers.DebounceHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal const val DEFAULT_USER_SUGGESTIONS_LIMIT = 8
internal const val DEFAULT_USER_SUGGESTIONS_DEBOUNCE_MS = 0L

data class GlobalSearchHeaderState(
    val activeTab: GlobalSearchTab = GlobalSearchTab.Chats,
    val query: String = "",
    val selectedUser: SceytUser? = null,
    val userSuggestions: List<SceytUser> = emptyList(),
    val isSelectedMemberRemovalPending: Boolean = false,
) {
    val showSuggestions: Boolean
        get() = query.isNotBlank() && selectedUser == null && userSuggestions.isNotEmpty()
}

open class GlobalSearchViewModel(
    initialTab: GlobalSearchTab = GlobalSearchTab.Chats,
    private val userSuggestionsProvider: GlobalSearchUserSuggestionsProvider,
    private val userSuggestionsLimit: Int = DEFAULT_USER_SUGGESTIONS_LIMIT,
    private val userSuggestionsDebounceMs: Long = DEFAULT_USER_SUGGESTIONS_DEBOUNCE_MS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val sessionStore = GlobalSearchSessionStore(
        GlobalSearchSessionState(activeTab = initialTab)
    )

    val sessionId: String = GlobalSearchSessionRegistry.newSessionId().also { sessionId ->
        GlobalSearchSessionRegistry.register(sessionId, sessionStore)
    }
    protected val debounceHelper = DebounceHelper(200, viewModelScope)

    private val _headerState = MutableStateFlow(GlobalSearchHeaderState(activeTab = initialTab))
    val headerState = _headerState.asStateFlow()

    private var suggestionJob: Job? = null

    init {
        refreshSuggestions("")
    }

    fun onQueryChanged(query: String) {
        val normalized = query.trimStart()
        val currentState = _headerState.value
        if (currentState.query == normalized) return

        _headerState.update {
            it.copy(
                query = normalized,
                isSelectedMemberRemovalPending = false,
            )
        }

        debounceHelper.submit {
            sessionStore.update { it.copy(query = normalized) }
            refreshSuggestions(normalized)
        }
    }

    fun onTabSelected(tab: GlobalSearchTab) {
        if (_headerState.value.activeTab == tab) return
        _headerState.update { it.copy(activeTab = tab) }
        sessionStore.update { it.copy(activeTab = tab) }
    }

    fun onMemberSelected(user: SceytUser) {
        suggestionJob?.cancel()
        _headerState.update {
            it.copy(
                selectedUser = user,
                userSuggestions = emptyList(),
                isSelectedMemberRemovalPending = false,
                query = "",
            )
        }
        sessionStore.update {
            it.copy(
                selectedMember = user,
                query = "",
            )
        }
    }

    fun onSelectedMemberRemoved() {
        suggestionJob?.cancel()
        _headerState.update {
            it.copy(
                selectedUser = null,
                userSuggestions = emptyList(),
                isSelectedMemberRemovalPending = false,
            )
        }
        sessionStore.update { it.copy(selectedMember = null) }
        refreshSuggestions(_headerState.value.query)
    }

    fun onClearRequested() {
        when {
            _headerState.value.query.isNotEmpty() -> onQueryChanged("")
            _headerState.value.selectedUser != null -> onSelectedMemberRemoved()
        }
    }

    fun onEmptyQueryDeleteRequested() {
        val state = _headerState.value
        when {
            state.selectedUser == null -> return
            !state.isSelectedMemberRemovalPending -> {
                suggestionJob?.cancel()
                _headerState.update {
                    it.copy(
                        userSuggestions = emptyList(),
                        isSelectedMemberRemovalPending = true,
                    )
                }
            }

            else -> onSelectedMemberRemoved()
        }
    }

    protected open fun refreshSuggestions(query: String) {
        suggestionJob?.cancel()
        if (query.isBlank() || _headerState.value.selectedUser != null) {
            _headerState.update { it.copy(userSuggestions = emptyList()) }
            return
        }

        suggestionJob = viewModelScope.launch(ioDispatcher) {
            if (userSuggestionsDebounceMs > 0) {
                delay(userSuggestionsDebounceMs)
            }
            val suggestions = try {
                userSuggestionsProvider.provideSuggestions(query, userSuggestionsLimit)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                emptyList()
            }
            withContext(Dispatchers.Main) {
                val current = _headerState.value
                if (current.query == query && current.selectedUser == null) {
                    _headerState.update { it.copy(userSuggestions = suggestions) }
                }
            }
        }
    }

    override fun onCleared() {
        suggestionJob?.cancel()
        GlobalSearchSessionRegistry.unregister(sessionId)
        super.onCleared()
    }
}

internal class GlobalSearchHeaderViewModelFactory(
    private val initialTab: GlobalSearchTab,
    private val userSuggestionsProvider: GlobalSearchUserSuggestionsProvider = DefaultUserSuggestionsProvider(),
    private val userSuggestionsLimit: Int = DEFAULT_USER_SUGGESTIONS_LIMIT,
    private val userSuggestionsDebounceMs: Long = DEFAULT_USER_SUGGESTIONS_DEBOUNCE_MS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GlobalSearchViewModel::class.java)) {
            return GlobalSearchViewModel(
                initialTab = initialTab,
                userSuggestionsProvider = userSuggestionsProvider,
                userSuggestionsLimit = userSuggestionsLimit,
                userSuggestionsDebounceMs = userSuggestionsDebounceMs,
                ioDispatcher = ioDispatcher
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
