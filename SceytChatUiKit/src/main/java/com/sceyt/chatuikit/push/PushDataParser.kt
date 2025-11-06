package com.sceyt.chatuikit.push

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sceyt.chat.models.attachment.Attachment
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.BodyAttribute
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.ForwardingDetails
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.poll.PollDetails
import com.sceyt.chat.models.poll.PollOption
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.Presence
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.extensions.getStringOrNull
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import org.json.JSONArray
import org.json.JSONObject

object PushDataParser {
    private const val KEY_CHANNEL = "channel"
    private const val KEY_MESSAGE = "message"
    private const val KEY_USER = "user"
    private const val KEY_REACTION = "reaction"
    private const val KEY_MENTIONED_USERS = "mentions"
    private const val KEY_ATTACHMENTS = "attachments"
    private const val KEY_POLL_DETAILS = "poll_details"

    fun getMessage(
        payload: Map<String, String>,
        channelId: Long?,
        user: User?,
    ): Message? {
        channelId ?: return null
        return try {
            val messageJson = payload[KEY_MESSAGE]
            val messageJsonObject = JSONObject(messageJson ?: return null)

            // Do not getLong from json when its a string
            // double.parse corrupts the value
            val messageIdString = messageJsonObject.getString("id")
            val parentMessageIdString =
                messageJsonObject.getStringOrNull("parent_id")?.toLongOrNull()
            val bodyString = messageJsonObject.getString("body")
            val messageType = messageJsonObject.getString("type")
            val meta = messageJsonObject.getString("metadata")
            val createdAtString = messageJsonObject.getString("created_at")
            val transient = messageJsonObject.getBoolean("transient")
            val deliveryStatus = getDeliveryStatusFromJson(messageJsonObject)
            val state = getStateFromJson(messageJsonObject)
            val forwardingDetails = getForwardingDetailsFromJson(messageJsonObject)
            val bodyAttributes = getBodyAttributesFromJson(messageJsonObject)
            val pollDetails = messageJsonObject.getPollDetailsFromJSON()
            val createdAt = DateTimeUtil.convertStringToDate(
                date = createdAtString,
                datePattern = DateTimeUtil.SERVER_DATE_PATTERN
            )

            val attachmentArray = mutableListOf<Attachment>()
            val attachments = messageJsonObject.getJSONArray(KEY_ATTACHMENTS)
            for (i in 0 until attachments.length()) {
                when (val attachment = attachments[i]) {
                    is JSONObject -> {
                        attachment.getAttachmentFromJSON()?.let {
                            attachmentArray.add(it)
                        }
                    }
                }
            }

            val mentionUsersArray by lazy { mutableListOf<User>() }
            val mentionedUsersJson = payload[KEY_MENTIONED_USERS]
            if (mentionedUsersJson != null) {
                runCatching {
                    JSONArray(mentionedUsersJson)
                }.onSuccess { mentionedUsers ->
                    for (i in 0 until mentionedUsers.length()) {
                        when (val value = mentionedUsers[i]) {
                            is JSONObject -> {
                                value.getUserFromJSON()?.let {
                                    mentionUsersArray.add(it)
                                }
                            }
                        }
                    }
                }
            }

            val parentMessage = if (parentMessageIdString != null)
                Message(parentMessageIdString, channelId, MessageState.Unmodified) else null
            val messageId = messageIdString.toLongOrNull() ?: return null
            Message(
                /* id = */ messageId,
                /* tid = */ messageId,
                /* channelId = */ channelId,
                /* body = */ bodyString,
                /* type = */ messageType,
                /* metadata = */ meta,
                /* createdAt = */ createdAt?.time ?: 0,
                /* updatedAt = */ 0L,
                /* incoming = */ true,
                /* isTransient = */ transient,
                /* silent = */ false,
                /* deliveryStatus = */ deliveryStatus,
                /* state = */ state,
                /* user = */ user,
                /* attachments = */ attachmentArray.toTypedArray(),
                /* userReactions = */ null,
                /* reactionTotal = */ null,
                /* markerTotals = */ null,
                /* userMarkers = */ null,
                /* mentionedUsers = */ mentionUsersArray.toTypedArray(),
                /* parentMessage = */ parentMessage,
                /* replyCount = */ 0,
                /* displayCount = */ 0,
                /* autoDeleteAt = */ 0,
                /* forwardingDetails = */ forwardingDetails,
                /* bodyAttributes = */ bodyAttributes.toTypedArray(),
                /* disableMentionsCount = */ false,
                /* poll = */ pollDetails
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getUser(payload: Map<String, String>): User? {
        val userJson = payload[KEY_USER] ?: return null
        return try {
            val userJsonObject = JSONObject(userJson)
            userJsonObject.getUserFromJSON()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getMember(payload: Map<String, String>): Member? {
        val userJson = payload[KEY_USER] ?: return null
        return try {
            val userJsonObject = JSONObject(userJson)
            val user = getUser(payload) ?: return null
            val role = userJsonObject.getString("role") ?: return null
            Member(Role(role), user)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getChannel(payload: Map<String, String>): Channel? {
        val channelJson = payload[KEY_CHANNEL] ?: return null
        return try {
            val channelJsonObject = JSONObject(channelJson)
            val id = channelJsonObject.getString("id").toLongOrNull() ?: return null
            val type = channelJsonObject.getString("type")
            val uri = channelJsonObject.getString("uri")
            val subject = channelJsonObject.getString("subject")
            val meta = channelJsonObject.getString("metadata")
            val membersCount = channelJsonObject.getLong("members_count")
            val members = listOfNotNull(getMember(payload))
            val channel = Channel(
                /* id = */ id,
                /* parentChannelId = */ 0,
                /* uri = */ uri,
                /* type = */ type,
                /* subject = */ subject,
                /* avatarUrl = */ null,
                /* metadata = */ meta,
                /* createdAt = */ 0,
                /* updatedAt = */ 0,
                /* messagesClearedAt = */ 0,
                /* memberCount = */ membersCount,
                /* createdBy = */ null,
                /* userRole = */ RoleTypeEnum.Member.value, // Todo we should receive the role from the server.
                /* unread = */ false,
                /* newMessageCount = */ 0,
                /* newMentionCount = */ 0,
                /* newReactedMessageCount = */ 0,
                /* hidden = */ false,
                /* archived = */ false,
                /* muted = */ false,
                /* mutedTill = */ 0,
                /* pinnedAt = */ 0,
                /* lastReceivedMessageId = */ 0,
                /* lastDisplayedMessageId = */ 0L,
                /* messageRetentionPeriod = */ 0L,
                /* lastMessage = */ null,
                /* messages = */ emptyArray(),
                /* members = */ members.toTypedArray(),
                /* newReactions = */ emptyArray())
            channel
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getReaction(payload: Map<String, String>, messageId: Long?, user: User?): SceytReaction? {
        return try {
            val jsonObject = JSONObject(payload[KEY_REACTION] ?: return null)
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

    fun JSONObject.getAttachmentFromJSON(): Attachment? {
        return try {
            val data = getString("data")
            val name = getString("name")
            val type = getString("type")
            val metadata = getString("metadata")
            val size = getString("size").toLongOrNull() ?: return null
            Attachment.Builder("", data, type)
                .setFileSize(size)
                .setName(name)
                .setMetadata(metadata).build()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun JSONObject.getUserFromJSON(): User? = try {
        val id = getString("id")
        val username = getStringOrNull("username") ?: ""
        val fName = getString("first_name")
        val lName = getString("last_name")
        val presence = getStringOrNull("presence_status")
        User(
            /* id = */ id,
            /* username = */ username,
            /* firstName = */ fName,
            /* lastName = */ lName,
            /* avatarURL = */ "",
            /* metadataMap = */ null,
            /* presence = */ Presence(PresenceState.Online, presence.orEmpty(), 0),
            /* state = */ UserState.Active,
            /* blocked = */ false
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    fun JSONObject.getPollOptionFromJSON(): PollOption? = try {
        val optionId = getString("id")
        val name = getString("name")
        PollOption(optionId, name)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    fun JSONObject.getPollDetailsFromJSON(): PollDetails? = try {
        val pollJson = getJSONObject(KEY_POLL_DETAILS) ?: return null
        val pollId = pollJson.getString("poll_id")
        val name = pollJson.getString("name")
        val description = pollJson.getStringOrNull("description")
        val anonymous = pollJson.getBoolean("anonymous")
        val allowMultipleVotes = pollJson.getBoolean("allow_multiple_votes")
        val allowVoteRetract = pollJson.getBoolean("allow_vote_retract")

        val optionsJsonArray = pollJson.getJSONArray("options")
        val optionsList = mutableListOf<PollOption>()
        for (i in 0 until optionsJsonArray.length()) {
            when (val option = optionsJsonArray[i]) {
                is JSONObject -> {
                    option.getPollOptionFromJSON()?.let {
                        optionsList.add(it)
                    }
                }
            }
        }

        PollDetails.Builder()
            .setId(pollId)
            .setName(name)
            .setDescription(description)
            .setOptions(optionsList.toTypedArray())
            .setAnonymous(anonymous)
            .setAllowMultipleVotes(allowMultipleVotes)
            .setAllowVoteRetract(allowVoteRetract)
            .build()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private fun getDeliveryStatusFromJson(jsonObject: JSONObject): DeliveryStatus {
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

    private fun getForwardingDetailsFromJson(jsonObject: JSONObject): ForwardingDetails? {
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

    private fun getStateFromJson(jsonObject: JSONObject): MessageState {
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

    private fun getBodyAttributesFromJson(jsonObject: JSONObject): List<BodyAttribute> {
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
}