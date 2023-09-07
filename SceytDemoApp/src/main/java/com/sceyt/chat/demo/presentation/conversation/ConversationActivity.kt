package com.sceyt.chat.demo.presentation.conversation

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chat.demo.databinding.ActivityConversationBinding
import com.sceyt.chat.demo.presentation.conversationinfo.CustomConversationInfoActivity
import com.sceyt.chat.demo.presentation.mainactivity.MainActivity
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelTypingEventData
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.asActivity
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.overrideTransitions
import com.sceyt.sceytchatuikit.extensions.parcelable
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.persistence.filetransfer.*
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageActionsViewClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.bindings.bind
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.clicklisteners.HeaderClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.eventlisteners.HeaderEventsListenerImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.uiupdatelisteners.HeaderUIElementsListenerImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.InputState
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.MessageInputView
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.eventlisteners.InputEventsListenerImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessageInputViewStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

open class ConversationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationBinding
    private val viewModel: MessageListViewModel by viewModels {
        MyViewModelFactory()
    }
    private lateinit var channel: SceytChannel
    private var isReplyInThread = false
    private var replyMessage: SceytMessage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(ActivityConversationBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        statusBarIconsColorWithBackground(SceytKitConfig.isDarkMode)

        getDataFromIntent()

        binding.headerView.initHeaderView()
        binding.messagesListView.initConversationView()
        binding.messageInputView.initMessageInputView()

        viewModel.bind(binding.messagesListView, lifecycleOwner = this)
        viewModel.bind(binding.messageInputView, replyMessage, lifecycleOwner = this)
        viewModel.bind(binding.headerView, replyMessage, lifecycleOwner = this)
    }

    private fun ConversationHeaderView.initHeaderView() {
        //Example
        setCustomUiElementsListener(object : HeaderUIElementsListenerImpl(this) {
            override fun onSubTitle(subjectTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage?, replyInThread: Boolean) {
                super.onSubTitle(subjectTextView, channel, replyMessage, replyInThread)
                println("onSubject")
            }
        })

        setCustomEventListener(object : HeaderEventsListenerImpl(this) {
            override fun onTypingEvent(data: ChannelTypingEventData) {
                super.onTypingEvent(data)
                println("typing")
            }
        })

        setCustomClickListener(object : HeaderClickListenersImpl(this) {
            override fun onAvatarClick(view: View) {
                ///CustomConversationInfoActivity.newInstance(this@ConversationActivity, channel)

                //binding.messageInputView.send()
            }

            override fun onToolbarClick(view: View) {
                CustomConversationInfoActivity.newInstance(this@ConversationActivity, channel)
            }
        })
    }

    private fun MessagesListView.initConversationView() {
        //This listener will not work if you have added custom click listener
        setMessageClickListener(MessageClickListeners.ReplyCountClickListener { _, item ->
            newInstance(this@ConversationActivity, channel, item.message)
        })


        setCustomMessageClickListener(object : MessageClickListenersImpl(binding.messagesListView) {
            override fun onReplyCountClick(view: View, item: MessageListItem.MessageItem) {
                super.onReplyCountClick(view, item)
                newInstance(this@ConversationActivity, channel, item.message)
            }
        })

        setCustomMessageActionsViewClickListener(object : MessageActionsViewClickListenersImpl(binding.messagesListView) {
            override fun onReactMessageClick(message: SceytMessage) {
                super.onReactMessageClick(message)
                println("React")
            }

            override fun onReplyMessageInThreadClick(message: SceytMessage) {
                super.onReplyMessageInThreadClick(message)
                newInstance(this@ConversationActivity, channel, message)
            }
        })

    }

    private fun MessageInputView.initMessageInputView() {
        setCustomClickListener(object : MessageInputClickListenersImpl(this) {
            override fun onSendMsgClick(view: View) {
                super.onSendMsgClick(view)
                println("send")
            }
        })

        setCustomEventListener(object : InputEventsListenerImpl(this) {
            override fun onInputStateChanged(sendImage: ImageView, state: InputState) {
                sendImage.apply {
                    setImageResource(MessageInputViewStyle.sendMessageIcon)
                    alpha = if (state == InputState.Voice) 0.5f else 1f
                }
            }
        })
    }

    private fun getDataFromIntent() {
        channel = requireNotNull(intent.parcelable(CHANNEL))
        isReplyInThread = intent.getBooleanExtra(REPLY_IN_THREAD, false)
        replyMessage = intent.parcelable(REPLY_IN_THREAD_MESSAGE)
    }

    companion object {
        const val CHANNEL = "CHANNEL"
        private const val REPLY_IN_THREAD = "REPLY_IN_THREAD"
        private const val REPLY_IN_THREAD_MESSAGE = "REPLY_IN_THREAD_MESSAGE"

        fun newInstance(context: Context, channel: SceytChannel) {
            context.launchActivity<ConversationActivity> {
                putExtra(CHANNEL, channel)
            }
            context.asActivity().overrideTransitions(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold, true)
        }

        fun newInstance(context: Context, channel: SceytChannel, message: SceytMessage) {
            context.launchActivity<ConversationActivity> {
                putExtra(CHANNEL, channel)
                putExtra(REPLY_IN_THREAD, true)
                putExtra(REPLY_IN_THREAD_MESSAGE, message)
            }
            context.asActivity().overrideTransitions(R.anim.sceyt_anim_slide_in_right, R.anim.sceyt_anim_slide_hold, true)
        }
    }

    override fun finish() {
        if (isTaskRoot) {
            launchActivity<MainActivity>()
            overrideTransitions(0, 0, true)
        }
        super.finish()
        overrideTransitions(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right, true)
    }

    inner class MyViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val channel: SceytChannel = requireNotNull(intent.parcelable(CHANNEL))
            val conversationId = if (isReplyInThread) replyMessage?.id ?: 0 else channel.id

            @Suppress("UNCHECKED_CAST")
            return MessageListViewModel(conversationId, isReplyInThread, channel) as T
        }
    }
}