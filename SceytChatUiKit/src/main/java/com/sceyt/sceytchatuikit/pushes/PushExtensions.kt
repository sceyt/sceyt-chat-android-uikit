package com.sceyt.sceytchatuikit.pushes

import com.google.firebase.messaging.RemoteMessage
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.PrivateChannel
import com.sceyt.chat.models.channel.PublicChannel
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.Presence
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserActivityStatus
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.stringToEnum
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import org.json.JSONObject

fun getMessageBodyFromPushJson(remoteMessage: RemoteMessage, channelId: Long?, from: User?): Message? {
    return try {
        val messageJson = remoteMessage.data["message"]
        val messageJsonObject = JSONObject(messageJson ?: return null)

        // Do not getLong from json when its a string
        // double.parse corrupts the value
        val messageIdString = messageJsonObject.getString("id")
        val bodyString = messageJsonObject.getString("body")
        val messageType = messageJsonObject.getString("type")
        val meta = messageJsonObject.getString("metadata")
        val createdAtString = messageJsonObject.getString("created_at")
        val transient = messageJsonObject.getBoolean("transient")
        val createdAt = DateTimeUtil.convertStringToDate(createdAtString, DateTimeUtil.SERVER_DATE_PATTERN)

        val attachmentArray = ArrayList<Attachment>()
        val attachments = messageJsonObject.getJSONArray("attachments")
        for (i in 0 until attachments.length()) {
            when (val value: Any = attachments[i]) {
                is JSONObject -> {
                    getAttachmentFromPushJson(value)?.let { attachmentArray.add(it) }
                }
            }
        }

        val messageId = messageIdString.toLong()

        Message(messageId, messageId, channelId
                ?: return null, "", bodyString, messageType, meta, createdAt?.time ?: 0,
            0L, true, true, transient, false, false, DeliveryStatus.Sent, MessageState.None,
            from, attachmentArray.toTypedArray(), null, null, null, null,
            null, null, false, 0, 0, null)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getUserFromPushJson(remoteMessage: RemoteMessage): User? {
    val userJson = remoteMessage.data["user"] ?: return null
    return try {
        val userJsonObject = JSONObject(userJson)
        val id = userJsonObject.getString("id")
        val fName = userJsonObject.getString("first_name")
        val lName = userJsonObject.getString("last_name")
        val meta = userJsonObject.getString("metadata")
        val presence = userJsonObject.getString("presence_status")
        User(id, fName, lName, "", meta, Presence(PresenceState.Online, presence, 0),
            UserActivityStatus.Active, false)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getChannelFromPushJson(remoteMessage: RemoteMessage, peer: User?): Channel? {
    val channelJson = remoteMessage.data["channel"] ?: return null
    return try {
        val channelJsonObject = JSONObject(channelJson)
        val id = channelJsonObject.getString("id")
        val type = channelJsonObject.getString("type")
        val uri = channelJsonObject.getString("uri")
        val subject = channelJsonObject.getString("subject")
        val meta = channelJsonObject.getString("metadata")
        val membersCount = channelJsonObject.getInt("members_count")
        val channel: Channel = when (stringToEnum(type)) {
            ChannelTypeEnum.Direct -> DirectChannel(id.toLong(), meta, "", 0, 0,
                Member(Role(RoleTypeEnum.Owner.name), peer), null, 0L, 0, 0, false, 0, false,
                0L, 0L, 0, null, null, null)

            ChannelTypeEnum.Public -> PublicChannel(id.toLong(), uri, subject, meta, null, "", 0,
                0, arrayOf(), null, 0, 0, 0, membersCount.toLong(), false, 0,
                false, 0, 0, 0, null, null, null)

            ChannelTypeEnum.Private -> PrivateChannel(id.toLong(), subject, meta, "", "", 0, 0,
                arrayOf(), null, 0, 0, 0, membersCount.toLong(), false, 0,
                false, 0, 0, 0, null, null, null)
        }
        channel
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getAttachmentFromPushJson(attachment: JSONObject?): Attachment? {
    return try {
        attachment ?: return null
        val data = attachment.getString("data")
        val name = attachment.getString("name")
        val type = attachment.getString("type")
        val size = attachment.getLong("size")
        Attachment.Builder("", data, type).setFileSize(size).setName(name).build()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getReactionScoreFromPushJson(json: String?): ReactionScore? {
    return try {
        val jsonObject = JSONObject(json ?: return null)
        val key = jsonObject.getString("key")
        val score = jsonObject.getLong("score")
        if (key.isEmpty() || score == 0L) return null
        ReactionScore(key, score)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}