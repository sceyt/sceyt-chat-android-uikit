package com.sceyt.chatuikit.data.repositories

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.CreateChannelInviteKeyRequest
import com.sceyt.chat.models.channel.CreateChannelInviteKeyResponse
import com.sceyt.chat.models.channel.DeleteRevokedInviteKeyRequest
import com.sceyt.chat.models.channel.GetChannelByInviteKeyRequest
import com.sceyt.chat.models.channel.RegenerateChannelInviteKeyRequest
import com.sceyt.chat.models.channel.RevokeChannelInviteKeyRequest
import com.sceyt.chat.sceyt_callbacks.ActionCallback
import com.sceyt.chat.sceyt_callbacks.ChannelCallback
import com.sceyt.chat.sceyt_callbacks.InviteKeyCallback
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelInviteKeyData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.mappers.toChannelInviteKeyData
import com.sceyt.chatuikit.persistence.mappers.toSceytUiChannel
import com.sceyt.chatuikit.persistence.repositories.ChannelInviteKeyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class ChannelInviteKeyRepositoryImpl : ChannelInviteKeyRepository {

    override suspend fun getChannelByInviteKey(
            inviteKey: String,
    ): SceytResponse<SceytChannel> = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            GetChannelByInviteKeyRequest(inviteKey).execute(object : ChannelCallback {
                override fun onResult(channel: Channel) {
                    continuation.safeResume(SceytResponse.Success(channel.toSceytUiChannel()))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "getChannelByInviteKey error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun getChannelInviteKeys(
            channelId: Long,
    ): SceytResponse<List<ChannelInviteKeyData>> = withContext(Dispatchers.IO) {
        TODO("Not yet implemented")
    }

    override suspend fun createChannelInviteKey(
            channelId: Long,
            expireAt: Long,
            maxUses: Int,
            accessPriorHistory: Boolean,
    ): SceytResponse<ChannelInviteKeyData> = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            CreateChannelInviteKeyRequest.Builder(channelId)
                .setMaxUses(maxUses)
                .setExpiresAt(expireAt)
                .setAccessPriorHistory(accessPriorHistory)
                .build().execute(object : InviteKeyCallback {
                    override fun onResult(response: CreateChannelInviteKeyResponse) {
                        val data = response.inviteKey.toChannelInviteKeyData()
                        continuation.safeResume(SceytResponse.Success(data))
                    }

                    override fun onError(e: SceytException?) {
                        continuation.safeResume(SceytResponse.Error(e))
                        SceytLog.e(TAG, "createChannelInviteLink error: ${e?.message}, code: ${e?.code}")
                    }
                })
        }
    }

    override suspend fun regenerateChannelInviteKey(
            channelId: Long,
            key: String,
    ): SceytResponse<ChannelInviteKeyData> = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            RegenerateChannelInviteKeyRequest(
                channelId, key
            ).execute(object : InviteKeyCallback {
                override fun onResult(response: CreateChannelInviteKeyResponse) {
                    val data = response.inviteKey.toChannelInviteKeyData()
                    continuation.safeResume(SceytResponse.Success(data))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "regenerateChannelInviteKey error: ${e?.message}, code: ${e?.code}")
                }
            }
            )
        }
    }

    override suspend fun revokeChannelInviteKeys(
            channelId: Long,
            keys: List<String>,
    ): SceytResponse<Boolean> = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            RevokeChannelInviteKeyRequest(
                channelId, keys.toTypedArray()
            ).execute(object : ActionCallback {
                override fun onSuccess() {
                    continuation.safeResume(SceytResponse.Success(true))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "revokeChannelInviteKeys error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }

    override suspend fun deleteRevokedChannelInviteKeys(
            channelId: Long,
            keys: List<String>,
    ): SceytResponse<Boolean> = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            DeleteRevokedInviteKeyRequest(
                channelId, keys.toTypedArray()
            ).execute(object : ActionCallback {
                override fun onSuccess() {
                    continuation.safeResume(SceytResponse.Success(true))
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "deleteRevokedChannelInviteKeys error: ${e?.message}, code: ${e?.code}")
                }
            })
        }
    }
}