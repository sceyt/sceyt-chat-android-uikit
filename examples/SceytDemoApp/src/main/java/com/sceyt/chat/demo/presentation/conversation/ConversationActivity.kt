package com.sceyt.chat.demo.presentation.conversation

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chat.demo.databinding.ActivityConversationBinding
import com.sceyt.chat.demo.presentation.common.keybordanim.ControlFocusInsetsAnimationCallback
import com.sceyt.chat.demo.presentation.common.keybordanim.RootViewDeferringInsetsCallback
import com.sceyt.chat.demo.presentation.common.keybordanim.TranslateDeferringInsetsAnimationCallback
import com.sceyt.chat.demo.presentation.conversationinfo.CustomConversationInfoActivity
import com.sceyt.chat.demo.presentation.mainactivity.MainActivity
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.asActivity
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.overrideTransitions
import com.sceyt.sceytchatuikit.extensions.parcelable
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.MessagesListView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageActionsViewClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.bindings.bind
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.ConversationHeaderView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.clicklisteners.HeaderClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.eventlisteners.HeaderEventsListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationheader.uiupdatelisteners.HeaderUIElementsListenerImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.MessageInputView
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners
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

        statusBarIconsColorWithBackground(SceytKitConfig.isDarkMode,
            statusBarColor = R.color.sceyt_color_primary,
            navigationBarColor = R.color.sceyt_color_primary_dark)

        //setupKeyboardAnimation()

        getDataFromIntent()

        binding.headerView.initHeaderView()
        binding.messagesListView.initConversationView()
        binding.messageInputView.initMessageInputView()

        viewModel.bind(binding.messagesListView, lifecycleOwner = this)
        viewModel.bind(binding.messageInputView, replyMessage, lifecycleOwner = this)
        viewModel.bind(binding.headerView, replyMessage, lifecycleOwner = this)
    }

    private fun setupKeyboardAnimation() {
        // Tell the Window that our app is going to responsible for fitting for any system windows.
        // This is similar to the now deprecated:
        // view.setSystemUiVisibility(LAYOUT_STABLE | LAYOUT_FULLSCREEN | LAYOUT_FULLSCREEN)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        /**
         * 1) Since our Activity has declared `window.setDecorFitsSystemWindows(false)`, we need to
         * handle any [WindowInsetsCompat] as appropriate.
         *
         * Our [RootViewDeferringInsetsCallback] will update our attached view's padding to match
         * the combination of the [WindowInsetsCompat.Type.systemBars], and selectively apply the
         * [WindowInsetsCompat.Type.ime] insets, depending on any ongoing WindowInsetAnimations
         * (see that class for more information).
         */
        val deferringInsetsListener = RootViewDeferringInsetsCallback(
            persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
            deferredInsetTypes = WindowInsetsCompat.Type.ime()
        )
        // RootViewDeferringInsetsCallback is both an WindowInsetsAnimation.Callback and an
        // OnApplyWindowInsetsListener, so needs to be set as so.
        ViewCompat.setWindowInsetsAnimationCallback(binding.root, deferringInsetsListener)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, deferringInsetsListener)

        /**
         * 2) The second step is reacting to any animations which run. This can be system driven,
         * such as the user focusing on an EditText and on-screen keyboard (IME) coming on screen,
         * or app driven (more on that in step 3).
         *
         * To react to animations, we set an [android.view.WindowInsetsAnimation.Callback] on any
         * views which we wish to react to inset animations. In this example, we want our
         * EditText holder view, and the conversation RecyclerView to react.
         *
         * We use our [TranslateDeferringInsetsAnimationCallback] class, bundled in this sample,
         * which will automatically move each view as the IME animates.
         *
         * Note about [TranslateDeferringInsetsAnimationCallback], it relies on the behavior of
         * [RootViewDeferringInsetsCallback] on the layout's root view.
         */
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.messageInputView,
            TranslateDeferringInsetsAnimationCallback(
                view = binding.messageInputView,
                persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
                deferredInsetTypes = WindowInsetsCompat.Type.ime(),
                // We explicitly allow dispatch to continue down to binding.messageHolder's
                // child views, so that step 2.5 below receives the call
                dispatchMode = WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE
            )
        )
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.messagesListView.getMessagesRecyclerView(),
            TranslateDeferringInsetsAnimationCallback(
                view = binding.messagesListView.getMessagesRecyclerView(),
                persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
                deferredInsetTypes = WindowInsetsCompat.Type.ime()
            )
        )

        /**
         * 2.5) We also want to make sure that our EditText is focused once the IME
         * is animated in, to enable it to accept input. Similarly, if the IME is animated
         * off screen and the EditText is focused, we should clear that focus.
         *
         * The bundled [ControlFocusInsetsAnimationCallback] callback will automatically request
         * and clear focus for us.
         *
         * Since `binding.messageEdittext` is a child of `binding.messageHolder`, this
         * [WindowInsetsAnimationCompat.Callback] will only work if the ancestor view's callback uses the
         * [WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE] dispatch mode, which
         * we have done above.
         */
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.messageInputView.inputEditText,
            ControlFocusInsetsAnimationCallback(binding.messageInputView.inputEditText)
        )
    }

    private fun ConversationHeaderView.initHeaderView() {
        //Example you cam implement your own custom UI elements
        setCustomUiElementsListener(object : HeaderUIElementsListenerImpl(this) {
            override fun onSubTitle(subjectTextView: TextView, channel: SceytChannel, replyMessage: SceytMessage?, replyInThread: Boolean) {
                super.onSubTitle(subjectTextView, channel, replyMessage, replyInThread)
                println("onSubject")
            }
        })

        setEventListener(HeaderEventsListener.TypingListener {
            println("Typing")
        })

        setCustomClickListener(object : HeaderClickListenersImpl(this) {
            override fun onAvatarClick(view: View) {
                CustomConversationInfoActivity.newInstance(this@ConversationActivity, channel)
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
            override fun onReplyMessageInThreadClick(message: SceytMessage) {
                super.onReplyMessageInThreadClick(message)
                newInstance(this@ConversationActivity, channel, message)
            }
        })

    }

    private fun MessageInputView.initMessageInputView() {
        setClickListener(MessageInputClickListeners.SendMsgClickListener {
            println("sending a message clicked...")
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