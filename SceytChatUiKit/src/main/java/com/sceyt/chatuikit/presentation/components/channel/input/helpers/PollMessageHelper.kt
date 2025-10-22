package com.sceyt.chatuikit.presentation.components.channel.input.helpers

import com.google.gson.Gson
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytPoll

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
     * @param allowMultipleAnswers Whether users can select multiple options (default: false)
     * @param anonymous Whether votes are anonymous (default: false)
     * @param allowAddOption Whether users can add their own options (default: false)
     * @param endAt Poll end timestamp, null for no expiration (default: null)
     * @return Message ready to be sent
     */
    fun createPollMessage(
        question: String,
        options: List<String>,
        allowMultipleAnswers: Boolean = false,
        anonymous: Boolean = false,
        allowAddOption: Boolean = false,
        endAt: Long? = null
    ): Message {
        require(question.isNotBlank()) { "Poll question cannot be blank" }
        require(options.size >= 2) { "Poll must have at least 2 options" }
        require(options.size <= 10) { "Poll cannot have more than 10 options" }

        val pollId = generatePollId()
        val createdAt = System.currentTimeMillis()

        val pollOptions = options.mapIndexed { index, text ->
            PollOption(
                id = "${pollId}_option_$index",
                text = text,
                voteCount = 0,
                voters = emptyList(),
                selected = false
            )
        }

        val poll = SceytPoll(
            id = pollId,
            question = question,
            options = pollOptions,
            allowMultipleAnswers = allowMultipleAnswers,
            anonymous = anonymous,
            allowAddOption = allowAddOption,
            endAt = endAt,
            createdAt = createdAt,
            totalVotes = 0,
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
            allowMultipleAnswers = true
        )
    }

    /**
     * Creates a poll with an expiration date
     * 
     * @param question The poll question
     * @param options List of options
     * @param durationMillis Duration in milliseconds from now
     */
    fun createTimedPoll(question: String, options: List<String>, durationMillis: Long): Message {
        val endAt = System.currentTimeMillis() + durationMillis
        return createPollMessage(
            question = question,
            options = options,
            endAt = endAt
        )
    }

    private fun generatePollId(): String {
        return "poll_${System.currentTimeMillis()}_${(0..999).random()}"
    }
}

/**
 * Extension function to send a poll message directly from MessageListViewModel
 */
fun com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel.sendPoll(
    question: String,
    options: List<String>,
    allowMultipleAnswers: Boolean = false,
    anonymous: Boolean = false,
    allowAddOption: Boolean = false,
    endAt: Long? = null
) {
    val message = PollMessageHelper.createPollMessage(
        question = question,
        options = options,
        allowMultipleAnswers = allowMultipleAnswers,
        anonymous = anonymous,
        allowAddOption = allowAddOption,
        endAt = endAt
    )
    sendMessage(message)
}

