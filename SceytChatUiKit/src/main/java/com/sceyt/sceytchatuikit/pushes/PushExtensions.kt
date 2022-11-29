package com.sceyt.sceytchatuikit.pushes

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.PrivateChannel
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.Presence
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserActivityStatus
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import org.json.JSONException
import org.json.JSONObject

@Throws(JSONException::class)
fun getMessageBodyFromPushJson(messageJson: String?, channelId: Long?): Message? {
    val messageJsonObject = JSONObject(messageJson ?: return null)
    val messageId = messageJsonObject.getLong("Id")
    val bodyString = messageJsonObject.getString("Body")
    val messageType = messageJsonObject.getString("Type")
    val meta = messageJsonObject.getString("Metadata")
    val createdAtString = messageJsonObject.getString("CreatedAt")
    val transient = messageJsonObject.getBoolean("Transient")
    val createdAt = DateTimeUtil.convertStringToDate(createdAtString, DateTimeUtil.SERVER_DATE_PATTERN)
    return Message.MessageBuilder(channelId ?: return null)
        .setId(messageId)
        .setTid(messageId)
        .setBody(bodyString)
        .setTransient(transient)
        .setType(messageType)
        .setMetadata(meta)
        .setCreatedAt(createdAt?.time ?: 0L)
        .build().apply {
            deliveryStatus = DeliveryStatus.Sent
            state = MessageState.None
        }
}

@Throws(JSONException::class)
fun getUserFromPushJson(userJson: String?): User? {
    userJson ?: return null
    val userJsonObject = JSONObject(userJson)
    val id = userJsonObject.getString("Id")
    val fName = userJsonObject.getString("FirstName")
    val lName = userJsonObject.getString("LastName")
    val meta = userJsonObject.getString("Metadata")
    val presence = userJsonObject.getString("PresenceStatus")
    return User(id, fName, lName, "", meta, Presence(PresenceState.Online, presence, 0),
        UserActivityStatus.Active, false)
}

@Throws(JSONException::class)
fun getChannelFromPushJson(channelJson: String?): Channel? {
    channelJson ?: return null
    val channelJsonObject = JSONObject(channelJson)
    val id = channelJsonObject.getString("Id")
    val type = channelJsonObject.getString("Type")
    val uri = channelJsonObject.getString("Uri")
    val subject = channelJsonObject.getString("Subject")
    val label = channelJsonObject.getString("Label")
    val meta = channelJsonObject.getString("Metadata")
    val membersCount = channelJsonObject.getInt("MembersCount")
    val channel: Channel = when (type) {
        "direct" -> DirectChannel(id.toLong(),
            meta,
            label,
            0,
            0,
            arrayOf(),
            null,
            0,
            false,
            0L,
            false,
            0L,
            0L)
        else -> PrivateChannel(id.toLong(),
            subject,
            meta,
            "",
            label,
            0,
            0,
            arrayOf(),
            null,
            0,
            membersCount.toLong(),
            false,
            0L,
            false,
            0L,
            0L)
    }
    return channel
}