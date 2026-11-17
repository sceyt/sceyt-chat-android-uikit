package com.sceyt.chatuikit.presentation.components.forward.viewmodel

import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForwardViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val createdViewModels = mutableListOf<ForwardViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        createdViewModels.forEach { it.viewModelScope.cancel() }
        createdViewModels.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `message deleted for everyone is removed`() = runTest(dispatcher) {
        val first = message(tid = 1)
        val second = message(tid = 2)
        val viewModel = viewModel(first, second)

        MessagesCache().messageUpdated(CHANNEL_ID, first.copy(state = MessageState.Deleted))

        assertThat(viewModel.forwardMessages.value).containsExactly(second)
    }

    @Test
    fun `hard deleted message is removed`() = runTest(dispatcher) {
        val first = message(tid = 1)
        val second = message(tid = 2)
        val viewModel = viewModel(first, second)

        MessagesCache().hardDeleteMessage(CHANNEL_ID, first)

        assertThat(viewModel.forwardMessages.value).containsExactly(second)
    }

    @Test
    fun `deleting every message leaves the list empty`() = runTest(dispatcher) {
        val first = message(tid = 1)
        val second = message(tid = 2)
        val viewModel = viewModel(first, second)

        MessagesCache().hardDeleteMessage(CHANNEL_ID, first.tid, second.tid)

        assertThat(viewModel.forwardMessages.value).isEmpty()
    }

    @Test
    fun `edited message stays`() = runTest(dispatcher) {
        val first = message(tid = 1)
        val viewModel = viewModel(first)

        MessagesCache().messageUpdated(CHANNEL_ID, first.copy(state = MessageState.Edited))

        assertThat(viewModel.forwardMessages.value).containsExactly(first)
    }

    @Test
    fun `deleting another message or a message in another channel keeps the list`() = runTest(dispatcher) {
        val first = message(tid = 1)
        val viewModel = viewModel(first)

        MessagesCache().messageUpdated(CHANNEL_ID, message(tid = 5).copy(state = MessageState.Deleted))
        MessagesCache().hardDeleteMessage(OTHER_CHANNEL_ID, first.tid)

        assertThat(viewModel.forwardMessages.value).containsExactly(first)
    }

    private fun message(tid: Long): SceytMessage =
        createMessage(createdAt = tid, id = tid, tid = tid).copy(channelId = CHANNEL_ID)

    private fun viewModel(vararg messages: SceytMessage) =
        ForwardViewModel(messages.toList()).also { createdViewModels.add(it) }

    private companion object {
        const val CHANNEL_ID = 1L
        const val OTHER_CHANNEL_ID = 99L
    }
}
