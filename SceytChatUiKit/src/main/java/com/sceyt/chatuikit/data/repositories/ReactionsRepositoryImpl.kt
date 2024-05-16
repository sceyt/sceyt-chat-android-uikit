package com.sceyt.chatuikit.data.repositories

import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.Reaction
import com.sceyt.chat.models.message.ReactionsListQuery
import com.sceyt.chat.operators.ChannelOperator
import com.sceyt.chat.sceyt_callbacks.MessageCallback
import com.sceyt.chat.sceyt_callbacks.ReactionsCallback
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.extensions.safeResume
import com.sceyt.chatuikit.persistence.mappers.toSceytReaction
import com.sceyt.chatuikit.persistence.mappers.toSceytUiMessage
import com.sceyt.chatuikit.persistence.repositories.ReactionsRepository
import kotlinx.coroutines.suspendCancellableCoroutine

class ReactionsRepositoryImpl : ReactionsRepository {
    private lateinit var reactionsQuery: ReactionsListQuery

    override suspend fun getReactions(messageId: Long, key: String): SceytResponse<List<SceytReaction>> {
        return suspendCancellableCoroutine { continuation ->
            val channelListQuery = createReactionsQuery(messageId, key).also { reactionsQuery = it }

            channelListQuery.loadNext(object : ReactionsCallback {
                override fun onResult(reactions: MutableList<Reaction>?) {
                    if (reactions.isNullOrEmpty())
                        continuation.safeResume(SceytResponse.Success(emptyList()))
                    else {
                        continuation.safeResume(SceytResponse.Success(reactions.map { it.toSceytReaction() }))
                    }
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "getReactions error: ${e?.message}")
                }
            })
        }
    }

    override suspend fun loadMoreReactions(messageId: Long, key: String): SceytResponse<List<SceytReaction>> {
        return suspendCancellableCoroutine { continuation ->
            val query = if (::reactionsQuery.isInitialized)
                reactionsQuery
            else createReactionsQuery(messageId, key).also { reactionsQuery = it }

            query.loadNext(object : ReactionsCallback {
                override fun onResult(reactions: MutableList<Reaction>?) {
                    if (reactions.isNullOrEmpty())
                        continuation.safeResume(SceytResponse.Success(emptyList()))
                    else {
                        continuation.safeResume(SceytResponse.Success(reactions.map { it.toSceytReaction() }))
                    }
                }

                override fun onError(e: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(e))
                    SceytLog.e(TAG, "loadMoreReactions error: ${e?.message}")
                }
            })
        }
    }

    private fun createReactionsQuery(messageId: Long, key: String? = null): ReactionsListQuery {
        return ReactionsListQuery.Builder(messageId)
            .withReactionKey(key)
            .setLimit(SceytChatUIKit.config.reactionsLoadSize)
            .build()
    }

    override suspend fun addReaction(channelId: Long, messageId: Long, key: String, score: Int): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).addReactionWithMessageId(messageId, key, score.toShort(), "", false, object : MessageCallback {
                override fun onResult(message: Message?) {
                    continuation.safeResume(SceytResponse.Success(message?.toSceytUiMessage()))
                }

                override fun onError(error: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(error))
                    SceytLog.e(TAG, "addReaction error: ${error?.message}")
                }
            })
        }
    }

    override suspend fun deleteReaction(channelId: Long, messageId: Long, scoreKey: String): SceytResponse<SceytMessage> {
        return suspendCancellableCoroutine { continuation ->
            ChannelOperator.build(channelId).deleteReactionWithMessageId(messageId, scoreKey, object : MessageCallback {
                override fun onResult(message: Message?) {
                    continuation.safeResume(SceytResponse.Success(message?.toSceytUiMessage()))
                }

                override fun onError(error: SceytException?) {
                    continuation.safeResume(SceytResponse.Error(error))
                    SceytLog.e(TAG, "deleteReaction error: ${error?.message}")
                }
            })
        }
    }
}