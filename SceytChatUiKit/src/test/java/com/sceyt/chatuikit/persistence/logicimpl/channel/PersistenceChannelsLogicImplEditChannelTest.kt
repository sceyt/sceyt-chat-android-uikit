package com.sceyt.chatuikit.persistence.logicimpl.channel

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelAvatar
import com.sceyt.chatuikit.data.models.channels.EditChannelData
import com.sceyt.chatuikit.persistence.repositories.ChannelsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

class PersistenceChannelsLogicImplEditChannelTest {

    private companion object {
        const val CHANNEL_ID = 1L
        const val LOCAL_AVATAR_PATH = "/data/avatar.png"
        const val REMOTE_AVATAR_URL = "avatars/remote.png"
    }

    private val channelsRepository = mock<ChannelsRepository>()

    private val logic = PersistenceChannelsLogicImpl(
        context = mock(),
        channelsRepository = channelsRepository,
        channelDao = mock(),
        globalSearchDao = mock(),
        usersDao = mock(),
        messageDao = mock(),
        rangeDao = mock(),
        draftMessageDao = mock(),
        chatUserReactionDao = mock(),
        pendingReactionDao = mock(),
        channelsCache = mock(),
        channelSyncStateStore = mock(),
        pendingChannelCoordinator = mock(),
        insertChannelWithMembersUseCase = mock(),
    )

    private fun editData(avatar: ChannelAvatar?) = EditChannelData(
        newSubject = "subject",
        metadata = null,
        avatar = avatar,
        channelUri = null,
        channelType = "group",
    )

    @Test
    fun `uploads a local avatar and edits the channel with the uploaded url`() = runTest {
        whenever(channelsRepository.uploadAvatar(LOCAL_AVATAR_PATH))
            .thenReturn(SceytResponse.Success(REMOTE_AVATAR_URL))
        whenever(channelsRepository.editChannel(any(), any()))
            .thenReturn(SceytResponse.Error(SceytException(0, "not persisted")))

        logic.editChannel(CHANNEL_ID, editData(ChannelAvatar.Local(LOCAL_AVATAR_PATH)))

        verifyBlocking(channelsRepository) {
            editChannel(
                eq(CHANNEL_ID),
                check { assertThat(it.avatar).isEqualTo(ChannelAvatar.Remote(REMOTE_AVATAR_URL)) }
            )
        }
    }

    @Test
    fun `edits the channel with a remote avatar without uploading`() = runTest {
        val data = editData(ChannelAvatar.Remote(REMOTE_AVATAR_URL))
        whenever(channelsRepository.editChannel(any(), any()))
            .thenReturn(SceytResponse.Error(SceytException(0, "not persisted")))

        logic.editChannel(CHANNEL_ID, data)

        verifyBlocking(channelsRepository, never()) { uploadAvatar(any()) }
        verifyBlocking(channelsRepository) { editChannel(CHANNEL_ID, data) }
    }

    @Test
    fun `returns the avatar upload error without editing the channel`() = runTest {
        val exception = SceytException(5, "upload failed")
        whenever(channelsRepository.uploadAvatar(LOCAL_AVATAR_PATH))
            .thenReturn(SceytResponse.Error(exception))

        val response = logic.editChannel(CHANNEL_ID, editData(ChannelAvatar.Local(LOCAL_AVATAR_PATH)))

        assertThat((response as SceytResponse.Error).exception).isEqualTo(exception)
        verifyBlocking(channelsRepository, never()) { editChannel(any(), any()) }
    }
}
