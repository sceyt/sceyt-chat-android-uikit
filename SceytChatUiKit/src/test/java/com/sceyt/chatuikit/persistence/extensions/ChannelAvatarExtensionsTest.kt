package com.sceyt.chatuikit.persistence.extensions

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelAvatar
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ChannelAvatarExtensionsTest {

    private companion object {
        const val LOCAL_AVATAR_PATH = "/data/avatar.png"
        const val REMOTE_AVATAR_URL = "avatars/remote.png"
    }

    private val uploadedPaths = mutableListOf<String>()

    private fun uploadReturning(response: SceytResponse<String>): suspend (String) -> SceytResponse<String> =
        { path ->
            uploadedPaths += path
            response
        }

    @Test
    fun `toAvatarUrl returns the file path of a local avatar`() {
        assertThat(ChannelAvatar.Local(LOCAL_AVATAR_PATH).toAvatarUrl()).isEqualTo(LOCAL_AVATAR_PATH)
    }

    @Test
    fun `toAvatarUrl returns the url of a remote avatar`() {
        assertThat(ChannelAvatar.Remote(REMOTE_AVATAR_URL).toAvatarUrl()).isEqualTo(REMOTE_AVATAR_URL)
    }

    @Test
    fun `toAvatarUrl returns an empty string when there is no avatar`() {
        val avatar: ChannelAvatar? = null

        assertThat(avatar.toAvatarUrl()).isEmpty()
    }

    @Test
    fun `uploadIfLocal uploads a local avatar and returns it as remote`() = runTest {
        val response = ChannelAvatar.Local(LOCAL_AVATAR_PATH)
            .uploadIfLocal(uploadReturning(SceytResponse.Success(REMOTE_AVATAR_URL)))

        assertThat(response.data).isEqualTo(ChannelAvatar.Remote(REMOTE_AVATAR_URL))
        assertThat(uploadedPaths).containsExactly(LOCAL_AVATAR_PATH)
    }

    @Test
    fun `uploadIfLocal returns no avatar when the upload succeeds without url`() = runTest {
        val response = ChannelAvatar.Local(LOCAL_AVATAR_PATH)
            .uploadIfLocal(uploadReturning(SceytResponse.Success(null)))

        assertThat(response).isInstanceOf(SceytResponse.Success::class.java)
        assertThat(response.data).isNull()
    }

    @Test
    fun `uploadIfLocal returns the upload error`() = runTest {
        val exception = SceytException(5, "upload failed")

        val response = ChannelAvatar.Local(LOCAL_AVATAR_PATH)
            .uploadIfLocal(uploadReturning(SceytResponse.Error(exception)))

        assertThat((response as SceytResponse.Error).exception).isEqualTo(exception)
    }

    @Test
    fun `uploadIfLocal keeps a remote avatar without uploading`() = runTest {
        val avatar = ChannelAvatar.Remote(REMOTE_AVATAR_URL)

        val response = avatar.uploadIfLocal(uploadReturning(SceytResponse.Success("unused")))

        assertThat(response.data).isEqualTo(avatar)
        assertThat(uploadedPaths).isEmpty()
    }

    @Test
    fun `uploadIfLocal keeps no avatar without uploading`() = runTest {
        val avatar: ChannelAvatar? = null

        val response = avatar.uploadIfLocal(uploadReturning(SceytResponse.Success("unused")))

        assertThat(response).isInstanceOf(SceytResponse.Success::class.java)
        assertThat(response.data).isNull()
        assertThat(uploadedPaths).isEmpty()
    }
}
