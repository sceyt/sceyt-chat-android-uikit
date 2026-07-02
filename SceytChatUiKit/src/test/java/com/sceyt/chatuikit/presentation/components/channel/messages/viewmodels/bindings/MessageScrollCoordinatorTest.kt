package com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.bindings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MessageScrollCoordinatorTest {

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
