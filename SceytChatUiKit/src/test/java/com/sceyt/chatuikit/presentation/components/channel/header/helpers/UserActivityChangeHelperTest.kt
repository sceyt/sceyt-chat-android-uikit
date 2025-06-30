package com.sceyt.chatuikit.presentation.components.channel.header.helpers

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.components.channel.input.data.UserActivity
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
class UserActivityChangeHelperTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var helper: UserActivityChangeHelper
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var scope: TestScope

    private val testUser1 = SceytUser("user1")
    private val testUser2 = SceytUser("user2")
    private val testChannel = createChannel(1, 0, 1)

    private var capturedActiveUsers: List<ActiveUser> = emptyList()
    private val activeUsersCallback: (List<ActiveUser>) -> Unit = { users ->
        capturedActiveUsers = users
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
            UserActivity.Typing,
            true
        )

        // When
        helper.onActivityEvent(typingEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then
        assertThat(capturedActiveUsers).hasSize(1)
        assertThat(capturedActiveUsers.first().user.id).isEqualTo(testUser1.id)
        assertThat(capturedActiveUsers.first().activity).isEqualTo(UserActivity.Typing)
    }

    @Test
    fun `when recording event is received, should add active user`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val recordingEvent = ChannelMemberActivityEvent(
            testChannel,
            testUser1,
            UserActivity.Recording,
            true
        )

        // When
        helper.onActivityEvent(recordingEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then
        assertThat(capturedActiveUsers).hasSize(1)
        assertThat(capturedActiveUsers.first().user.id).isEqualTo(testUser1.id)
        assertThat(capturedActiveUsers.first().activity).isEqualTo(UserActivity.Recording)
    }

    @Test
    fun `when stop event is received, should remove active user`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val startEvent = ChannelMemberActivityEvent(
            testChannel,
            testUser1,
            UserActivity.Typing,
            true
        )
        val stopEvent = ChannelMemberActivityEvent(
            testChannel,
            testUser1,
            UserActivity.Typing,
            false
        )

        // When
        helper.onActivityEvent(startEvent)
        advanceTimeBy(300) // Wait for debounce
        helper.onActivityEvent(stopEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then
        assertThat(capturedActiveUsers).isEmpty()
    }

    @Test
    fun `auto cancel should work after 5 seconds`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val typingEvent = ChannelMemberActivityEvent(
            testChannel,
            testUser1,
            UserActivity.Typing,
            true
        )

        // When
        helper.onActivityEvent(typingEvent)
        advanceTimeBy(300) // Wait for debounce - user should be added

        // Verify user is added
        assertThat(helper.activeUsers).hasSize(1)

        // Wait for auto-cancel (5 seconds + debounce)
        advanceTimeBy(5300)

        // Then - user should be auto-removed
        assertThat(helper.activeUsers).isEmpty()
    }

    @Test
    fun `getActivityState should return correct state for typing users`() {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val typingUsers = listOf(
            ActiveUser(testUser1, UserActivity.Typing),
            ActiveUser(testUser2, UserActivity.Recording)
        )

        // When
        val state = helper.getActivityState(typingUsers)

        // Then - Typing takes precedence over Recording
        assertThat(state).isEqualTo(UsersActivityState.Typing)
    }

    @Test
    fun `getActivityState should return recording when only recording users`() {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val recordingUsers = listOf(
            ActiveUser(testUser1, UserActivity.Recording),
            ActiveUser(testUser2, UserActivity.Recording)
        )

        // When
        val state = helper.getActivityState(recordingUsers)

        // Then
        assertThat(state).isEqualTo(UsersActivityState.Recording)
    }

    @Test
    fun `getActivityState should return none when no active users`() {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)

        // When
        val state = helper.getActivityState(emptyList())

        // Then
        assertThat(state).isEqualTo(UsersActivityState.None)
    }

    @Test
    fun `sequence mode should show single user immediately`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = true)
        val event = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Typing, true)

        // When
        helper.onActivityEvent(event)
        advanceTimeBy(300) // Wait for debounce

        // Then - Should show immediately, not cycle
        assertThat(capturedActiveUsers).hasSize(1)
        assertThat(capturedActiveUsers.first().user.id).isEqualTo(testUser1.id)
    }

    @Test
    fun `sequence mode should cycle through multiple users`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = true)
        val event1 = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Typing, true)
        val event2 = ChannelMemberActivityEvent(testChannel, testUser2, UserActivity.Recording, true)

        var callbackCount = 0
        val callbackHelper = createHelper(showActiveUsersInSequence = true) { users ->
            capturedActiveUsers = users
            callbackCount++
        }

        // When - Add two users
        callbackHelper.onActivityEvent(event1)
        callbackHelper.onActivityEvent(event2)
        advanceTimeBy(300) // Wait for debounce

        // Verify both users are active
        assertThat(callbackHelper.activeUsers).hasSize(2)

        // Should start cycling through users
        val initialCallbackCount = callbackCount
        advanceTimeBy(2000) // First 2 seconds
        assertThat(callbackCount > initialCallbackCount).isTrue()
    }

    @Test
    fun `equal active users should be based on user id`() {
        // Given
        val user1Typing = ActiveUser(testUser1, UserActivity.Typing)
        val user1Recording = ActiveUser(testUser1, UserActivity.Recording)
        val user2Typing = ActiveUser(testUser2, UserActivity.Typing)

        // When & Then
        assertThat(user1Typing).isEqualTo(user1Recording) // Same user ID
        assertThat(user1Typing).isNotEqualTo(user2Typing) // Different user ID
        assertThat(user1Typing.hashCode()).isEqualTo(user1Recording.hashCode()) // Same hash
    }

    @Test
    fun `multiple events from same user should update activity state`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val typingEvent = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Typing, true)
        val recordingEvent = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Recording, true)

        // When
        helper.onActivityEvent(typingEvent)
        advanceTimeBy(300)

        // Verify typing state
        assertThat(helper.activeUsers).hasSize(1)
        assertThat(helper.activeUsers.first().activity).isEqualTo(UserActivity.Typing)

        helper.onActivityEvent(recordingEvent)
        advanceTimeBy(300)

        // Then - Should update to recording (same user, different activity)
        assertThat(helper.activeUsers).hasSize(1)
        assertThat(helper.activeUsers.first().activity).isEqualTo(UserActivity.Recording)
    }

    @Test
    fun `should handle multiple users simultaneously`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val event1 = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Typing, true)
        val event2 = ChannelMemberActivityEvent(testChannel, testUser2, UserActivity.Recording, true)

        // When
        helper.onActivityEvent(event1)
        helper.onActivityEvent(event2)
        advanceTimeBy(300) // Wait for debounce

        // Then
        assertThat(helper.activeUsers).hasSize(2)
        assertThat(helper.haveUserAction).isTrue()

        val userIds = helper.activeUsers.map { it.user.id }
        assertThat(userIds).containsExactly(testUser1.id, testUser2.id)
    }

    @Test
    fun `should handle rapid start stop events correctly`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val startEvent = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Typing, true)
        val stopEvent = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Typing, false)

        // When - Rapid fire events
        helper.onActivityEvent(startEvent)
        helper.onActivityEvent(stopEvent)
        helper.onActivityEvent(startEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then - Should end up with user active
        assertThat(helper.activeUsers).hasSize(1)
        assertThat(helper.activeUsers.first().user.id).isEqualTo(testUser1.id)
    }

    @Test
    fun `when same user switches between typing and recording, should update preview correctly`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        var callbackInvocations = 0
        val callbackHelper = createHelper(showActiveUsersInSequence = false) { users ->
            capturedActiveUsers = users
            callbackInvocations++
        }

        // When - User starts typing
        val typingEvent = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Typing, true)
        callbackHelper.onActivityEvent(typingEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then - Should show typing
        assertThat(capturedActiveUsers).hasSize(1)
        assertThat(capturedActiveUsers.first().activity).isEqualTo(UserActivity.Typing)
        val typingCallbacks = callbackInvocations

        // When - Same user switches to recording
        val recordingEvent = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Recording, true)
        callbackHelper.onActivityEvent(recordingEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then - Should show recording and trigger callback
        assertThat(capturedActiveUsers).hasSize(1)
        assertThat(capturedActiveUsers.first().activity).isEqualTo(UserActivity.Recording)
        assertThat(capturedActiveUsers.first().user.id).isEqualTo(testUser1.id)
        assertThat(callbackInvocations).isGreaterThan(typingCallbacks)

        // When - User switches back to typing
        val typingEvent2 = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Typing, true)
        callbackHelper.onActivityEvent(typingEvent2)
        advanceTimeBy(300) // Wait for debounce

        // Then - Should show typing again
        assertThat(capturedActiveUsers).hasSize(1)
        assertThat(capturedActiveUsers.first().activity).isEqualTo(UserActivity.Typing)
        assertThat(capturedActiveUsers.first().user.id).isEqualTo(testUser1.id)
    }

    @Test
    fun `when same user rapidly switches activity states, should handle debouncing correctly`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val typingEvent = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Typing, true)
        val recordingEvent = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Recording, true)

        // When - Rapid activity switches without waiting for debounce
        helper.onActivityEvent(typingEvent)
        helper.onActivityEvent(recordingEvent)
        helper.onActivityEvent(typingEvent)
        helper.onActivityEvent(recordingEvent)

        // Then - Before debounce, should not update callback yet
        assertThat(capturedActiveUsers).isEmpty()

        // When - Wait for debounce
        advanceTimeBy(300)

        // Then - Should show final state (recording)
        assertThat(capturedActiveUsers).hasSize(1)
        assertThat(capturedActiveUsers.first().activity).isEqualTo(UserActivity.Recording)
        assertThat(capturedActiveUsers.first().user.id).isEqualTo(testUser1.id)
    }

    @Test
    fun `auto cancel should work properly and trigger callback`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        var callbackInvocations = 0
        val callbackHelper = createHelper(showActiveUsersInSequence = false) { users ->
            capturedActiveUsers = users
            callbackInvocations++
        }

        val typingEvent = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Typing, true)

        // When - Start typing
        callbackHelper.onActivityEvent(typingEvent)
        advanceTimeBy(300) // Wait for debounce - user should be added

        // Then - Verify user is added and callback was invoked
        assertThat(capturedActiveUsers).hasSize(1)
        assertThat(capturedActiveUsers.first().user.id).isEqualTo(testUser1.id)
        val initialCallbacks = callbackInvocations

        // When - Wait for auto-cancel (5 seconds + debounce)
        advanceTimeBy(5300)

        // Then - User should be auto-removed and callback should be invoked
        assertThat(capturedActiveUsers).isEmpty()
        assertThat(callbackInvocations).isGreaterThan(initialCallbacks)
        assertThat(callbackHelper.activeUsers).isEmpty()
        assertThat(callbackHelper.haveUserAction).isFalse()
    }

    @Test
    fun `auto cancel should reset when user activity is renewed`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val typingEvent = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Typing, true)

        // When - Start typing
        helper.onActivityEvent(typingEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then - Verify user is added
        assertThat(helper.activeUsers).hasSize(1)

        // When - Wait 3 seconds (before auto-cancel)
        advanceTimeBy(3000)

        // Then - User should still be active
        assertThat(helper.activeUsers).hasSize(1)

        // When - User types again (renews activity)
        helper.onActivityEvent(typingEvent)
        advanceTimeBy(300)

        // Then - User should still be active
        assertThat(helper.activeUsers).hasSize(1)

        // When - Wait another 3 seconds (total 6+ seconds from first event, but only 3 from renewal)
        advanceTimeBy(3000)

        // Then - User should still be active (auto-cancel timer was reset)
        assertThat(helper.activeUsers).hasSize(1)

        // When - Wait for full auto-cancel period from renewal
        advanceTimeBy(2300) // 5.3 seconds from renewal

        // Then - Now user should be auto-removed
        assertThat(helper.activeUsers).isEmpty()
    }

    @Test
    fun `auto cancel should work independently for multiple users`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val typingEvent1 = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Typing, true)
        val recordingEvent2 = ChannelMemberActivityEvent(testChannel, testUser2, UserActivity.Recording, true)

        // When - Start both users' activities
        helper.onActivityEvent(typingEvent1)
        advanceTimeBy(300)

        // Then - First user should be active
        assertThat(helper.activeUsers).hasSize(1)

        // When - Add second user 2 seconds later
        advanceTimeBy(2000)
        helper.onActivityEvent(recordingEvent2)
        advanceTimeBy(300)

        // Then - Both users should be active
        assertThat(helper.activeUsers).hasSize(2)

        // When - Wait 3.5 more seconds (total 5.8 seconds for user1, 3.8 for user2)
        advanceTimeBy(3500)

        // Then - First user should be auto-cancelled, second should remain
        assertThat(helper.activeUsers).hasSize(1)
        assertThat(helper.activeUsers.first().user.id).isEqualTo(testUser2.id)

        // When - Wait 2 more seconds (total 5.8 seconds for user2)
        advanceTimeBy(2000)

        // Then - Second user should also be auto-cancelled
        assertThat(helper.activeUsers).isEmpty()
    }

    @Test
    fun `multiple activity state changes should preserve user count correctly`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val typingEvent = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Typing, true)
        val recordingEvent = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Recording, true)

        // When - User starts typing
        helper.onActivityEvent(typingEvent)
        advanceTimeBy(300)

        // Then - Should have 1 active user
        assertThat(helper.activeUsers).hasSize(1)
        assertThat(helper.haveUserAction).isTrue()

        // When - Same user switches to recording
        helper.onActivityEvent(recordingEvent)
        advanceTimeBy(300)

        // Then - Should still have 1 active user (same user, different activity)
        assertThat(helper.activeUsers).hasSize(1)
        assertThat(helper.activeUsers.first().activity).isEqualTo(UserActivity.Recording)
        assertThat(helper.haveUserAction).isTrue()

        // When - User stops recording
        val stopRecordingEvent = ChannelMemberActivityEvent(testChannel, testUser1, UserActivity.Recording, false)
        helper.onActivityEvent(stopRecordingEvent)
        advanceTimeBy(300)

        // Then - Should have no active users
        assertThat(helper.activeUsers).isEmpty()
        assertThat(helper.haveUserAction).isFalse()
    }

    private fun createHelper(
            showActiveUsersInSequence: Boolean,
            callback: (List<ActiveUser>) -> Unit = activeUsersCallback
    ): UserActivityChangeHelper {
        return UserActivityChangeHelper(
            scope = scope,
            activeUsersUpdated = callback,
            showActiveUsersInSequence = showActiveUsersInSequence
        )
    }
} 