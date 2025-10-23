package com.sceyt.chatuikit.presentation.components.channel.input.helpers

import com.google.gson.Gson
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytPollDetails
import java.util.UUID

/**
 * Helper class for creating and sending poll messages
 */
object PollMessageHelper {

    private val gson = Gson()

    /**
     * Creates a poll message
     *
     * @param question The poll question/title
     * @param options List of option texts
     * @param description Optional poll description (default: empty)
     * @param allowMultipleVotes Whether users can select multiple options (default: false)
     * @param anonymous Whether votes are anonymous (default: false)
     * @param allowVoteRetract Whether users can retract their votes (default: true)
     * @return Message ready to be sent
     */
    fun createPollMessage(
            question: String,
            options: List<String>,
            description: String = "",
            allowMultipleVotes: Boolean = false,
            anonymous: Boolean = false,
            allowVoteRetract: Boolean = true,
    ): Message {
        require(question.isNotBlank()) { "Poll question cannot be blank" }
        require(options.size >= 2) { "Poll must have at least 2 options" }
        require(options.size <= 10) { "Poll cannot have more than 10 options" }

        val pollId = generatePollId()
        val createdAt = System.currentTimeMillis()

        val pollOptions = options.map { text ->
            PollOption(
                id = UUID.randomUUID().toString(),
                name = text
            )
        }

        val poll = SceytPollDetails(
            id = pollId,
            name = question,
            description = description,
            options = pollOptions,
            anonymous = anonymous,
            allowMultipleVotes = allowMultipleVotes,
            allowVoteRetract = allowVoteRetract,
            votesPerOption = emptyMap(),
            votes = emptyList(),
            ownVotes = emptyList(),
            createdAt = createdAt,
            updatedAt = createdAt,
            closedAt = 0,
            closed = false
        )

        return Message.MessageBuilder()
            .setTid(ClientWrapper.generateTid())
            .setType("poll")
            .setBody(question) // Use question as body for preview/notification
            .setMetadata(gson.toJson(poll))
            .setCreatedAt(createdAt)
            .build()
    }

    /**
     * Creates a simple poll message with just a question and options
     */
    fun createSimplePoll(question: String, vararg options: String): Message {
        return createPollMessage(
            question = question,
            options = options.toList()
        )
    }

    /**
     * Creates an anonymous poll message
     */
    fun createAnonymousPoll(question: String, options: List<String>): Message {
        return createPollMessage(
            question = question,
            options = options,
            anonymous = true
        )
    }

    /**
     * Creates a poll with multiple choice allowed
     */
    fun createMultipleChoicePoll(question: String, options: List<String>): Message {
        return createPollMessage(
            question = question,
            options = options,
            allowMultipleVotes = true
        )
    }

    private fun generatePollId(): String {
        return UUID.randomUUID().toString()
    }
}

/**
 * Extension function to send a poll message directly from MessageListViewModel
 */
fun com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel.sendPoll(
        question: String,
        options: List<String>,
        description: String = "",
        allowMultipleVotes: Boolean = false,
        anonymous: Boolean = false,
        allowVoteRetract: Boolean = true,
) {
    val message = PollMessageHelper.createPollMessage(
        question = question,
        options = options,
        description = description,
        allowMultipleVotes = allowMultipleVotes,
        anonymous = anonymous,
        allowVoteRetract = allowVoteRetract
    )
    sendMessage(message)
}

