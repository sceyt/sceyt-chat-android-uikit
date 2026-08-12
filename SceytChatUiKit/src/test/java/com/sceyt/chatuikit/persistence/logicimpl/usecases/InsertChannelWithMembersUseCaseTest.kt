package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.role.Role
import com.sceyt.chatuikit.createChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLinkEntity
import com.sceyt.chatuikit.koin.SceytKoinApp
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.logger.SceytLogLevel
import com.sceyt.chatuikit.logger.SceytLoggerImpl
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

class InsertChannelWithMembersUseCaseTest {

    private companion object {
        const val CHANNEL_ID = 11L
    }

    private val channelDao = mock<ChannelDao>()
    private val usersDao = mock<UserDao>()
    private val messageLogic = mock<PersistenceMessagesLogic>()
    private val useCase = InsertChannelWithMembersUseCase(channelDao = channelDao, usersDao = usersDao)

    @Before
    fun setUp() {
        stopKoin()
        SceytLog.setLogger(SceytLogLevel.None) { _, _, _, _ -> }
        SceytKoinApp.koinApp = startKoin { modules(module { single { messageLogic } }) }
    }

    @After
    fun tearDown() {
        SceytKoinApp.koinApp = null
        stopKoin()
        SceytLog.setLogger(SceytLogLevel.Verbose, SceytLoggerImpl())
    }

    @Test
    fun `inserts the channel with a link row per member`() = runTest {
        val members = listOf(member("peer-1", "member"), member("peer-2", "owner"))
        val channel = createChannel(id = CHANNEL_ID, pinnedAt = 0, createdAt = 1).copy(members = members)

        useCase(channel)

        verifyBlocking(usersDao) {
            insertUsersWithMetadata(
                check { users -> assertThat(users.map { it.user.id }).containsExactly("peer-1", "peer-2") },
                any()
            )
        }
        verifyBlocking(channelDao) {
            insertChannelAndLinks(
                check { entity -> assertThat(entity.id).isEqualTo(CHANNEL_ID) },
                check { links ->
                    assertThat(links).containsExactly(
                        UserChatLinkEntity(userId = "peer-1", chatId = CHANNEL_ID, role = "member"),
                        UserChatLinkEntity(userId = "peer-2", chatId = CHANNEL_ID, role = "owner")
                    )
                }
            )
        }
    }

    @Test
    fun `explicit members override the ones carried by the channel`() = runTest {
        val channel = createChannel(id = CHANNEL_ID, pinnedAt = 0, createdAt = 1)
            .copy(members = listOf(member("ignored", "member")))

        useCase(channel, listOf(member("explicit", "owner")))

        verifyBlocking(channelDao) {
            insertChannelAndLinks(
                any(),
                check { links -> assertThat(links.map { it.userId }).containsExactly("explicit") }
            )
        }
    }

    @Test
    fun `still inserts the channel when it has no members`() = runTest {
        val channel = createChannel(id = CHANNEL_ID, pinnedAt = 0, createdAt = 1).copy(members = emptyList())

        useCase(channel)

        verifyBlocking(usersDao) { insertUsersWithMetadata(eq(emptyList()), any()) }
        verifyBlocking(channelDao) {
            insertChannelAndLinks(any(), check { links -> assertThat(links).isEmpty() })
        }
    }

    @Test
    fun `saves the last message and its reaction authors alongside the channel`() = runTest {
        val reactionAuthor = SceytUser("reaction-author")
        val reaction = mock<SceytReaction>()
        whenever(reaction.user).thenReturn(reactionAuthor)
        val lastMessage = mock<SceytMessage>()
        whenever(lastMessage.userReactions).thenReturn(listOf(reaction))
        val channel = createChannel(id = CHANNEL_ID, pinnedAt = 0, createdAt = 1, lastMessage = lastMessage)
            .copy(members = listOf(member("peer-1", "member")))

        useCase(channel)

        verifyBlocking(messageLogic) { saveChannelLastMessagesToDb(eq(listOf(lastMessage))) }
        verifyBlocking(usersDao) {
            insertUsersWithMetadata(
                check { users ->
                    assertThat(users.map { it.user.id }).containsExactly("peer-1", "reaction-author")
                },
                any()
            )
        }
    }

    @Test
    fun `does not touch the message logic when the channel has no last message`() = runTest {
        val channel = createChannel(id = CHANNEL_ID, pinnedAt = 0, createdAt = 1)
            .copy(members = listOf(member("peer-1", "member")))

        useCase(channel)

        verifyBlocking(messageLogic, never()) { saveChannelLastMessagesToDb(any()) }
    }

    private fun member(id: String, role: String) = SceytMember(
        role = Role(role),
        user = SceytUser(id)
    )
}