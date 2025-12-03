package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.data.models.SDKErrorTypeEnum
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingPollVoteDao
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage

internal class HandleChangeVoteErrorUseCase(
    private val pendingPollVoteDao: PendingPollVoteDao,
    private val messageDao: MessageDao,
    private val messagesCache: MessagesCache
) {

    suspend operator fun invoke(
        messageId: Long,
        pollId: String,
        optionIds: List<String>,
        exception: SceytException?
    ) {
        val errorType = SDKErrorTypeEnum.fromValue(exception?.type) ?: return
        if (!errorType.isResendable) {
            val messageTid = messageDao.getMessageTidById(messageId) ?: return
            val count = pendingPollVoteDao.deleteVotesByOptionIds(
                messageTid = messageTid,
                pollId = pollId,
                optionIds = optionIds
            )

            if (count > 0) {
                messageDao.getMessageByTid(messageTid)?.let {
                    messagesCache.upsertMessages(
                        channelId = it.messageEntity.channelId,
                        it.toSceytMessage()
                    )
                }
            }
            SceytLog.e(
                "HandleChangeVoteErrorUseCase",
                "Delete pending votes for messageId: $messageId, pollId: $pollId, optionIds: $optionIds due to non-resendable error: ${exception?.type}"
            )
        }
    }
}