package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.createMessage
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.interactor.MessagePinInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PinControllerTest {

    private val dispatcher = StandardTestDispatcher()
    private val pinInteractor = mock<MessagePinInteractor>()
    private val channelId = 7L
    private val notifications = mutableListOf<Pair<SceytResponse<*>, Boolean>>()

    private fun controller(scope: CoroutineScope) = PinController(
        scope = scope,
        pinInteractor = pinInteractor,
        channelId = { channelId },
        notifyResponse = { response, showError -> notifications += response to showError },
    )

    @Test
    fun `pin forwards the chosen scope to the interactor`() = runTest(dispatcher) {
        whenever(pinInteractor.pinMessage(any(), any(), any()))
            .doReturn(SceytResponse.Success(null))
        val controller = controller(this)

        controller.pin(createMessage(createdAt = 1, id = 1, tid = 1), PinType.PERSONAL)
        advanceUntilIdle()

        verifyBlocking(pinInteractor) {
            pinMessage(eq(channelId), eq(1L), eq(PinType.PERSONAL))
        }
    }

    @Test
    fun `each tap reaches persistence which owns duplicate detection`() = runTest(dispatcher) {
        whenever(pinInteractor.pinMessage(any(), any(), any()))
            .doReturn(SceytResponse.Success(null))
        val controller = controller(this)
        val message = createMessage(createdAt = 1, id = 1, tid = 1)

        controller.pin(message, PinType.SHARED)
        controller.pin(message, PinType.SHARED)
        advanceUntilIdle()

        // Persistence can reject a duplicate without blocking local updates for other taps.
        verifyBlocking(pinInteractor, times(2)) { pinMessage(any(), any(), any()) }
    }

    @Test
    fun `unpin reports its response without a message`() = runTest(dispatcher) {
        whenever(pinInteractor.unpinMessage(any(), any()))
            .doReturn(SceytResponse.Error(null))
        val controller = controller(this)

        controller.unpin(2L)
        advanceUntilIdle()

        // The page state still records the failure, but silently: the intent is durable and
        // is retried on reconnect, so there is nothing for the user to do about it.
        assertThat(notifications).hasSize(1)
        assertThat(notifications.single().second).isFalse()
    }

    @Test
    fun `a failed pin is not reported to the user either`() = runTest(dispatcher) {
        whenever(pinInteractor.pinMessage(any(), any(), any()))
            .doReturn(SceytResponse.Error(null))
        val controller = controller(this)

        controller.pin(createMessage(createdAt = 1, id = 1, tid = 1), PinType.SHARED)
        advanceUntilIdle()

        assertThat(notifications.single().second).isFalse()
    }

    @Test
    fun `a permanent pin failure is shown to the user`() = runTest(dispatcher) {
        val error = mock<SceytException> { on { type }.thenReturn("NotAllowed") }
        whenever(pinInteractor.pinMessage(any(), any(), any()))
            .thenAnswer { SceytResponse.Error<SceytPinnedMessage>(error) }

        controller(this).pin(createMessage(createdAt = 1, id = 1, tid = 1), PinType.SHARED)
        advanceUntilIdle()

        assertThat(notifications.single().second).isTrue()
    }

    @Test
    fun `a permanent unpin failure is shown to the user`() = runTest(dispatcher) {
        val error = mock<SceytException> { on { type }.thenReturn("NotAllowed") }
        whenever(pinInteractor.unpinMessage(any(), any()))
            .thenAnswer { SceytResponse.Error<Boolean>(error) }

        controller(this).unpin(1L)
        advanceUntilIdle()

        assertThat(notifications.single().second).isTrue()
    }

    @Test
    fun `unpin reaches persistence while the previous pin request is stalled`() = runTest(dispatcher) {
        val started = kotlinx.coroutines.CompletableDeferred<Unit>()
        val release = kotlinx.coroutines.CompletableDeferred<Unit>()
        org.mockito.kotlin.doSuspendableAnswer {
            started.complete(Unit)
            release.await()
            SceytResponse.Success<SceytPinnedMessage>(null)
        }.whenever(pinInteractor) { pinMessage(any(), any(), any()) }
        whenever(pinInteractor.unpinMessage(any(), any())).doReturn(SceytResponse.Success(true))
        val controller = controller(this)
        controller.pin(createMessage(createdAt = 1, id = 1, tid = 1), PinType.SHARED)
        started.await()
        controller.unpin(1L)
        runCurrent()
        verifyBlocking(pinInteractor) { unpinMessage(channelId, 1L) }
        release.complete(Unit)
    }

}
