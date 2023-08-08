package com.sceyt.sceytchatuikit.pushes

import com.google.firebase.messaging.RemoteMessage
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.Presence
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserActivityState
import com.sceyt.sceytchatuikit.data.models.messages.SceytReaction
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import org.json.JSONObject

fun getMessageBodyFromPushJson(remoteMessage: RemoteMessage, channelId: Long?, user: User?): Message? {
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
        val deliveryStatus = getDeliveryStatusFromPushJson(messageJsonObject)
        val state = getStateFromPushJson(messageJsonObject)
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
                ?: return null, bodyString, messageType, meta, createdAt?.time ?: 0,
            0L, true, transient, false, deliveryStatus, state,
            user, attachmentArray.toTypedArray(), null, null, null, null,
            null, null, 0, 0, 0, null)
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
            UserActivityState.Active, false)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getChannelFromPushJson(remoteMessage: RemoteMessage): Channel? {
    val channelJson = remoteMessage.data["channel"] ?: return null
    return try {
        val channelJsonObject = JSONObject(channelJson)
        val id = channelJsonObject.getString("id").toLong()
        val type = channelJsonObject.getString("type")
        val uri = channelJsonObject.getString("uri")
        val subject = channelJsonObject.getString("subject")
        val meta = channelJsonObject.getString("metadata")
        val membersCount = channelJsonObject.getLong("members_count")
        val channel = Channel(id, 0, uri, type, subject, null, meta, 0, 0,
            0, membersCount, null, "", false, 0, 0,
            0, false, false, false, 0, 0, 0,
            0L, 0L, null, emptyArray(), emptyArray(), emptyArray())
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
        val metadata = attachment.getString("metadata")
        val size = attachment.getString("size").toLong()
        Attachment.Builder("", data, type)
            .setFileSize(size)
            .setName(name)
            .setMetadata(metadata).build()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getReactionFromPushJson(json: String?, messageId: Long?, user: User?): SceytReaction? {
    return try {
        val jsonObject = JSONObject(json ?: return null)
        val id = jsonObject.getString("id").toLong()
        val key = jsonObject.getString("key")
        val score = jsonObject.getString("score").toInt()
        val reason = jsonObject.getString("reason")
        val createdAt = jsonObject.getString("created_at").toLong()

        if (key.isEmpty() || score == 0 || messageId == null || user == null) return null
        SceytReaction(id, messageId, key, score, reason, createdAt, user, false)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getDeliveryStatusFromPushJson(jsonObject: JSONObject): DeliveryStatus {
    return try {
        when (jsonObject.getString("delivery_status")) {
            "sent" -> DeliveryStatus.Sent
            "delivered" -> DeliveryStatus.Received
            "read" -> DeliveryStatus.Displayed
            else -> DeliveryStatus.Sent
        }
    } catch (e: Exception) {
        e.printStackTrace()
        DeliveryStatus.Sent
    }
}

private fun getStateFromPushJson(jsonObject: JSONObject): MessageState {
    return try {
        when (jsonObject.getString("state")) {
            "none" -> MessageState.Unmodified
            "edited" -> MessageState.Edited
            "deleted" -> MessageState.Deleted
            else -> MessageState.Unmodified
        }
    } catch (e: Exception) {
        e.printStackTrace()
        MessageState.Unmodified
    }
}