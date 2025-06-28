package com.sceyt.chatuikit.presentation.components.channel.header.helpers

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.data.models.messages.SceytUser
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
        val typingEvent = ChannelMemberActivityEvent.Typing(
            testChannel,
            testUser1,
            true
        )

        // When
        helper.onActivityEvent(typingEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then
        assertThat(capturedActiveUsers).hasSize(1)
        assertThat(capturedActiveUsers.first().user.id).isEqualTo(testUser1.id)
        assertThat(capturedActiveUsers.first().activity).isEqualTo(UserActivityState.Typing)
    }

    @Test
    fun `when recording event is received, should add active user`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val recordingEvent = ChannelMemberActivityEvent.Recording(
            testChannel,
            testUser1,
            true
        )

        // When
        helper.onActivityEvent(recordingEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then
        assertThat(capturedActiveUsers).hasSize(1)
        assertThat(capturedActiveUsers.first().user.id).isEqualTo(testUser1.id)
        assertThat(capturedActiveUsers.first().activity).isEqualTo(UserActivityState.Recording)
    }

    @Test
    fun `when stop event is received, should remove active user`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val startEvent = ChannelMemberActivityEvent.Typing(
            testChannel,
            testUser1,
            true
        )
        val stopEvent = ChannelMemberActivityEvent.Typing(
            testChannel,
            testUser1,
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
        val typingEvent = ChannelMemberActivityEvent.Typing(
            testChannel,
            testUser1,
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
            ActiveUser(testUser1, UserActivityState.Typing),
            ActiveUser(testUser2, UserActivityState.Recording)
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
            ActiveUser(testUser1, UserActivityState.Recording),
            ActiveUser(testUser2, UserActivityState.Recording)
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
        val event = ChannelMemberActivityEvent.Typing(testChannel, testUser1, true)

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
        val event1 = ChannelMemberActivityEvent.Typing(testChannel, testUser1, true)
        val event2 = ChannelMemberActivityEvent.Recording(testChannel, testUser2, true)

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
        val user1Typing = ActiveUser(testUser1, UserActivityState.Typing)
        val user1Recording = ActiveUser(testUser1, UserActivityState.Recording)
        val user2Typing = ActiveUser(testUser2, UserActivityState.Typing)

        // When & Then
        assertThat(user1Typing).isEqualTo(user1Recording) // Same user ID
        assertThat(user1Typing).isNotEqualTo(user2Typing) // Different user ID
        assertThat(user1Typing.hashCode()).isEqualTo(user1Recording.hashCode()) // Same hash
    }

    @Test
    fun `multiple events from same user should update activity state`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val typingEvent = ChannelMemberActivityEvent.Typing(testChannel, testUser1, true)
        val recordingEvent = ChannelMemberActivityEvent.Recording(testChannel, testUser1, true)

        // When
        helper.onActivityEvent(typingEvent)
        advanceTimeBy(300)

        // Verify typing state
        assertThat(helper.activeUsers).hasSize(1)
        assertThat(helper.activeUsers.first().activity).isEqualTo(UserActivityState.Typing)

        helper.onActivityEvent(recordingEvent)
        advanceTimeBy(300)

        // Then - Should update to recording (same user, different activity)
        assertThat(helper.activeUsers).hasSize(1)
        assertThat(helper.activeUsers.first().activity).isEqualTo(UserActivityState.Recording)
    }

    @Test
    fun `should handle multiple users simultaneously`() = scope.runTest {
        // Given
        helper = createHelper(showActiveUsersInSequence = false)
        val event1 = ChannelMemberActivityEvent.Typing(testChannel, testUser1, true)
        val event2 = ChannelMemberActivityEvent.Recording(testChannel, testUser2, true)

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
        val startEvent = ChannelMemberActivityEvent.Typing(testChannel, testUser1, true)
        val stopEvent = ChannelMemberActivityEvent.Typing(testChannel, testUser1, false)

        // When - Rapid fire events
        helper.onActivityEvent(startEvent)
        helper.onActivityEvent(stopEvent)
        helper.onActivityEvent(startEvent)
        advanceTimeBy(300) // Wait for debounce

        // Then - Should end up with user active
        assertThat(helper.activeUsers).hasSize(1)
        assertThat(helper.activeUsers.first().user.id).isEqualTo(testUser1.id)
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