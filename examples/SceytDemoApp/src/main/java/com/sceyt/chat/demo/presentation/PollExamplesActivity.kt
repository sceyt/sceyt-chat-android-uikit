package com.sceyt.chat.demo.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.sceyt.chat.demo.databinding.ActivityPollExamplesBinding
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytPoll
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModelFactory
import java.util.UUID

/**
 * Example activity demonstrating how to send poll messages
 *
 * This activity provides buttons to test all different types of polls.
 */
class PollExamplesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPollExamplesBinding
    private val viewModel: MessageListViewModel by viewModels(factoryProducer = { factory })

    private val factory: MessageListViewModelFactory by lazy(LazyThreadSafetyMode.NONE) {
        MessageListViewModelFactory(requireNotNull(intent.parcelable(CHANNEL_KEY)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPollExamplesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
    }

    private fun setupButtons() {
        binding.btnSimplePoll.setOnClickListener {
            sendSimplePoll()
            showStatus("Sent: Simple Poll")
        }

        binding.btnAnonymousPoll.setOnClickListener {
            sendAnonymousPoll()
            showStatus("Sent: Anonymous Poll")
        }

        binding.btnMultipleChoicePoll.setOnClickListener {
            sendMultipleChoicePoll()
            showStatus("Sent: Multiple Choice Poll")
        }

        binding.btnTimedPoll.setOnClickListener {
            sendTimedPoll()
            showStatus("Sent: Timed Poll (7 days)")
        }

        binding.btnAdvancedPoll.setOnClickListener {
            sendAdvancedPoll()
            showStatus("Sent: Advanced Poll")
        }

        binding.btnExtensionPoll.setOnClickListener {
            sendPollWithExtension()
            showStatus("Sent: Poll using Extension Function")
        }

        binding.btnYesNoPoll.setOnClickListener {
            sendQuickYesNoPoll()
            showStatus("Sent: Yes/No Poll")
        }

        binding.btnRatingPoll.setOnClickListener {
            sendRatingPoll()
            showStatus("Sent: Rating Poll")
        }
    }

    private fun showStatus(message: String) {
        binding.tvStatus.text = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        // Finish after short delay to return to chat
        binding.root.postDelayed({ finish() }, 500)
    }

    companion object {
        private const val CHANNEL_KEY = "CHANNEL_KEY"

        fun launch(context: Context, channel: SceytChannel) {
            context.startActivity(Intent(context, PollExamplesActivity::class.java).apply {
                putExtra(CHANNEL_KEY, channel)
            })
        }

        // Mock users for demonstration
        private val mockUsers = listOf(
            SceytUser(
                id = "user1",
            ).copy(
                firstName = "John",
                lastName = "Doe",
            ),
            SceytUser(
                id = "user2",
            ).copy(
                firstName = "Jane",
                lastName = "Smith",
            ),
            SceytUser(
                id = "user3",
            ).copy(
                firstName = "Bob",
                lastName = "Wilson",
            ),
            SceytUser(
                id = "user4",
            ).copy(
                firstName = "Alice",
                lastName = "Johnson",
            ),
            SceytUser(
                id = "user5",
            ).copy(
                firstName = "Charlie",
                lastName = "Brown",
            ),
            SceytUser(
                id = "user6",
            ).copy(
                firstName = "Diana",
                lastName = "Prince",
            )
        )
    }

    /**
     * Helper function to create a poll message with voters
     */
    private fun createPollWithVoters(
            question: String,
            options: List<String>,
            voteCounts: List<Int>,
            allowMultipleAnswers: Boolean = false,
            anonymous: Boolean = false,
    ): Message {
        val pollId = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()

        val pollOptions = options.mapIndexed { index, text ->
            val voteCount = voteCounts.getOrNull(index) ?: 0
            val voters = if (!anonymous && voteCount > 0) {
                mockUsers.take(minOf(voteCount, mockUsers.size))
            } else {
                emptyList()
            }

            PollOption(
                id = UUID.randomUUID().toString(),
                text = text,
                voteCount = voteCount,
                voters = voters,
                selected = false
            )
        }

        val totalVotes = voteCounts.sum()
        val poll = SceytPoll(
            id = pollId,
            question = question,
            options = pollOptions,
            allowMultipleAnswers = allowMultipleAnswers,
            anonymous = anonymous,
            allowAddOption = false,
            endAt = null,
            createdAt = createdAt,
            totalVotes = totalVotes,
            closed = false
        )

        val gson = Gson()
        return Message.MessageBuilder()
            .setTid(ClientWrapper.generateTid())
            .setType("poll")
            .setBody(question)
            .setMetadata(gson.toJson(poll))
            .setCreatedAt(createdAt)
            .build()
    }


    /**
     * Example 1: Simple poll
     * Creates a basic poll with a question and options with voters
     */
    fun sendSimplePoll() {
        val message = createPollWithVoters(
            question = "What's the most useful AI feature?",
            options = listOf("AI avatars", "AI music", "AI assistance", "AI translation"),
            voteCounts = listOf(3, 6, 1, 2) // Vote counts for each option
        )

        viewModel.sendMessage(message)
    }

    /**
     * Example 2: Anonymous poll
     * Creates a poll where votes are anonymous (no voter avatars shown)
     */
    fun sendAnonymousPoll() {
        val message = createPollWithVoters(
            question = "The new UI design improves user experience",
            options = listOf("Strongly Agree", "Agree", "Neutral", "Disagree", "Strongly Disagree"),
            voteCounts = listOf(5, 3, 2, 1, 0),
            anonymous = true
        )

        viewModel.sendMessage(message)
    }

    /**
     * Example 3: Multiple choice poll
     * Allows users to select multiple options
     */
    fun sendMultipleChoicePoll() {
        val message = createPollWithVoters(
            question = "Which programming languages do you use? (Select all)",
            options = listOf("Java", "Kotlin", "Python", "JavaScript", "Swift", "Go"),
            voteCounts = listOf(2, 5, 4, 3, 2, 1),
            allowMultipleAnswers = true
        )

        viewModel.sendMessage(message)
    }

    /**
     * Example 4: Timed poll
     * Creates a poll that expires after a certain duration
     */
    fun sendTimedPoll() {
        val message = createPollWithVoters(
            question = "Best day for team meeting?",
            options = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"),
            voteCounts = listOf(2, 4, 3, 1, 2)
        )

        viewModel.sendMessage(message)
    }

    /**
     * Example 5: Advanced poll with all options
     * Creates a poll with full customization
     */
    fun sendAdvancedPoll() {
        val message = createPollWithVoters(
            question = "Which feature should we prioritize?",
            options = listOf("Dark Mode", "Push Notifications", "Search", "Custom Themes"),
            voteCounts = listOf(5, 3, 4, 2),
            allowMultipleAnswers = false
        )

        viewModel.sendMessage(message)
    }

    /**
     * Example 6: Using extension function
     * Simplified way to send polls directly from ViewModel
     */
    fun sendPollWithExtension() {
        val message = createPollWithVoters(
            question = "Should we have a team outing?",
            options = listOf("Yes", "No", "Maybe"),
            voteCounts = listOf(5, 1, 2),
            anonymous = false
        )

        viewModel.sendMessage(message)
    }

    /**
     * Example 7: Quick yes/no poll
     */
    fun sendQuickYesNoPoll() {
        val message = createPollWithVoters(
            question = "Are you attending the meeting?",
            options = listOf("Yes", "No", "Maybe"),
            voteCounts = listOf(4, 2, 1)
        )

        viewModel.sendMessage(message)
    }

    /**
     * Example 8: Rating poll
     */
    fun sendRatingPoll() {
        val message = createPollWithVoters(
            question = "How would you rate the new feature?",
            options = listOf("⭐", "⭐⭐", "⭐⭐⭐", "⭐⭐⭐⭐", "⭐⭐⭐⭐⭐"),
            voteCounts = listOf(0, 1, 2, 3, 6)
        )

        viewModel.sendMessage(message)
    }
}

