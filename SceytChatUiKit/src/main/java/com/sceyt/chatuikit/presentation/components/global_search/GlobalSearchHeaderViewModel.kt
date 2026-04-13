package com.sceyt.chatuikit.presentation.components.global_search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sceyt.chatuikit.data.models.messages.SceytUser
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

internal const val DEFAULT_MEMBER_SUGGESTIONS_LIMIT = 8
internal const val DEFAULT_MEMBER_SUGGESTIONS_DEBOUNCE_MS = 0L

data class GlobalSearchHeaderState(
    val activeTab: GlobalSearchTab = GlobalSearchTab.Chats,
    val query: String = "",
    val selectedMember: SceytUser? = null,
    val memberSuggestions: List<SceytUser> = emptyList(),
    val isSelectedMemberRemovalPending: Boolean = false,
) {
    val showSuggestions: Boolean
        get() = query.isNotBlank() && selectedMember == null && memberSuggestions.isNotEmpty()
}

open class GlobalSearchHeaderViewModel internal constructor(
    initialTab: GlobalSearchTab = GlobalSearchTab.Chats,
    private val memberSuggestionsProvider: GlobalSearchMemberSuggestionsProvider = GlobalSearchLocalInteractor(),
    private val memberSuggestionsLimit: Int = DEFAULT_MEMBER_SUGGESTIONS_LIMIT,
    private val memberSuggestionsDebounceMs: Long = DEFAULT_MEMBER_SUGGESTIONS_DEBOUNCE_MS,
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
                selectedMember = user,
                memberSuggestions = emptyList(),
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
                selectedMember = null,
                memberSuggestions = emptyList(),
                isSelectedMemberRemovalPending = false,
            )
        }
        sessionStore.update { it.copy(selectedMember = null) }
        refreshSuggestions(_headerState.value.query)
    }

    fun onClearRequested() {
        when {
            _headerState.value.query.isNotEmpty() -> onQueryChanged("")
            _headerState.value.selectedMember != null -> onSelectedMemberRemoved()
        }
    }

    fun onEmptyQueryDeleteRequested() {
        val state = _headerState.value
        when {
            state.selectedMember == null -> return
            !state.isSelectedMemberRemovalPending -> {
                suggestionJob?.cancel()
                _headerState.update {
                    it.copy(
                        memberSuggestions = emptyList(),
                        isSelectedMemberRemovalPending = true,
                    )
                }
            }

            else -> onSelectedMemberRemoved()
        }
    }

    protected open fun refreshSuggestions(query: String) {
        suggestionJob?.cancel()
        if (query.isBlank() || _headerState.value.selectedMember != null) {
            _headerState.update { it.copy(memberSuggestions = emptyList()) }
            return
        }

        suggestionJob = viewModelScope.launch(ioDispatcher) {
            if (memberSuggestionsDebounceMs > 0) {
                delay(memberSuggestionsDebounceMs)
            }
            val suggestions = try {
                memberSuggestionsProvider.provideSuggestions(query, memberSuggestionsLimit)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                emptyList()
            }
            withContext(Dispatchers.Main) {
                val current = _headerState.value
                if (current.query == query && current.selectedMember == null) {
                    _headerState.update { it.copy(memberSuggestions = suggestions) }
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
    private val memberSuggestionsProvider: GlobalSearchMemberSuggestionsProvider = GlobalSearchLocalInteractor(),
    private val memberSuggestionsLimit: Int = DEFAULT_MEMBER_SUGGESTIONS_LIMIT,
    private val memberSuggestionsDebounceMs: Long = DEFAULT_MEMBER_SUGGESTIONS_DEBOUNCE_MS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GlobalSearchHeaderViewModel::class.java)) {
            return GlobalSearchHeaderViewModel(
                initialTab = initialTab,
                memberSuggestionsProvider = memberSuggestionsProvider,
                memberSuggestionsLimit = memberSuggestionsLimit,
                memberSuggestionsDebounceMs = memberSuggestionsDebounceMs,
                ioDispatcher = ioDispatcher
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
