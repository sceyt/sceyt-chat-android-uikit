package com.sceyt.sceytchatuikit.pushes

import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.PrivateChannel
import com.sceyt.chat.models.channel.PublicChannel
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.Presence
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserActivityStatus
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.stringToEnum
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import org.json.JSONException
import org.json.JSONObject

@Throws(JSONException::class)
fun getMessageBodyFromPushJson(messageJson: String?, channelId: Long?, from: User?): Message? {
    val messageJsonObject = JSONObject(messageJson ?: return null)
    val messageId = messageJsonObject.getLong("Id")
    val bodyString = messageJsonObject.getString("Body")
    val messageType = messageJsonObject.getString("Type")
    val meta = messageJsonObject.getString("Metadata")
    val createdAtString = messageJsonObject.getString("CreatedAt")
    val transient = messageJsonObject.getBoolean("Transient")
    val createdAt = DateTimeUtil.convertStringToDate(createdAtString, DateTimeUtil.SERVER_DATE_PATTERN)

    val attachmentArray = ArrayList<Attachment>()
    val attachments = messageJsonObject.getJSONArray("Attachments")
    for (i in 0 until attachments.length()) {
        when (val value: Any = attachments[i]) {
            is JSONObject -> {
                getAttachmentFromPushJson(value)?.let { attachmentArray.add(it) }
            }
        }
    }

    return Message(messageId, messageId, channelId
            ?: return null, "", bodyString, messageType, meta, createdAt?.time ?: 0,
        0L, true, true, transient, false, false, DeliveryStatus.Sent, MessageState.None,
        from, attachmentArray.toTypedArray(), null, null, null, null,
        null, null, false, 0, 0, null)
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
    val channel: Channel = when (stringToEnum(type)) {
        ChannelTypeEnum.Direct -> DirectChannel(id.toLong(), meta, label, 0, 0,
            arrayOf(), null, 0, false, 0L, false,
            0L, 0L, 0)

        ChannelTypeEnum.Public -> PublicChannel(id.toLong(), uri, subject, meta, null, label, 0,
            0, arrayOf(), null, 0, membersCount.toLong(), false, 0,
            false, 0, 0, 0)

        ChannelTypeEnum.Private -> PrivateChannel(id.toLong(), subject, meta, "", label, 0, 0,
            arrayOf(), null, 0, membersCount.toLong(), false, 0,
            false, 0, 0, 0)
    }
    return channel
}

@Throws(JSONException::class)
fun getAttachmentFromPushJson(attachment: JSONObject?): Attachment? {
    attachment ?: return null
    val data = attachment.getString("Data")
    val name = attachment.getString("Name")
    val type = attachment.getString("Type")
    val size = attachment.getLong("Size")

    return Attachment.Builder("", data, type).setFileSize(size).setName(name).build()
}