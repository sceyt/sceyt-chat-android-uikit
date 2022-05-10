package com.sceyt.chat.ui.data.models.messages

import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.message.*
import com.sceyt.chat.models.user.User
import java.util.*

open class SceytUiMessage(var id: Long,
                          var tid: Long,
                          var channelId: Long,
                          var to: String,
                          var body: String,
                          var type: String,
                          var metadata: String,
                          var createdAt: Long,
                          var updatedAt: Date,
                          var incoming: Boolean = false,
                          var receipt: Boolean = false,
                          var isTransient: Boolean = false,
                          var silent: Boolean = false,
                          var deliveryStatus: DeliveryStatus,
                          var state: MessageState,
                          var from: User,
                          var attachments: Array<Attachment>? = null,
                          var lastReactions: Array<Reaction>? = null,
                          var selfReactions: Array<Reaction>? = null,
                          var reactionScores: Array<ReactionScore>? = null,
                          var markerCount: Array<MarkerCount>? = null,
                          var selfMarkers: Array<String>? = null,
                          var mentionedUsers: Array<User?>,
                          var parent: Message?,
                          var replyInThread: Boolean = false,
                          var replyCount: Long = 0) {

}
