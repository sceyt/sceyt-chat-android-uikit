package com.sceyt.chatuikit.push

import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.ForwardingDetails
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.Presence
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.extensions.getStringOrNull
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import org.json.JSONObject

fun getMessageFromPushJson(remoteMessage: RemoteMessage, channelId: Long?, user: User?): Message? {
    channelId ?: return null
    return try {
        val messageJson = remoteMessage.data["message"]
        val messageJsonObject = JSONObject(messageJson ?: return null)

        // Do not getLong from json when its a string
        // double.parse corrupts the value
        val messageIdString = messageJsonObject.getString("id")
        val parentMessageIdString = messageJsonObject.getStringOrNull("parent_id")?.toLongOrNull()
        val bodyString = messageJsonObject.getString("body")
        val messageType = messageJsonObject.getString("type")
        val meta = messageJsonObject.getString("metadata")
        val createdAtString = messageJsonObject.getString("created_at")
        val transient = messageJsonObject.getBoolean("transient")
        val deliveryStatus = getDeliveryStatusFromPushJson(messageJsonObject)
        val state = getStateFromPushJson(messageJsonObject)
        val forwardingDetails = getForwardingDetailsFromPushJson(messageJsonObject)
        val bodyAttributes = getBodyAttributesFromPushJson(messageJsonObject)
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
        val parentMessage = if (parentMessageIdString != null) Message(parentMessageIdString, channelId, MessageState.Unmodified) else null
        val messageId = messageIdString.toLongOrNull() ?: return null
        Message(messageId, messageId, channelId, bodyString, messageType, meta, createdAt?.time
                ?: 0,
            0L, true, transient, false, deliveryStatus, state,
            user, attachmentArray.toTypedArray(), null, null, null, null,
            null, parentMessage, 0, 0, 0, forwardingDetails, bodyAttributes.toTypedArray())
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
        val username = userJsonObject.getStringOrNull("username") ?: ""
        val fName = userJsonObject.getString("first_name")
        val lName = userJsonObject.getString("last_name")
        val presence = userJsonObject.getString("presence_status")
        User(id, username, fName, lName, "", null,
            Presence(PresenceState.Online, presence, 0),
            UserState.Active, false)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getChannelFromPushJson(remoteMessage: RemoteMessage): Channel? {
    val channelJson = remoteMessage.data["channel"] ?: return null
    return try {
        val channelJsonObject = JSONObject(channelJson)
        val id = channelJsonObject.getString("id").toLongOrNull() ?: return null
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
        val size = attachment.getString("size").toLongOrNull() ?: return null
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
        val id = jsonObject.getString("id").toLongOrNull() ?: return null
        val key = jsonObject.getString("key")
        val score = jsonObject.getString("score").toInt()
        val reason = jsonObject.getString("reason")
        val createdAt = jsonObject.getString("created_at").toLongOrNull() ?: return null

        if (key.isEmpty() || score == 0 || messageId == null || user == null) return null
        SceytReaction(id, messageId, key, score, reason, createdAt, user.toSceytUser(), false)
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

private fun getForwardingDetailsFromPushJson(jsonObject: JSONObject): ForwardingDetails? {
    return try {
        val data = jsonObject.getJSONObject("forwarding_details")
        val channelId = data.getString("channel_id").toLongOrNull() ?: return null
        val messageId = data.getString("message_id").toLongOrNull() ?: return null
        val userId = data.getString("user_id")
        val hops = data.getString("hops").toIntOrNull() ?: 0
        ForwardingDetails(messageId, channelId, User(userId), hops)
    } catch (e: Exception) {
        e.printStackTrace()
        null
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

private fun getBodyAttributesFromPushJson(jsonObject: JSONObject): List<BodyAttribute> {
    return try {
        val bodyAttributes = jsonObject.getString("body_attributes")
        val typeToken = object : TypeToken<List<BodyAttribute>>() {}.type
        val bodyAttributesList: List<BodyAttribute> = Gson().fromJson(bodyAttributes, typeToken)
        bodyAttributesList
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}