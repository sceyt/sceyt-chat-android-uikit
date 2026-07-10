package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.presentation.common.recyclerview.ScrollHandle
import org.junit.Test

class MessageScrollCoordinatorTest {

    @Test
    fun `superseding a request cancels its physical scroll`() {
        val coordinator = MessageScrollCoordinator()
        val first = coordinator.beginMessageRequest(targetMessageId = 1)
        val handle = ScrollHandle()
        coordinator.attachPhysicalHandle(first.id, handle)

        coordinator.beginMessageRequest(targetMessageId = 2)

        assertThat(handle.cancelled).isTrue()
    }

    @Test
    fun `superseding a request with a pending load cancels the load`() {
        var loadCancelled = false
        val coordinator = MessageScrollCoordinator(cancelPendingLoad = { loadCancelled = true })
        val first = coordinator.beginMessageRequest(targetMessageId = 1)
        coordinator.markLoadStarted(first.id)

        coordinator.beginUnreadRequest()

        assertThat(loadCancelled).isTrue()
    }

    @Test
    fun `attaching a handle to a superseded request cancels it immediately`() {
        val coordinator = MessageScrollCoordinator()
        val first = coordinator.beginMessageRequest(targetMessageId = 1)
        coordinator.beginMessageRequest(targetMessageId = 2)

        val handle = ScrollHandle()
        coordinator.attachPhysicalHandle(first.id, handle)

        assertThat(handle.cancelled).isTrue()
    }

    @Test
    fun `hasActiveExplicitJump reflects request type`() {
        val coordinator = MessageScrollCoordinator()

        coordinator.beginMessageRequest(targetMessageId = 1)
        assertThat(coordinator.hasActiveExplicitJump()).isTrue()

        coordinator.beginRealtimeScrollRequest()
        assertThat(coordinator.hasActiveExplicitJump()).isFalse()

        coordinator.beginUnreadRequest()
        assertThat(coordinator.hasActiveExplicitJump()).isTrue()
    }

    @Test
    fun `new request invalidates older request`() {
        val coordinator = MessageScrollCoordinator()

        val first = coordinator.beginMessageRequest(targetMessageId = 1)
        val second = coordinator.beginMessageRequest(targetMessageId = 2)

        assertThat(coordinator.activeRequestFor(first.id)).isNull()
        assertThat(coordinator.activeRequestFor(second.id)).isEqualTo(second)
    }

    @Test
    fun `newest request is kept until loading settles`() {
        val coordinator = MessageScrollCoordinator()
        val request = coordinator.beginNewestMessageRequest(targetMessageId = 10)

        coordinator.clearIfSettled(request, loadingInProgress = true)

        assertThat(coordinator.activeNewestMessageRequest()).isEqualTo(request)

        coordinator.clearIfSettled(request, loadingInProgress = false)

        assertThat(coordinator.activeNewestMessageRequest()).isNull()
    }

    @Test
    fun `message request clears even when loading is active`() {
        val coordinator = MessageScrollCoordinator()
        val request = coordinator.beginMessageRequest(targetMessageId = 10)

        coordinator.clearIfSettled(request, loadingInProgress = true)

        assertThat(coordinator.activeRequestFor(request.id)).isNull()
    }

    @Test
    fun `delayed work is ignored after newer request starts`() {
        val coordinator = MessageScrollCoordinator()
        val first = coordinator.beginNewestMessageRequest(targetMessageId = 1)

        coordinator.beginMessageRequest(targetMessageId = 2)

        assertThat(coordinator.canRunDelayedWorkFor(first)).isFalse()
    }

    @Test
    fun `cancel active request clears newest request`() {
        val coordinator = MessageScrollCoordinator()
        coordinator.beginNewestMessageRequest(targetMessageId = 1)

        coordinator.cancelActiveRequest()

        assertThat(coordinator.activeNewestMessageRequest()).isNull()
    }
}
