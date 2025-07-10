package com.sceyt.chatuikit.presentation.components.channel.header.helpers

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.components.channel.input.data.ChannelEventEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ChannelEventChangeHelperTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var helper: ChannelEventChangeHelper
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var scope: TestScope

    private val testUser1 = SceytUser("user1")
    private val testUser2 = SceytUser("user2")
    private val testChannel = createChannel(1, 0, 1)

    private var capturedChannelEventData: List<ChannelEventData> = emptyList()
    private val activeUsersCallback: (List<ChannelEventData>) -> Unit = { users ->
        capturedChannelEventData = users
    }

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        scope = TestScope(testDispatcher)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when typing event is received, should add active user`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val typingEvent = ChannelMemberActivityEvent(
            testChannel,
            testUser1,
            ChannelEventEnum.Typing,
            true
        )

        // When
        helper.onActivityEvent(typingEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then
        assertThat(capturedChannelEventData).hasSize(1)
        assertThat(capturedChannelEventData.first().user.id).isEqualTo(testUser1.id)
        assertThat(capturedChannelEventData.first().activity).isEqualTo(ChannelEventEnum.Typing)
    }

    @Test
    fun `when recording event is received, should add active user`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val recordingEvent = ChannelMemberActivityEvent(
            testChannel,
            testUser1,
            ChannelEventEnum.Recording,
            true
        )

        // When
        helper.onActivityEvent(recordingEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then
        assertThat(capturedChannelEventData).hasSize(1)
        assertThat(capturedChannelEventData.first().user.id).isEqualTo(testUser1.id)
        assertThat(capturedChannelEventData.first().activity).isEqualTo(ChannelEventEnum.Recording)
    }

    @Test
    fun `when stop event is received, should remove active user`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val startEvent = ChannelMemberActivityEvent(
            testChannel,
            testUser1,
            ChannelEventEnum.Typing,
            true
        )
        val stopEvent = ChannelMemberActivityEvent(
            testChannel,
            testUser1,
            ChannelEventEnum.Typing,
            false
        )

        // When
        helper.onActivityEvent(startEvent)
        advanceTimeBy(300) // Wait for debounce
        helper.onActivityEvent(stopEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then
        assertThat(capturedChannelEventData).isEmpty()
    }

    @Test
    fun `auto cancel should work after 5 seconds`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val typingEvent = ChannelMemberActivityEvent(
            testChannel,
            testUser1,
            ChannelEventEnum.Typing,
            true
        )

        // When
        helper.onActivityEvent(typingEvent)
        advanceTimeBy(300) // Wait for debounce - user should be added

        // Verify user is added
        assertThat(helper.channelEventData).hasSize(1)

        // Wait for auto-cancel (5 seconds + debounce)
        advanceTimeBy(5300)

        // Then - user should be auto-removed
        assertThat(helper.channelEventData).isEmpty()
    }

    @Test
    fun `getChannelEventState should return correct state for typing users`() {
        // Given
        val typingUsers = listOf(
            ChannelEventData(testUser1, ChannelEventEnum.Typing),
            ChannelEventData(testUser2, ChannelEventEnum.Recording)
        )

        // When
        val state = ChannelEventChangeHelper.getChannelEventState(typingUsers)

        // Then - Typing takes precedence over Recording
        assertThat(state).isEqualTo(ChannelEventState.Typing)
    }

    @Test
    fun `getChannelEventState should return recording when only recording users`() {
        // Given
        val recordingUsers = listOf(
            ChannelEventData(testUser1, ChannelEventEnum.Recording),
            ChannelEventData(testUser2, ChannelEventEnum.Recording)
        )

        // When
        val state = ChannelEventChangeHelper.getChannelEventState(recordingUsers)

        // Then
        assertThat(state).isEqualTo(ChannelEventState.Recording)
    }

    @Test
    fun `getChannelEventState should return none when no active users`() {
        // When
        val state = ChannelEventChangeHelper.getChannelEventState(emptyList())

        // Then
        assertThat(state).isEqualTo(ChannelEventState.None)
    }

    @Test
    fun `sequence mode should show single user immediately`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = true)
        val event = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Typing, true)

        // When
        helper.onActivityEvent(event)
        advanceTimeBy(300) // Wait for debounce

        // Then - Should show immediately, not cycle
        assertThat(capturedChannelEventData).hasSize(1)
        assertThat(capturedChannelEventData.first().user.id).isEqualTo(testUser1.id)
    }

    @Test
    fun `sequence mode should cycle through multiple users`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = true)
        val event1 = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Typing, true)
        val event2 = ChannelMemberActivityEvent(testChannel, testUser2, ChannelEventEnum.Recording, true)

        var callbackCount = 0
        val callbackHelper = createHelper(showActiveUsersInSequence = true) { users ->
            capturedChannelEventData = users
            callbackCount++
        }

        // When - Add two users
        callbackHelper.onActivityEvent(event1)
        callbackHelper.onActivityEvent(event2)
        advanceTimeBy(300) // Wait for debounce

        // Verify both users are active
        assertThat(callbackHelper.channelEventData).hasSize(2)

        // Should start cycling through users
        val initialCallbackCount = callbackCount
        advanceTimeBy(2000) // First 2 seconds
        assertThat(callbackCount > initialCallbackCount).isTrue()
    }

    @Test
    fun `equal active users should be based on user id`() {
        // Given
        val user1Typing = ChannelEventData(testUser1, ChannelEventEnum.Typing)
        val user1Recording = ChannelEventData(testUser1, ChannelEventEnum.Recording)
        val user2Typing = ChannelEventData(testUser2, ChannelEventEnum.Typing)

        // When & Then
        assertThat(user1Typing).isEqualTo(user1Recording) // Same user ID
        assertThat(user1Typing).isNotEqualTo(user2Typing) // Different user ID
        assertThat(user1Typing.hashCode()).isEqualTo(user1Recording.hashCode()) // Same hash
    }

    @Test
    fun `multiple events from same user should update activity state`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val typingEvent = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Typing, true)
        val recordingEvent = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Recording, true)

        // When
        helper.onActivityEvent(typingEvent)
        advanceTimeBy(300)

        // Verify typing state
        assertThat(helper.channelEventData).hasSize(1)
        assertThat(helper.channelEventData.first().activity).isEqualTo(ChannelEventEnum.Typing)

        helper.onActivityEvent(recordingEvent)
        advanceTimeBy(300)

        // Then - Should update to recording (same user, different activity)
        assertThat(helper.channelEventData).hasSize(1)
        assertThat(helper.channelEventData.first().activity).isEqualTo(ChannelEventEnum.Recording)
    }

    @Test
    fun `should handle multiple users simultaneously`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val event1 = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Typing, true)
        val event2 = ChannelMemberActivityEvent(testChannel, testUser2, ChannelEventEnum.Recording, true)

        // When
        helper.onActivityEvent(event1)
        helper.onActivityEvent(event2)
        advanceTimeBy(300) // Wait for debounce

        // Then
        assertThat(helper.channelEventData).hasSize(2)
        assertThat(helper.haveUserAction).isTrue()

        val userIds = helper.channelEventData.map { it.user.id }
        assertThat(userIds).containsExactly(testUser1.id, testUser2.id)
    }

    @Test
    fun `should handle rapid start stop events correctly`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val startEvent = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Typing, true)
        val stopEvent = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Typing, false)

        // When - Rapid fire events
        helper.onActivityEvent(startEvent)
        helper.onActivityEvent(stopEvent)
        helper.onActivityEvent(startEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then - Should end up with user active
        assertThat(helper.channelEventData).hasSize(1)
        assertThat(helper.channelEventData.first().user.id).isEqualTo(testUser1.id)
    }

    @Test
    fun `when same user switches between typing and recording, should update preview correctly`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        var callbackInvocations = 0
        val callbackHelper = createHelper(showActiveUsersInSequence = false) { users ->
            capturedChannelEventData = users
            callbackInvocations++
        }

        // When - User starts typing
        val typingEvent = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Typing, true)
        callbackHelper.onActivityEvent(typingEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then - Should show typing
        assertThat(capturedChannelEventData).hasSize(1)
        assertThat(capturedChannelEventData.first().activity).isEqualTo(ChannelEventEnum.Typing)
        val typingCallbacks = callbackInvocations

        // When - Same user switches to recording
        val recordingEvent = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Recording, true)
        callbackHelper.onActivityEvent(recordingEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then - Should show recording and trigger callback
        assertThat(capturedChannelEventData).hasSize(1)
        assertThat(capturedChannelEventData.first().activity).isEqualTo(ChannelEventEnum.Recording)
        assertThat(capturedChannelEventData.first().user.id).isEqualTo(testUser1.id)
        assertThat(callbackInvocations).isGreaterThan(typingCallbacks)

        // When - User switches back to typing
        val typingEvent2 = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Typing, true)
        callbackHelper.onActivityEvent(typingEvent2)
        advanceTimeBy(300) // Wait for debounce

        // Then - Should show typing again
        assertThat(capturedChannelEventData).hasSize(1)
        assertThat(capturedChannelEventData.first().activity).isEqualTo(ChannelEventEnum.Typing)
        assertThat(capturedChannelEventData.first().user.id).isEqualTo(testUser1.id)
    }

    @Test
    fun `when same user rapidly switches activity states, should handle debouncing correctly`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val typingEvent = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Typing, true)
        val recordingEvent = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Recording, true)

        // When - Rapid activity switches without waiting for debounce
        helper.onActivityEvent(typingEvent)
        helper.onActivityEvent(recordingEvent)
        helper.onActivityEvent(typingEvent)
        helper.onActivityEvent(recordingEvent)

        // Then - Before debounce, should not update callback yet
        assertThat(capturedChannelEventData).isEmpty()

        // When - Wait for debounce
        advanceTimeBy(300)

        // Then - Should show final state (recording)
        assertThat(capturedChannelEventData).hasSize(1)
        assertThat(capturedChannelEventData.first().activity).isEqualTo(ChannelEventEnum.Recording)
        assertThat(capturedChannelEventData.first().user.id).isEqualTo(testUser1.id)
    }

    @Test
    fun `auto cancel should work properly and trigger callback`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        var callbackInvocations = 0
        val callbackHelper = createHelper(showActiveUsersInSequence = false) { users ->
            capturedChannelEventData = users
            callbackInvocations++
        }

        val typingEvent = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Typing, true)

        // When - Start typing
        callbackHelper.onActivityEvent(typingEvent)
        advanceTimeBy(300) // Wait for debounce - user should be added

        // Then - Verify user is added and callback was invoked
        assertThat(capturedChannelEventData).hasSize(1)
        assertThat(capturedChannelEventData.first().user.id).isEqualTo(testUser1.id)
        val initialCallbacks = callbackInvocations

        // When - Wait for auto-cancel (5 seconds + debounce)
        advanceTimeBy(5300)

        // Then - User should be auto-removed and callback should be invoked
        assertThat(capturedChannelEventData).isEmpty()
        assertThat(callbackInvocations).isGreaterThan(initialCallbacks)
        assertThat(callbackHelper.channelEventData).isEmpty()
        assertThat(callbackHelper.haveUserAction).isFalse()
    }

    @Test
    fun `auto cancel should reset when user activity is renewed`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val typingEvent = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Typing, true)

        // When - Start typing
        helper.onActivityEvent(typingEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then - Verify user is added
        assertThat(helper.channelEventData).hasSize(1)

        // When - Wait 3 seconds (before auto-cancel)
        advanceTimeBy(3000)

        // Then - User should still be active
        assertThat(helper.channelEventData).hasSize(1)

        // When - User types again (renews activity)
        helper.onActivityEvent(typingEvent)
        advanceTimeBy(300)

        // Then - User should still be active
        assertThat(helper.channelEventData).hasSize(1)

        // When - Wait another 3 seconds (total 6+ seconds from first event, but only 3 from renewal)
        advanceTimeBy(3000)

        // Then - User should still be active (auto-cancel timer was reset)
        assertThat(helper.channelEventData).hasSize(1)

        // When - Wait for full auto-cancel period from renewal
        advanceTimeBy(2300) // 5.3 seconds from renewal

        // Then - Now user should be auto-removed
        assertThat(helper.channelEventData).isEmpty()
    }

    @Test
    fun `auto cancel should work independently for multiple users`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val typingEvent1 = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Typing, true)
        val recordingEvent2 = ChannelMemberActivityEvent(testChannel, testUser2, ChannelEventEnum.Recording, true)

        // When - Start both users' activities
        helper.onActivityEvent(typingEvent1)
        advanceTimeBy(300)

        // Then - First user should be active
        assertThat(helper.channelEventData).hasSize(1)

        // When - Add second user 2 seconds later
        advanceTimeBy(2000)
        helper.onActivityEvent(recordingEvent2)
        advanceTimeBy(300)

        // Then - Both users should be active
        assertThat(helper.channelEventData).hasSize(2)

        // When - Wait 3.5 more seconds (total 5.8 seconds for user1, 3.8 for user2)
        advanceTimeBy(3500)

        // Then - First user should be auto-cancelled, second should remain
        assertThat(helper.channelEventData).hasSize(1)
        assertThat(helper.channelEventData.first().user.id).isEqualTo(testUser2.id)

        // When - Wait 2 more seconds (total 5.8 seconds for user2)
        advanceTimeBy(2000)

        // Then - Second user should also be auto-cancelled
        assertThat(helper.channelEventData).isEmpty()
    }

    @Test
    fun `multiple activity state changes should preserve user count correctly`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val typingEvent = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Typing, true)
        val recordingEvent = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Recording, true)

        // When - User starts typing
        helper.onActivityEvent(typingEvent)
        advanceTimeBy(300)

        // Then - Should have 1 active user
        assertThat(helper.channelEventData).hasSize(1)
        assertThat(helper.haveUserAction).isTrue()

        // When - Same user switches to recording
        helper.onActivityEvent(recordingEvent)
        advanceTimeBy(300)

        // Then - Should still have 1 active user (same user, different activity)
        assertThat(helper.channelEventData).hasSize(1)
        assertThat(helper.channelEventData.first().activity).isEqualTo(ChannelEventEnum.Recording)
        assertThat(helper.haveUserAction).isTrue()

        // When - User stops recording
        val stopRecordingEvent = ChannelMemberActivityEvent(testChannel, testUser1, ChannelEventEnum.Recording, false)
        helper.onActivityEvent(stopRecordingEvent)
        advanceTimeBy(300)

        // Then - Should have no active users
        assertThat(helper.channelEventData).isEmpty()
        assertThat(helper.haveUserAction).isFalse()
    }

    private fun createHelper(
            showActiveUsersInSequence: Boolean,
            callback: (List<ChannelEventData>) -> Unit = activeUsersCallback,
    ): ChannelEventChangeHelper {
        return ChannelEventChangeHelper(
            scope = scope,
            activeUsersUpdated = callback,
            showChannelEventsInSequence = showActiveUsersInSequence
        )
    }
} 