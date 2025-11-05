package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.persistence.database.dao.PendingPollVoteDao
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage

internal class HandleChangeVoteErrorUseCase(
    private val pendingPollVoteDao: PendingPollVoteDao,
    private val messageDao: com.sceyt.chatuikit.persistence.database.dao.MessageDao,
    private val messagesCache: com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
) {

    suspend operator fun invoke(
        messageId: Long,
        pollId: String,
        optionIds: List<String>,
        errorCode: Int?
    ) {
        if (errorCode == 1301) {
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
        }
    }
}